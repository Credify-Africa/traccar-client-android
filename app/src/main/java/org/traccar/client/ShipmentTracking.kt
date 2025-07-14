package org.traccar.client

import com.google.gson.annotations.SerializedName

data class ShipmentTracking (
    val id :Int,
    val deviceId: String,
    val containerNo: String,
    val status: String,

    @SerializedName("user.firstName")
    val firstName: String,

    @SerializedName("user.lastName")
    val lastName: String,
)