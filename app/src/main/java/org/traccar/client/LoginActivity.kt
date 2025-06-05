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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.traccar.client.UserData // Ensure this import is present

class LoginActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper // Declare without initialization
//    private val apiService = RetrofitClient.retrofit.create(SyncApiService::class.java)
    private val apiService = object : SyncApiService {
    override suspend fun sendPosition(position: Position): Unit = Unit
    override suspend fun sendFormData(submission: FormSubmission): Unit = Unit
    override suspend fun login(request: LoginRequest): LoginResponse {
        Log.d("LoginActivity", "Mock login called with phone: ${request.phone}, deviceId: ${request.deviceId}")
        return LoginResponse(
            data = UserData(
                id = 1L,
                phone = request.phone,
                firstName = "John",
                lastName = "Doe",
                password = "mockpassword"
            ),
            message = "Login successful",
            status = 200
        )
    }
    override suspend fun verifyCode(request: CodeVerificationRequest): CodeVerificationResponse = throw NotImplementedError()
    override suspend fun getShipmentHistory(userId: String): List<FormSubmission> = throw NotImplementedError()
}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize dbHelper here, after the activity context is available
        dbHelper = DatabaseHelper(this)

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

            val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
            Log.d("LoginActivity", "Attempting login with phone: $phoneNumber, deviceId: $deviceId")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = apiService.login(LoginRequest(phoneNumber, deviceId))
                    Log.d("LoginActivity","${response}")
                    Log.d("LoginActivity", "API Response: status=${response.status}, message=${response.message}, data=${response.data}")
                    if (response.status == 200 && response.data != null) {

                            runOnUiThread {
                                try {
                                    Log.d("LoginActivity", "Login successful, preparing to transition to CodeConfirmationActivity")
                                    val intent = Intent(this@LoginActivity, CodeConfirmationActivity::class.java)
                                    Log.d("LoginActivity", "Intent created for CodeConfirmationActivity")
                                    intent.putExtra("USER_DATA", response.data as Parcelable?)
                                    Log.d("LoginActivity", "Extra added to intent: USER_DATA=${response.data}")
                                    startActivity(intent)
                                    Log.d("LoginActivity", "startActivity called for CodeConfirmationActivity")
                                    finish()
                                    Log.d("LoginActivity", "finish called for LoginActivity")
                                } catch (e: Exception){
                                    Log.e("LoginActivity", "${e}")
                                }
                            }


                    } else {
                        Log.e("LOGIN", "${response.message}")
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, "Login failed: ${response.message}", Toast.LENGTH_SHORT).show()
                            loginButton.isEnabled = true
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LoginActivity", "${e.message}")
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