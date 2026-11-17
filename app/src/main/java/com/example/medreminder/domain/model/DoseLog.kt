package com.example.medreminder.domain.model

/**
 * Domain model representing historical log entries of taken or missed doses.
 */
data class DoseLog(
    val id: Long = 0,
    val medicationId: Long,
    val medicationName: String,
    val dosage: String,
    val timestamp: String,
    val dayOfWeek: String,
    val isTaken: Boolean,
    val note: String? = null
)
