package org.traccar.client

import PasswordSubmitActivity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CodeVerificationActiivity : AppCompatActivity() {

    private lateinit var codeInput: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var verifyButton: Button
    private lateinit var apiService: SyncApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.verify_code)

        codeInput = findViewById(R.id.verification_code)
        progressBar = findViewById(R.id.verify_progress)
        verifyButton = findViewById(R.id.verify_button)

        verifyButton.isEnabled = false

        codeInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateInput()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.credify.africa/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(SyncApiService::class.java)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val phone = preferences.getString("phone", null)

        verifyButton.setOnClickListener {
            val code = codeInput.text.toString().trim()
            progressBar.visibility = View.VISIBLE

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = apiService.vrifyResetCode(
                        verifyResetCodeRequest(code = code, phone = phone)
                    )
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        if (response.isSuccessful && response.body()?.success == true) {
                            startActivity(Intent(this@CodeVerificationActiivity, PasswordSubmitActivity::class.java))
                            finish()
                        } else {
                            codeInput.error = response.body()?.message ?: "Verification failed"
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CodeVerification", "Error verifying code", e)
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        codeInput.error = "Verification failed"
                    }
                }
            }
        }
    }

    private fun validateInput() {
        val code = codeInput.text.toString().trim()
        verifyButton.isEnabled = code.isNotEmpty()
        if (code.isEmpty()) {
            codeInput.error = "Enter verification code"
        } else {
            codeInput.error = null
        }
    }
}