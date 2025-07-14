package org.traccar.client

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.hbb20.CountryCodePicker

class PasswordSubmitActivity: AppCompatActivity() {

    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmInput: TextInputEditText
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.change_password)

        passwordInput = findViewById(R.id.password)
        confirmInput = findViewById(R.id.confirm_password)
        progressBar = findViewById(R.id.verify_progress)

        findViewById<Button>(R.id.submit_password).setOnClickListener {
            val password = passwordInput.text.toString().trim()
            val confirm = confirmInput.text.toString().trim()

            if (password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirm) {
                confirmInput.error = "Passwords do not match"
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            // TODO: Send new password to backend
            Toast.makeText(this, "Password reset successful", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }
}