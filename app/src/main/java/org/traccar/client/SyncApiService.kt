package org.traccar.client

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SyncApiService {
    @POST("api/positions")
    suspend fun sendPosition(@Body position: Position)

    @POST("shipment-tracking-event")
    suspend fun sendFormData(@Body submission: FormSubmission)

    @POST("driver-login")
    suspend fun login(@Body request: LoginRequest): retrofit2.Response<LoginResponse>

    @POST("verify-email")
    suspend fun verifyCode(@Body request: CodeVerificationRequest): CodeVerificationResponse

    @GET("shipment-tracking/{userId}")
    suspend fun getShipmentHistory(@Path("userId") userId: String): ShipmentResponse
}

data class ShipmentResponse(
    val data: List<ShipmentTracking>,
    val message: String
)

data class LoginRequest(val phone: String, val deviceId: String)

data class LoginResponse(
    val user: UserData?,
    val message: String,
    val requiresPasswordChange: Boolean? = null,
    val status: Int? = null,
    val error: Any? = null,
    val stack: String? = null
)

data class CodeVerificationRequest(val userId: String, val code: String) // Aligned userId type with getShipmentHistory

data class CodeVerificationResponse(
    val status: Int,
    val message: String,
    val success: Boolean,
    val userData: UserData?
)