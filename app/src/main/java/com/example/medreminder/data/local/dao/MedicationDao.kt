package com.example.medreminder.data.local.dao

import androidx.room.*
import com.example.medreminder.data.local.entity.MedicationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for Medication queries. Returns reactive Flow streams.
 */
@Dao
interface MedicationDao {

    @Query("SELECT * FROM medications ORDER BY id ASC")
    fun getAllMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    fun getMedicationById(id: Long): Flow<MedicationEntity?>

    @Query("SELECT * FROM medications ORDER BY scheduledTime ASC")
    fun getTodaySchedule(): Flow<List<MedicationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity): Long

    @Update
    suspend fun updateMedication(medication: MedicationEntity)

    @Delete
    suspend fun deleteMedication(medication: MedicationEntity)

    @Query("DELETE FROM medications")
    suspend fun deleteAllMedications()

    @Query("UPDATE medications SET isTakenToday = :isTaken, lastTakenTime = :takenTime WHERE id = :id")
    suspend fun updateDoseStatus(id: Long, isTaken: Boolean, takenTime: String?)
}
