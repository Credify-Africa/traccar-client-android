package org.traccar.client

import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.hbb20.CountryCodePicker

// Change 'class Utils' to 'object Utils'
object Utils {

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

    fun validatePhoneNumber(rawPhone: String, rootView: View, ccp: CountryCodePicker): String? {
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
}