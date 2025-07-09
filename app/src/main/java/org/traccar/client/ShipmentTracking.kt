package org.traccar.client

data class ShipmentTracking (
    val id :Int,
    val deviceId: String,
    val containerNo: String,
    val status: String? = "PENDING",
    val firstName: String? = "Paul",
    val lastName: String? = "Naftali",
)