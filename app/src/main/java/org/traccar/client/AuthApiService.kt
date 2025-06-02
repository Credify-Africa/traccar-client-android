package org.traccar.client

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}