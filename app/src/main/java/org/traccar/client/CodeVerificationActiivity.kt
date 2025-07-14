package org.traccar.client

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

class CodeVerificationActiivity : AppCompatActivity() {

    private lateinit var codeInput: EditText
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.verify_code)

        codeInput = findViewById(R.id.verification_code)
        progressBar = findViewById(R.id.verify_progress)



        findViewById<Button>(R.id.verify_button).setOnClickListener {
            val code = codeInput.text.toString().trim()
            if (code.isEmpty()) {
                codeInput.error = "Enter verification code"
                return@setOnClickListener
            }
            progressBar.visibility = View.VISIBLE
            // TODO: Verify code with backend
            startActivity(Intent(this, PasswordSubmitActivity::class.java))
            finish()
        }
    }
}