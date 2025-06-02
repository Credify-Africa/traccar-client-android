package org.traccar.client

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SyncApiService {
    @POST("positions")
    suspend fun sendPosition(@Body position: Position)

    @POST("shipment-tracking")
    suspend fun sendFormData(@Body submission: FormSubmission)

    @GET("shipment-tracking/{userId}/")
    suspend fun getShipmentHistory(@Path("userId") userId: String): List<FormSubmission>
}