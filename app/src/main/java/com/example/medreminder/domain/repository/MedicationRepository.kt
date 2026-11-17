package com.example.medreminder.domain.repository

import com.example.medreminder.domain.model.AdherenceStats
import com.example.medreminder.domain.model.DoseLog
import com.example.medreminder.domain.model.Medication
import kotlinx.coroutines.flow.Flow

interface MedicationRepository{
    fun getAllMedications() : Flow<List<Medication>>
    fun getMedicationById(id: Long) : Flow<Medication?>
    fun getTodaySchedule() : Flow<List<Medication>>
    fun getRecentDoseLogs(): Flow<List<DoseLog>>
    fun getAdherenceStats() : Flow<AdherenceStats>

    suspend fun toggleDoseTaken(medicationId : Long, isTaken : Boolean)
    suspend fun insertMedication(medication: Medication): Long
    suspend fun deleteMedication(medication: Medication)
    suspend fun fetchDrugInfoFromNetwork(drugName : String): Result<String>
}
