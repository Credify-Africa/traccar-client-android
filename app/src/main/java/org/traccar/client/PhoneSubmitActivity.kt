package org.traccar.client

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.hbb20.CountryCodePicker


class PhoneSubmitActivity : AppCompatActivity() {
    private lateinit var phoneInput: TextInputEditText
    private lateinit var ccp: CountryCodePicker
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.phone_input_layout)

        phoneInput = findViewById(R.id.phone_number)
        progressBar = findViewById(R.id.verify_progress_submit)
        ccp = findViewById(R.id.country_code_picker)

        ccp.registerCarrierNumberEditText(phoneInput)

        findViewById<Button>(R.id.submit).setOnClickListener {
            val phoneNumber = phoneInput.text.toString().trim()
            val fullNumber = ccp.fullNumberWithPlus + phoneNumber
//            if (phoneNumber.isEmpty()) {
//                phoneInput.error = "Phone number required"
//                return@setOnClickListener
//            }

            progressBar.visibility = View.VISIBLE
            // TODO: Send verification code to the number
            val intent = Intent(this@PhoneSubmitActivity, CodeVerificationActiivity::class.java)
//                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
//            startActivity(Intent(this, CodeVerificationActiivity::class.java))
//            finish()
        }
    }
}