package org.traccar.client

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.textfield.TextInputEditText
import com.hbb20.CountryCodePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PhoneSubmitActivity : AppCompatActivity() {
    private lateinit var phoneInput: TextInputEditText
    private lateinit var ccp: CountryCodePicker
    private lateinit var progressBar: ProgressBar
    private lateinit var submitButton: Button
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var apiService: SyncApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = DatabaseHelper(this)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                response
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.credify.africa/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(SyncApiService::class.java)
        setContentView(R.layout.phone_input_layout)

        phoneInput = findViewById(R.id.phone_number)
        progressBar = findViewById(R.id.verify_progress_submit)
        ccp = findViewById(R.id.country_code_picker)
        submitButton = findViewById(R.id.submit)
        val rootView = findViewById<View>(R.id.phone_layout)

        ccp.registerCarrierNumberEditText(phoneInput)
        submitButton.isEnabled = false

        phoneInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                Utils.validatePhoneNumber(phoneInput.text.toString().trim(),rootView,ccp)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        submitButton.setOnClickListener {
            val phoneNumber = phoneInput.text.toString().trim()
            val fullPhone = Utils.validatePhoneNumber(phoneNumber, rootView, ccp)

            progressBar.visibility = View.VISIBLE

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val retrofitResponse = apiService.requestResetPassword(RestPasswordRequest(fullPhone))
                    val httpStatusCode = retrofitResponse.code()
                    Log.d("LoginActivity", "HTTP Status Code: $httpStatusCode")

                    if (retrofitResponse.isSuccessful) {
                        preferences.edit().putString("phone", fullPhone).apply()
                        val intent = Intent(this@PhoneSubmitActivity, CodeVerificationActiivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } catch (e: Exception) {
                    Log.e("LoginActivity", "Error during API call", e)
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        phoneInput.error = "Failed to send verification code"
                    }
                }
            }
        }
    }

    private fun validateInput(rootView: View) {
        val phoneNumber = phoneInput.text.toString().trim()
        val isValid = phoneNumber.isNotEmpty() && Utils.validatePhoneNumber(phoneNumber, rootView, ccp) != null
        submitButton.isEnabled = isValid
        if (!isValid) {
            phoneInput.error = "Enter a valid phone number"
        } else {
            phoneInput.error = null
        }
    }
}