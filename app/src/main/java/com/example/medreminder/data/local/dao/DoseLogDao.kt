package com.example.medreminder.data.local.dao

import androidx.room.*
import com.example.medreminder.data.local.entity.DoseLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for Dose History Logs.
 */
@Dao
interface DoseLogDao {

    @Query("SELECT * FROM dose_logs ORDER BY id DESC")
    fun getAllDoseLogs(): Flow<List<DoseLogEntity>>

    @Query("SELECT * FROM dose_logs WHERE medicationId = :medicationId ORDER BY id DESC")
    fun getLogsForMedication(medicationId: Long): Flow<List<DoseLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoseLog(log: DoseLogEntity): Long

    @Query("DELETE FROM dose_logs WHERE medicationId = :medicationId AND dayOfWeek = :dayOfWeek")
    suspend fun deleteLogForMedicationAndDay(medicationId: Long, dayOfWeek: String)

    @Query("DELETE FROM dose_logs WHERE medicationId = :medicationId")
    suspend fun deleteLogsForMedication(medicationId: Long)

    @Query("DELETE FROM dose_logs")
    suspend fun deleteAllDoseLogs()
}
