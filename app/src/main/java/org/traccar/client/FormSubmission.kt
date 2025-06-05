package org.traccar.client

data class FormSubmission(
    val id: String,
    val containerId: String,
    val comment: String,
    val deviceId:String,
    val shipmentTrackingId: String,
    val timestamp: Long
)