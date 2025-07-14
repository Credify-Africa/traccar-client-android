package org.traccar.client

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
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
import com.google.android.material.textfield.TextInputEditText
import com.hbb20.CountryCodePicker
import com.google.android.material.snackbar.Snackbar


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
        dbHelper = DatabaseHelper(this)
        initializePreferences()
        // Initialize dbHelper here, after the activity context is available

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
                Log.e("LoginActivity", "Auth Header: $authHeader")
                if (authHeader != null && authHeader.startsWith("Authorization=")) {
                    authToken = authHeader
                }
                Log.e("LoginActivity", "Auth Header: $authToken")
                response
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.credify.africa/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(SyncApiService::class.java)

        val usernameInput = findViewById<TextInputEditText>(R.id.phone_number)
        val ccp = findViewById<CountryCodePicker>(R.id.country_code_picker)
        val loginButton = findViewById<Button>(R.id.login_button)
        val loginProgress = findViewById<ProgressBar>(R.id.login_progress)
        val passwordInput = findViewById<TextInputEditText>(R.id.password)
        val passwordHint = findViewById<TextView>(R.id.password_hint)
        val forgotPassword = findViewById<TextView>(R.id.forgot_password)
        val rootView = findViewById<View>(R.id.root_layout)

        ccp.setDefaultCountryUsingNameCode("KE")
        ccp.resetToDefaultCountry()

        ccp.registerCarrierNumberEditText(usernameInput)


        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION), 1)
        }

        usernameInput.setOnFocusChangeListener { _, hasFocus ->
            Log.e("LoginActivity", "usernameInput focus changed. hasFocus: $hasFocus")
            if (!hasFocus) {
                val phone = usernameInput.text.toString().trim()
                validatePhoneNumber(phone, rootView, ccp)
                if (phone.isNotEmpty()) {
                    Log.e("LoginActivity", "Checking user by phone: $phone")

                    dbHelper.getUserByPhoneAsync(phone, object : DatabaseHandler<User?> {
                        override fun onComplete(success: Boolean, result: User?) {
                            runOnUiThread {
//                                Log.e("LoginActivity", "getUserByPhoneAsync completed. Success: $success, Result: $result")
                                if (result == null) {
                                    // First time user, generate and show password

                                    passwordHint.text = "First time user? here is a reccomended pin"
                                    passwordHint.visibility = android.view.View.VISIBLE
                                    passwordInput.setText(generateRecommendedPassword())
                                } else {
                                    passwordHint.visibility = android.view.View.GONE
                                    passwordInput.setText("")
                                }
                            }
                        }
                    })
                }
            }
        }

        passwordInput.setOnFocusChangeListener { _, hasFocus ->
            Log.e("LoginActivity", "passwordInput focus changed. hasFocus: $hasFocus")
            if (!hasFocus) {
                val password = passwordInput.text.toString().trim()
                if (password.isEmpty()) {
//                    passwordHint.visibility = android.view.View.VISIBLE
//                    passwordHint.text = "Please enter your pin"
                    Snackbar.make(rootView, "Please enter your pin", Snackbar.LENGTH_LONG).show()
//                    null
                }

                if (password.length > 4 || password.length < 4) {
                    Snackbar.make(rootView, "Pin is too long or too short", Snackbar.LENGTH_LONG).show()
                }
//                } else {
//                    passwordHint.visibility = android.view.View.GONE
//                }
            }
        }

        passwordInput.text

        loginButton.setOnClickListener {
            usernameInput.clearFocus() // Add this line
            passwordInput.clearFocus()

            loginButton.isEnabled = false
            loginProgress.visibility = android.view.View.VISIBLE

            val rawPhone = usernameInput.text.toString().trim()
            val fullPhone = validatePhoneNumber(rawPhone, rootView, ccp)

            if (fullPhone == null) {
                loginButton.isEnabled = true
                loginProgress.visibility = View.GONE
                return@setOnClickListener
            }
//            val phoneNumber = usernameInput.text.toString().trim()

//            val fullPhoneNumber = ccp.fullNumberWithPlus
//            Log.d("LoginActivity", "Full phone number: $fullPhoneNumber")


//            if (phoneNumber.isEmpty()) {
//                Snackbar.make(rootView, "Please enter your phone number", Snackbar.LENGTH_SHORT).show()
////                Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show()
//                loginButton.isEnabled = true
//                return@setOnClickListener
//            }
//
//            var sanitized = phoneNumber
//            if (phoneNumber.startsWith("0")) {
//                sanitized = phoneNumber.substring(1)
//            }
//
//            if (sanitized.length < 9) {
//                Snackbar.make(rootView, "Phone number seems too short", Snackbar.LENGTH_SHORT).show()
////                Toast.makeText(this, "Phone number seems too short", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            var fullPhone = "+" + ccp.selectedCountryCode + sanitized
//            fullPhone = fullPhone.replace(" ", "")
//            Log.d("LoginActivity", "Full phone number: $fullPhone")
//
            val password = passwordInput.text.toString().trim()
            if (password.isEmpty()) {
                runOnUiThread {
                    Toast.makeText(this, "Please enter your pin", Toast.LENGTH_SHORT).show()
                    loginButton.isEnabled = true
                }
                return@setOnClickListener
            }

            val deviceId = preferences.getString(MainFragment.KEY_DEVICE, "undefined")!!
            Log.d("LoginActivity", "Attempting login with phone: $fullPhone, deviceId: $deviceId")
//            Toast.makeText(this@LoginActivity, "ID: ${deviceId}", Toast.LENGTH_LONG).show()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val retrofitResponse: retrofit2.Response<LoginResponse> = apiService.login(LoginRequest(fullPhone, deviceId, password))

                    // Get the HTTP status code
                    val httpStatusCode = retrofitResponse.code()
                    Log.d("LoginActivity", "HTTP Status Code: $httpStatusCode")

                    if (retrofitResponse.isSuccessful) {
                        // Request was successful (HTTP status 2xx)
                        val apiResponse = retrofitResponse.body() // This is your LoginResponse object
                        if (apiResponse != null) {
                            Log.d("LoginActivity", "API Response: status=${apiResponse.status}, message=${apiResponse.message}, data=${apiResponse.user}")

                            if (apiResponse.user != null && httpStatusCode == 200) {

                                if (authToken != null) {
                                    Log.d("LoginActivity", "Auth Token: $authToken")
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
                                                Log.e("LoginActivity", "User data saved successfully")
                                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
//                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                startActivity(intent)
                                                finish()

//                                                if (phoneNumber.contains("2568000000000")){
//                                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
////                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                                                startActivity(intent)
//                                                finish()
//                                                } else {
//                                                    val intent = Intent(this@LoginActivity, CodeConfirmationActivity::class.java)
////                                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                                                    startActivity(intent)
//                                                    finish()
//                                                }
//                                                Log.e("LoginActivity", "Navigating to CodeConfirmationActivity")
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
//                                runOnUiThread {
                                    Snackbar.make(rootView, "Login failed: Incorrect password or phone number", Snackbar.LENGTH_SHORT).show()
//                                    Toast.makeText(this@LoginActivity, "Login failed: Incorrect password or phone number", Toast.LENGTH_SHORT).show()
                                    loginButton.isEnabled = true
                                    loginProgress.visibility = android.view.View.GONE
//                                }
                            }
                        } else {
                            // Response body was null (e.g., 204 No Content, but typically not for login)
                            Log.e("LoginActivity", "Successful response with no body")
                            runOnUiThread {
                                Toast.makeText(this@LoginActivity, "Login failed: Empty response", Toast.LENGTH_SHORT).show()
                                loginButton.isEnabled = true
                                loginProgress.visibility = android.view.View.GONE
                            }
                        }
                    } else {
                        // Request was not successful (HTTP status 4xx or 5xx)
                        val errorBody = retrofitResponse.errorBody()?.string()
                        Log.e("LoginActivity", "Login failed: HTTP Status Code $httpStatusCode, Error Body: $errorBody")
                        runOnUiThread {
//                        Snackbar.make(rootView, "Login failed: Incorrect password or phone number", Snackbar.LENGTH_SHORT).show()
                            Toast.makeText(this@LoginActivity, "Login failed: Incorrect password or phone number", Toast.LENGTH_SHORT).show()
                            loginButton.isEnabled = true
                            loginProgress.visibility = android.view.View.GONE
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LoginActivity", "Exception during login: ${e.message} ", e)
                    runOnUiThread {
//                    Snackbar.make(rootView, "Login failed: Incorrect password or phone number", Snackbar.LENGTH_SHORT).show()
                        Toast.makeText(this@LoginActivity, "Incorrect password or phone number", Toast.LENGTH_SHORT).show()
                        loginButton.isEnabled = true
                        loginProgress.visibility = android.view.View.GONE
                    }
                }
            }
        }


        forgotPassword.setOnClickListener {
            val intent = Intent(this, PhoneSubmitActivity::class.java)
            startActivity(intent)
        }
    }

    private fun initializePreferences() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sharedPreferences.edit()

        // Set status to true to enable TrackingService
        editor.putBoolean("status", true)

        // Check if device ID exists in DB, otherwise generate and store
        dbHelper.selectDeviceIdAsync(object : DatabaseHandler<String?> {
            override fun onComplete(success: Boolean, result: String?) {
                if (success && result != null) {
                    // Device ID exists in DB, ensure it's in preferences
                    if (!sharedPreferences.contains(MainFragment.KEY_DEVICE)) {
                        editor.putString(MainFragment.KEY_DEVICE, result)
                    }
                } else {
                    // No device ID in DB, check preferences or generate new
                    val deviceId = sharedPreferences.getString(MainFragment.KEY_DEVICE, null) ?: run {
                        val newDeviceId = (Random().nextInt(900000) + 100000).toString()
                        dbHelper.insertDeviceIdAsync(newDeviceId, object : DatabaseHandler<Unit?> {
                            override fun onComplete(success: Boolean, result: Unit?) {
                                if (!success) {
                                    Log.e("LoginActivity", "Failed to save device ID to database")
                                }
                            }
                        })
                        newDeviceId
                    }
                    editor.putString(MainFragment.KEY_DEVICE, deviceId)
                }
                // Set other preferences
                editor.putString("url", "https://tracking.credify.africa")
                editor.putString("accuracy", "medium")
                editor.putString("interval", "300")
                editor.putString("distance", "0")
                editor.putString("angle", "0")
                editor.putBoolean("buffer", true)
                editor.putBoolean("wakelock", true)
                editor.apply()
            }
        })
    }

    private fun validatePhoneNumber(rawPhone: String, rootView: View, ccp: CountryCodePicker): String? {
        val sanitized = if (rawPhone.startsWith("0")) rawPhone.substring(1) else rawPhone

        return when {
            rawPhone.isEmpty() -> {
                Snackbar.make(rootView, "Please enter your phone number", Snackbar.LENGTH_LONG).show()
                null
            }
            sanitized.length < 9 -> {
                Snackbar.make(rootView, "Phone number seems too short", Snackbar.LENGTH_LONG).show()
                null
            }
            else -> {
                val fullPhone = ccp.selectedCountryCode + sanitized
                fullPhone.replace(" ", "")
            }
        }
    }


    fun generateRecommendedPassword(): String {
        val random = java.util.Random()
        var password: String
        do {
            val digits = mutableListOf<Char>()
            while (digits.size < 4) {
                val nextDigit = ('0' + random.nextInt(10))
                if (digits.count { it == nextDigit } < 2) {
                    digits.add(nextDigit)
                }
            }
            // Ensure the first digit is not '0'
            if (digits[0] == '0') {
                val nonZeroIndex = digits.indexOfFirst { it != '0' }
                if (nonZeroIndex > 0) {
                    val temp = digits[0]
                    digits[0] = digits[nonZeroIndex]
                    digits[nonZeroIndex] = temp
                }
            }
            password = digits.joinToString("")
        } while (password == "1234")
        return password
    }
}