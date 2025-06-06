package org.traccar.client

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.traccar.client.CodeConfirmationActivity
import org.traccar.client.DatabaseHelper.DatabaseHandler
import org.traccar.client.UserData // Ensure this import is present
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.text.startsWith
import okhttp3.logging.HttpLoggingInterceptor

class LoginActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper // Declare without initialization
//    private val apiService = RetrofitClient.retrofit.create(SyncApiService::class.java)
private lateinit var apiService: SyncApiService
    private var authToken: String? = null
//    private val preferences = PreferenceManager.getDefaultSharedPreferences(this)
//    private val apiService = object : SyncApiService {
//    override suspend fun sendPosition(position: Position): Unit = Unit
//    override suspend fun sendFormData(submission: FormSubmission): Unit = Unit
//    override suspend fun login(request: LoginRequest): LoginResponse {
//        Log.d("LoginActivity", "Mock login called with phone: ${request.phone}, deviceId: ${request.deviceId}")
//        return LoginResponse(
//            data = UserData(
//                id = 1L,
//                phone = request.phone,
//                firstName = "John",
//                lastName = "Doe",
//                password = "mockpassword"
//            ).apply { this.token = "mock-token-123" },
//            message = "Login successful",
//            status = 200
//        )
//    }
//    override suspend fun verifyCode(request: CodeVerificationRequest): CodeVerificationResponse = throw NotImplementedError()
//    override suspend fun getShipmentHistory(userId: String): List<ShipmentTracking> = throw NotImplementedError()
//}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize dbHelper here, after the activity context is available
        dbHelper = DatabaseHelper(this)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // LOGS FULL REQUEST + RESPONSE
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                val authHeader = response.header("Authorization")
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authToken = authHeader
                }
                response
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.credify.africa/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(SyncApiService::class.java)

        val usernameInput = findViewById<EditText>(R.id.phone_number)
        val loginButton = findViewById<Button>(R.id.login_button)

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION), 1)
        }

        loginButton.setOnClickListener {
            loginButton.isEnabled = false
            val phoneNumber = usernameInput.text.toString().trim()

            if (phoneNumber.isEmpty()) {
                Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show()
                loginButton.isEnabled = true
                return@setOnClickListener
            }

            val deviceId = preferences.getString(MainFragment.KEY_DEVICE, "undefined")!!
            Log.d("LoginActivity", "Attempting login with phone: $phoneNumber, deviceId: $deviceId")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = apiService.login(LoginRequest(phoneNumber, deviceId))
                    Log.d("LoginActivity","Response ${response}")
                    Log.d("LoginActivity", "API Response: status=${response.status}, message=${response.message}, data=${response.data}")
                    if (response.status == 200 && response.data != null) {

                        if (authToken != null) {
                            dbHelper.insertUserAsync(User(
                                id = response.data.id,
                                phone = response.data.phone,
                                firstName = response.data.firstName,
                                lastName = response.data.lastName,
                                password = response.data.password,
                                token = authToken
                            ), object : DatabaseHandler<Unit?> {
                                override fun onComplete(success: Boolean, result: Unit?) {
                                    if (success) {
                                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        runOnUiThread {
                                            Toast.makeText(this@LoginActivity, "Failed to save user data", Toast.LENGTH_SHORT).show()
                                            loginButton.isEnabled = true
                                        }
                                    }
                                }
                            })
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@LoginActivity, "Token not received from server", Toast.LENGTH_SHORT).show()
                                loginButton.isEnabled = true
                            }
                        }
//                            runOnUiThread {
//                                try {
//                                    Log.d("LoginActivity", "Login successful, preparing to transition to CodeConfirmationActivity")
//                                    val intent = Intent(this@LoginActivity, CodeConfirmationActivity::class.java)
//                                    Log.d("LoginActivity", "Intent created for CodeConfirmationActivity")
//                                    intent.putExtra("USER_DATA", response.data as Parcelable?)
//                                    Log.d("LoginActivity", "Extra added to intent: USER_DATA=${response.data}")
//                                    startActivity(intent)
//                                    Log.d("LoginActivity", "startActivity called for CodeConfirmationActivity")
//                                    finish()
//                                    Log.d("LoginActivity", "finish called for LoginActivity")
//                                } catch (e: Exception){
//                                    Log.e("LoginActivity", "${e}")
//                                }
//                            }


                    } else {
                        Log.e("LOGIN", "${response.message}")
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, "Login failed: ${response.message}", Toast.LENGTH_SHORT).show()
                            loginButton.isEnabled = true
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LoginActivity", "${e.message} ")
                    Log.e("LoginActivity", "Stack trace: ", e)
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        loginButton.isEnabled = true
                    }
                }
            }
        }
    }
}