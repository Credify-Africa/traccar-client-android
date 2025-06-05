package org.traccar.client

data class FormSubmission(
    val id: String,
    val eventDesciption: String,
    val shipmentTrackingId: String,
    val eventTimeStamp: Long
)