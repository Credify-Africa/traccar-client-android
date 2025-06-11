package org.traccar.client

import android.content.Intent
import android.os.Bundle
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
import org.traccar.client.DatabaseHelper.DatabaseHandler
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.text.startsWith
import okhttp3.logging.HttpLoggingInterceptor
import java.util.Random

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
        initializePreferences()
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
                val authHeader = response.header("Set-Cookie")
                if (authHeader != null && authHeader.startsWith("Authorization=")) {
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
            Toast.makeText(this@LoginActivity, "ID: ${deviceId}", Toast.LENGTH_LONG).show()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val retrofitResponse: retrofit2.Response<LoginResponse> = apiService.login(LoginRequest(phoneNumber, deviceId))

                    // Get the HTTP status code
                    val httpStatusCode = retrofitResponse.code()
                    Log.d("LoginActivity", "HTTP Status Code: $httpStatusCode")

                    if (retrofitResponse.isSuccessful) {
                        // Request was successful (HTTP status 2xx)
                        val apiResponse = retrofitResponse.body() // This is your LoginResponse object
                        if (apiResponse != null) {
                            Log.d("LoginActivity", "API Response: status=${apiResponse.status}, message=${apiResponse.message}, data=${apiResponse.user}")

                            if (apiResponse.user != null) {

                                if (authToken != null) {
                                    PreferenceManager.getDefaultSharedPreferences(this@LoginActivity)
                                        .edit()
                                        .putString("auth_token", authToken)
                                        .apply()

                                    dbHelper.insertUserAsync(User(
                                        id = apiResponse.user.id,
                                        phone = apiResponse.user.phone,
                                        firstName = apiResponse.user.firstName,
                                        lastName = apiResponse.user.lastName,
                                        password = apiResponse.user.password,
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

                            } else {
                                // Your custom API response indicates failure, even if HTTP status is 2xx
                                Log.e("LOGIN", "${apiResponse.message}")
                                runOnUiThread {
                                    Toast.makeText(this@LoginActivity, "Login failed: ${apiResponse.message}", Toast.LENGTH_SHORT).show()
                                    loginButton.isEnabled = true
                                }
                            }
                        } else {
                            // Response body was null (e.g., 204 No Content, but typically not for login)
                            Log.e("LoginActivity", "Successful response with no body")
                            runOnUiThread {
                                Toast.makeText(this@LoginActivity, "Login failed: Empty response", Toast.LENGTH_SHORT).show()
                                loginButton.isEnabled = true
                            }
                        }
                    } else {
                        // Request was not successful (HTTP status 4xx or 5xx)
                        val errorBody = retrofitResponse.errorBody()?.string()
                        Log.e("LoginActivity", "Login failed: HTTP Status Code $httpStatusCode, Error Body: $errorBody")
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, "Login failed: Server error ($httpStatusCode)", Toast.LENGTH_SHORT).show()
                            loginButton.isEnabled = true
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LoginActivity", "Exception during login: ${e.message} ", e)
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        loginButton.isEnabled = true
                    }
                }
            }
        }
    }
    private fun initializePreferences() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sharedPreferences.edit()

        // Set status to true to enable TrackingService
        editor.putBoolean("status", true)

        // Set device ID (random 6-digit number)
        if (!sharedPreferences.contains("id")) {
            val id = (Random().nextInt(900000) + 100000).toString()
            editor.putString("id", id)



        }

        // Set server URL
        editor.putString("url", "https://tracking.credify.africa") // Replace with your server URL

        // Set accuracy
        editor.putString("accuracy", "medium")

        // Set interval
        editor.putString("interval", "300")

        // Set distance
        editor.putString("distance", "0")

        // Set angle
        editor.putString("angle", "0")

        // Set buffer
        editor.putBoolean("buffer", true)

        // Set wakelock
        editor.putBoolean("wakelock", true)

        editor.apply()
    }
}