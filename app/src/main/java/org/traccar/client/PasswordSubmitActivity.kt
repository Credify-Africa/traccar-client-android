import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.traccar.client.LoginActivity
import org.traccar.client.R
import org.traccar.client.ResetPasswordRequest
import org.traccar.client.SyncApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PasswordSubmitActivity : AppCompatActivity() {

    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmInput: TextInputEditText
    private lateinit var progressBar: ProgressBar
    private lateinit var submitButton: Button
    private lateinit var apiService: SyncApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.change_password)

        passwordInput = findViewById(R.id.password)
        confirmInput = findViewById(R.id.confirm_password)
        progressBar = findViewById(R.id.verify_progress)
        submitButton = findViewById(R.id.submit_password)

        // Retrofit setup
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

        // Validation logic
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateInputs(phone)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        passwordInput.addTextChangedListener(watcher)
        confirmInput.addTextChangedListener(watcher)

        submitButton.setOnClickListener {
            val password = passwordInput.text.toString().trim()
            val confirm = confirmInput.text.toString().trim()

            progressBar.visibility = View.VISIBLE

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = apiService.resetPassword(
                        ResetPasswordRequest(
                            phone = phone!!,
                            password = password,
                            confirmPassword = confirm
                        )
                    )
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        if (response.isSuccessful && response.body()?.success == true) {
                            Toast.makeText(this@PasswordSubmitActivity, "Password reset successful", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@PasswordSubmitActivity, LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                            finish()
                        } else {
                            Toast.makeText(this@PasswordSubmitActivity, response.body()?.message ?: "Reset failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PasswordSubmit", "Error resetting password", e)
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@PasswordSubmitActivity, "Reset failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun validateInputs(phone: String?) {
        val password = passwordInput.text.toString().trim()
        val confirm = confirmInput.text.toString().trim()

        when {
            password.isEmpty() || confirm.isEmpty() -> {
                submitButton.isEnabled = false
            }
            password != confirm -> {
                confirmInput.error = "Passwords do not match"
                submitButton.isEnabled = false
            }
            phone.isNullOrEmpty() -> {
                submitButton.isEnabled = false
            }
            else -> {
                confirmInput.error = null
                submitButton.isEnabled = true
            }
        }
    }
}