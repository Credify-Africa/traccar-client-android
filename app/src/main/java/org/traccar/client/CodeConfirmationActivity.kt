package org.traccar.client

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.traccar.client.DatabaseHelper.DatabaseHandler
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.util.Log

class CodeConfirmationActivity : AppCompatActivity() {
    private lateinit var apiService: SyncApiService
    private lateinit var dbHelper: DatabaseHelper

    private var authToken: String? = null

//    data class UserData(
//        @SerializedName("id") val id: Long,
//        @SerializedName("phone") val phone: String?,
//        @SerializedName("firstName") val firstName: String?,
//        @SerializedName("lastName") val lastName: String?,
//        @SerializedName("password") val password: String?
//    ) : Parcelable {
//        constructor(parcel: Parcel) : this(
//            parcel.readLong(),
//            parcel.readString(),
//            parcel.readString(),
//            parcel.readString(),
//            parcel.readString()
//        )
//
//        override fun writeToParcel(parcel: Parcel, flags: Int) {
//            parcel.writeLong(id)
//            parcel.writeString(phone)
//            parcel.writeString(firstName)
//            parcel.writeString(lastName)
//            parcel.writeString(password)
//        }
//
//        override fun describeContents(): Int = 0
//
//        companion object CREATOR : Parcelable.Creator<UserData> {
//            override fun createFromParcel(parcel: Parcel): UserData = UserData(parcel)
//            override fun newArray(size: Int): Array<UserData?> = arrayOfNulls(size)
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_code_confirmation)

        dbHelper = DatabaseHelper(this)

        val userData = intent.getParcelableExtra("USER_DATA") as UserData? ?: run {
            Toast.makeText(this, "User data not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Set up OkHttpClient with interceptor to capture Authorization header
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                val authHeader = response.header("Authorization")
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
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

//        apiService = object : SyncApiService {
//            override suspend fun sendPosition(position: Position): Unit = Unit
//            override suspend fun sendFormData(submission: FormSubmission): Unit = Unit
//            override suspend fun login(request: LoginRequest): LoginResponse = throw NotImplementedError()
//            override suspend fun verifyCode(request: CodeVerificationRequest): CodeVerificationResponse {
//                return CodeVerificationResponse(
//                    status = 200,
//                    message = "Verification successful",
//                    success = true,
//                    userData = UserData(
//                        id = userData.id,
//                        phone = userData.phone,
//                        firstName = userData.firstName,
//                        lastName = userData.lastName,
//                        password = userData.password
//                    ).apply { this.token = "mock-token-123" }
//                ).also { response ->
//                    // Simulate header in mock (though interceptor needs a real response)
//                    authToken = "Bearer mock-token-123"
//                }
//            }
//            override suspend fun getShipmentHistory(userId: String): List<ShipmentTracking> = throw NotImplementedError()
//        }

        val codeInput = findViewById<EditText>(R.id.verification_code)
        val verifyButton = findViewById<Button>(R.id.verify_button)

        verifyButton.setOnClickListener {
            verifyButton.isEnabled = false
            val code = codeInput.text.toString().trim()

            if (code.isEmpty()) {
                Toast.makeText(this, "Please enter the verification code", Toast.LENGTH_SHORT).show()
                verifyButton.isEnabled = true
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = apiService.verifyCode(CodeVerificationRequest(userData.id.toString(), code))
                    if (response.status == 200) {
                        if (authToken != null) {
                            dbHelper.insertUserAsync(User(
                                id = userData.id,
                                phone = userData.phone,
                                firstName = userData.firstName,
                                lastName = userData.lastName,
                                password = userData.password,
                                token = authToken
                            ), object : DatabaseHandler<Unit?> {
                                override fun onComplete(success: Boolean, result: Unit?) {
                                    if (success) {
                                        val intent = Intent(this@CodeConfirmationActivity, MainActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        runOnUiThread {
                                            Toast.makeText(this@CodeConfirmationActivity, "Failed to save user data", Toast.LENGTH_SHORT).show()
                                            verifyButton.isEnabled = true
                                        }
                                    }
                                }
                            })
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@CodeConfirmationActivity, "Token not received from server", Toast.LENGTH_SHORT).show()
                                verifyButton.isEnabled = true
                            }
                        }
                    } else {
                        Log.e("CodeConfirmation","${response.message}")
                        runOnUiThread {
                            Toast.makeText(this@CodeConfirmationActivity, "Verification failed: ${response.message}", Toast.LENGTH_SHORT).show()
                            verifyButton.isEnabled = true
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CodeConfirmation", "${e}")
                    runOnUiThread {
                        Toast.makeText(this@CodeConfirmationActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        verifyButton.isEnabled = true
                    }
                }
            }
        }
    }
}