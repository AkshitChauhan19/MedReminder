package com.example.medreminder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.medreminder.domain.model.Medication
import com.example.medreminder.util.TimeUtils

/**
 * Room Entity representing the "medications" table.
 */
@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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

/**
 * Mapper: DB Entity -> Domain Model
 */
fun MedicationEntity.toDomainModel() = Medication(
    id = id,
    name = name,
    dosage = dosage,
    form = form,
    frequency = frequency,
    timeOfDay = timeOfDay,
    scheduledTime = TimeUtils.formatTo12Hour(scheduledTime),
    remainingPills = remainingPills,
    totalPills = totalPills,
    accentColorHex = accentColorHex,
    specialInstructions = specialInstructions,
    prescriberName = prescriberName,
    prescriberSpecialty = prescriberSpecialty,
    prescriberPhone = prescriberPhone,
    pharmacyName = pharmacyName,
    pharmacyAddress = pharmacyAddress,
    rxNumber = rxNumber,
    refillsLeft = refillsLeft,
    isTakenToday = isTakenToday,
    lastTakenTime = lastTakenTime
)

/**
 * Mapper: Domain Model -> DB Entity
 */
fun Medication.toEntity() = MedicationEntity(
    id = id,
    name = name,
    dosage = dosage,
    form = form,
    frequency = frequency,
    timeOfDay = timeOfDay,
    scheduledTime = scheduledTime,
    remainingPills = remainingPills,
    totalPills = totalPills,
    accentColorHex = accentColorHex,
    specialInstructions = specialInstructions,
    prescriberName = prescriberName,
    prescriberSpecialty = prescriberSpecialty,
    prescriberPhone = prescriberPhone,
    pharmacyName = pharmacyName,
    pharmacyAddress = pharmacyAddress,
    rxNumber = rxNumber,
    refillsLeft = refillsLeft,
    isTakenToday = isTakenToday,
    lastTakenTime = lastTakenTime
)
