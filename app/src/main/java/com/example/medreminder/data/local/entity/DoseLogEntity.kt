package com.example.medreminder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.medreminder.domain.model.DoseLog

/**
 * Room Entity representing historical dose logs table.
 */
@Entity(tableName = "dose_logs")
data class DoseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val medicationName: String,
    val dosage: String,
    val timestamp: String,
    val dayOfWeek: String,
    val isTaken: Boolean,
    val note: String? = null
)

fun DoseLogEntity.toDomainModel() = DoseLog(
    id = id,
    medicationId = medicationId,
    medicationName = medicationName,
    dosage = dosage,
    timestamp = timestamp,
    dayOfWeek = dayOfWeek,
    isTaken = isTaken,
    note = note
)

fun DoseLog.toEntity() = DoseLogEntity(
    id = id,
    medicationId = medicationId,
    medicationName = medicationName,
    dosage = dosage,
    timestamp = timestamp,
    dayOfWeek = dayOfWeek,
    isTaken = isTaken,
    note = note
)