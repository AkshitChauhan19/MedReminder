package com.example.medreminder.domain.model

/**
 * Core Domain model for a medication prescription.
 */
data class Medication(
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val form: String,
    val frequency: String,
    val timeOfDay: String,
    val scheduledTime: String,
    val remainingPills: Int,
    val totalPills: Int = 30,
    val accentColorHex: String = "#009688",
    val specialInstructions: String? = null,
    val prescriberName: String? = null,
    val prescriberSpecialty: String? = null,
    val prescriberPhone: String? = null,
    val pharmacyName: String? = null,
    val pharmacyAddress: String? = null,
    val rxNumber: String? = null,
    val refillsLeft: Int = 0,
    val isTakenToday: Boolean = false,
    val lastTakenTime: String? = null
)
