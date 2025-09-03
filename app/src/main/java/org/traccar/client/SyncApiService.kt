package org.traccar.client

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SyncApiService {
    @POST("api/positions")
    suspend fun sendPosition(@Body position: Position)

    @POST("shipment-tracking-event")
    suspend fun sendFormData(@Body submission: FormSubmission): retrofit2.Response<Unit>

    @POST("driver-login")
    suspend fun login(@Body request: LoginRequest): retrofit2.Response<LoginResponse>

    @POST("mobile/request-reset")
    suspend fun requestResetPassword(@Body request: RestPasswordRequest): retrofit2.Response<RestPasswordResponse>

    @POST("mobile/verify-code")
    suspend fun vrifyResetCode(@Body request: verifyResetCodeRequest): retrofit2.Response<verifyResetCodeResponse>

    @POST("mobile/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): retrofit2.Response<ResetPasswordResponse>

    @POST("verify-email")
    suspend fun verifyCode(@Body request: CodeVerificationRequest): retrofit2.Response<CodeVerificationResponse>

    @GET("shipment-tracking/{userId}")
    suspend fun getShipmentHistory(@Path("userId") userId: String): ShipmentResponse
}

data class ShipmentResponse(
    val data: List<ShipmentTracking>,
    val message: String
)

data class LoginRequest(val phone: String, val deviceId: String, val password: String)

data class LoginResponse(
    val user: UserData?,
    val message: String,
    val requiresPasswordChange: Boolean? = null,
    val status: Int? = null,
    val error: Any? = null,
    val stack: String? = null
)

data class CodeVerificationRequest(val userId: Int, val code: String) // Aligned userId type with getShipmentHistory

data class CodeVerificationResponse(
    val status: Int,
    val message: String,
    val success: Boolean,
    val userData: UserData?
)

data class RestPasswordRequest(
    val phone: String?,
)

data class RestPasswordResponse(
    val message: String,
)

data class verifyResetCodeRequest(
    val code: String,
    val phone: String?,
)

data class verifyResetCodeResponse(
    val message: String,
    val success: Boolean,
)

data class ResetPasswordRequest(
    val phone: String,
    val password: String,
    val confirmPassword: String
)

data class ResetPasswordResponse(
    val message: String,
    val success: Boolean
)
