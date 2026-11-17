package com.example.medreminder.data.repository

import com.example.medreminder.data.local.dao.DoseLogDao
import com.example.medreminder.data.local.dao.MedicationDao
import com.example.medreminder.data.local.entity.DoseLogEntity
import com.example.medreminder.data.local.entity.MedicationEntity
import com.example.medreminder.data.local.entity.toDomainModel
import com.example.medreminder.data.local.entity.toEntity
import com.example.medreminder.data.remote.DrugApiService
import com.example.medreminder.domain.model.AdherenceStats
import com.example.medreminder.domain.model.DayAdherence
import com.example.medreminder.domain.model.DoseLog
import com.example.medreminder.domain.model.Medication
import com.example.medreminder.domain.repository.MedicationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import com.example.medreminder.notifications.MedicationAlarmScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val medicationDao: MedicationDao,
    private val doseLogDao: DoseLogDao,
    private val drugApiService: DrugApiService,
    private val alarmScheduler: MedicationAlarmScheduler
) : MedicationRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private var firestoreListener: ListenerRegistration? = null

    private fun getActiveUserId(): String? {
        val user = auth.currentUser
        if (user != null && user.uid.isNotBlank()) return user.uid
        val prefs = context.getSharedPreferences("med_auth_prefs", Context.MODE_PRIVATE)
        return prefs.getString("saved_uid", null)
    }

    init {
        // Automatically sync cloud medications whenever user logs in or app starts
        getActiveUserId()?.let { uid ->
            listenToCloudMedications(uid)
        }

        auth.addAuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            val uid = currentUser?.uid ?: getActiveUserId()
            if (uid != null) {
                listenToCloudMedications(uid)
            }
        }
    }

    /**
     * Listens to real-time Firebase Firestore cloud storage under users/{uid}/medications.
     * Automatically handles daily midnight reset and downloads all medications into Room when signing in.
     */
    private fun listenToCloudMedications(userId: String) {
        firestoreListener?.remove()
        try {
            val collectionRef = firestore.collection("users").document(userId).collection("medications")
            firestoreListener = collectionRef.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    android.util.Log.e("MedicationRepo", "Firestore snapshot listener error: ${error?.message}")
                    return@addSnapshotListener
                }
                repositoryScope.launch {
                    val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val cloudEntities = snapshot.documents.mapNotNull { doc ->
                        val entity = firestoreDocumentToEntity(doc) ?: return@mapNotNull null
                        val lastTakenDate = doc.getString("lastTakenDate") ?: ""
                        // Reset isTakenToday if date has changed (midnight daily refresh)
                        if (lastTakenDate.isNotBlank() && lastTakenDate != todayDateStr) {
                            entity.copy(isTakenToday = false)
                        } else {
                            entity
                        }
                    }

                    // If cloud has entries, sync down to Room cache
                    if (cloudEntities.isNotEmpty()) {
                        medicationDao.deleteAllMedications()
                        cloudEntities.forEach { entity ->
                            medicationDao.insertMedication(entity)
                            alarmScheduler.scheduleMedicationAlarm(entity.toDomainModel())
                        }
                        android.util.Log.d("MedicationRepo", "Synced ${cloudEntities.size} medications from Cloud Firestore to local cache and scheduled alarms.")
                    } else {
                        // If cloud is initially empty but user already has medications locally, upload them to Firestore
                        val localMeds = medicationDao.getAllMedications().firstOrNull() ?: emptyList()
                        if (localMeds.isNotEmpty()) {
                            localMeds.forEach { entity ->
                                val med = entity.toDomainModel()
                                collectionRef.document(med.id.toString()).set(med.toFirestoreMap())
                            }
                            android.util.Log.d("MedicationRepo", "Uploaded ${localMeds.size} local medications to freshly created Cloud Firestore.")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MedicationRepo", "Failed to attach Firestore listener: ${e.message}", e)
        }
    }

    override fun getAllMedications(): Flow<List<Medication>> {
        return medicationDao.getAllMedications().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getMedicationById(id: Long): Flow<Medication?> {
        return medicationDao.getMedicationById(id).map { it?.toDomainModel() }
    }

    override fun getTodaySchedule(): Flow<List<Medication>> {
        return medicationDao.getTodaySchedule().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getRecentDoseLogs(): Flow<List<DoseLog>> {
        return doseLogDao.getAllDoseLogs().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getAdherenceStats(): Flow<AdherenceStats> {
        return combine(medicationDao.getAllMedications(), doseLogDao.getAllDoseLogs()) { meds, logs ->
            val totalMeds = meds.size
            val takenTodayCount = meds.count { it.isTakenToday }

            val cal = Calendar.getInstance()
            cal.firstDayOfWeek = Calendar.MONDAY
            
            val currentDayIdx = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> 0
            }

            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val dayNames = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

            val todayCal = Calendar.getInstance()
            val todayDayNum = todayCal.get(Calendar.DAY_OF_MONTH)
            val todayMonthNum = todayCal.get(Calendar.MONTH)

            var perfectDaysCount = 0
            var activeDaysElapsed = 0

            val weeklyRecord = dayNames.mapIndexed { index, name ->
                val dayNum = cal.get(Calendar.DAY_OF_MONTH)
                val isToday = (dayNum == todayDayNum && cal.get(Calendar.MONTH) == todayMonthNum)
                val isPast = index < currentDayIdx

                val isComplete: Boolean
                val isMissed: Boolean

                if (isToday) {
                    isComplete = (takenTodayCount == totalMeds && totalMeds > 0)
                    isMissed = false
                    if (totalMeds > 0) {
                        activeDaysElapsed++
                        if (isComplete) perfectDaysCount++
                    }
                } else if (isPast) {
                    val dayLogs = logs.filter { it.dayOfWeek.equals(name, ignoreCase = true) }
                    val takenLogsCount = dayLogs.count { it.isTaken }
                    if (dayLogs.isNotEmpty()) {
                        isComplete = takenLogsCount >= totalMeds && totalMeds > 0
                        isMissed = !isComplete && dayLogs.any { !it.isTaken }
                        activeDaysElapsed++
                        if (isComplete) perfectDaysCount++
                    } else {
                        // Past day before user started or created meds — do not penalize as 0% missed
                        isComplete = false
                        isMissed = false
                    }
                } else {
                    isComplete = false
                    isMissed = false
                }

                val item = DayAdherence(
                    dayName = name,
                    dayNumber = dayNum,
                    isComplete = isComplete,
                    isMissed = isMissed,
                    isToday = isToday
                )
                cal.add(Calendar.DAY_OF_MONTH, 1)
                item
            }

            val elapsed = activeDaysElapsed.coerceAtLeast(1)
            val weeklyPercentage = if (totalMeds == 0) 100 else ((perfectDaysCount.toFloat() / elapsed.toFloat()) * 100).toInt()

            AdherenceStats(
                weeklyPercentage = weeklyPercentage,
                perfectDaysCount = perfectDaysCount,
                totalDaysCount = elapsed,
                goalPercentage = 95,
                weeklyRecord = weeklyRecord
            )
        }
    }

    override suspend fun toggleDoseTaken(medicationId: Long, isTaken: Boolean) {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())

        val currentTimeStr = timeFormat.format(Date())
        val currentDateStr = dateFormat.format(Date())
        val currentDayOfWeek = dayOfWeekFormat.format(Date()).uppercase()

        val currentEntity = medicationDao.getMedicationById(medicationId).firstOrNull()
        if (currentEntity != null) {
            // Decrement remaining pills when checked off, increment back if unchecked
            val currentPills = currentEntity.remainingPills
            val updatedPills = if (isTaken) {
                maxOf(0, currentPills - 1)
            } else {
                currentPills + 1
            }

            val updatedEntity = currentEntity.copy(
                isTakenToday = isTaken,
                lastTakenTime = if (isTaken) currentTimeStr else null,
                remainingPills = updatedPills
            )

            // 1. Update Room DB
            medicationDao.insertMedication(updatedEntity)

            // 2. Clear old duplicate logs for this medication today
            doseLogDao.deleteLogForMedicationAndDay(medicationId, currentDayOfWeek)

            // 3. Insert ONE clean, non-repetitive Dose Log into history database
            if (isTaken) {
                doseLogDao.insertDoseLog(
                    DoseLogEntity(
                        medicationId = medicationId,
                        medicationName = currentEntity.name,
                        dosage = currentEntity.dosage,
                        timestamp = currentTimeStr,
                        dayOfWeek = currentDayOfWeek,
                        isTaken = true
                    )
                )
            }

            // 4. Sync to Firebase Firestore Account Cloud Storage
            getActiveUserId()?.let { uid ->
                try {
                    val docRef = firestore.collection("users").document(uid)
                        .collection("medications").document(medicationId.toString())
                    val updatedMed = updatedEntity.toDomainModel()
                    val map = updatedMed.toFirestoreMap().toMutableMap()
                    map["lastTakenDate"] = currentDateStr
                    docRef.set(map).await()
                    android.util.Log.d("MedicationRepo", "Updated medication $medicationId in Firestore for user $uid")
                } catch (e: Exception) {
                    android.util.Log.e("MedicationRepo", "Failed to update medication in Firestore: ${e.message}", e)
                }
            }
        }
    }

    override suspend fun insertMedication(medication: Medication): Long {
        val generatedId = if (medication.id == 0L) System.currentTimeMillis() else medication.id
        val finalMed = medication.copy(id = generatedId)

        // 1. Save to Room Local Cache
        val localId = medicationDao.insertMedication(finalMed.toEntity())

        // 2. Schedule Exact Alarm for this medication
        alarmScheduler.scheduleMedicationAlarm(finalMed)

        // 3. Save to Firebase Firestore Account Cloud Storage (Bound to user's UID)
        getActiveUserId()?.let { uid ->
            try {
                firestore.collection("users").document(uid)
                    .collection("medications").document(generatedId.toString())
                    .set(finalMed.toFirestoreMap())
                    .await()
                android.util.Log.d("MedicationRepo", "Saved medication ${finalMed.name} to Firestore for user $uid")
            } catch (e: Exception) {
                android.util.Log.e("MedicationRepo", "Failed to save medication to Firestore: ${e.message}", e)
            }
        }

        return localId
    }

    override suspend fun deleteMedication(medication: Medication) {
        // 1. Cancel Exact Alarm
        alarmScheduler.cancelMedicationAlarm(medication.id)

        // 2. Delete from Room Local Cache
        medicationDao.deleteMedication(medication.toEntity())
        doseLogDao.deleteLogsForMedication(medication.id)

        // 3. Delete from Firebase Firestore Account Cloud Storage
        getActiveUserId()?.let { uid ->
            try {
                firestore.collection("users").document(uid)
                    .collection("medications").document(medication.id.toString())
                    .delete()
                    .await()
                android.util.Log.d("MedicationRepo", "Deleted medication ${medication.id} from Firestore for user $uid")
            } catch (e: Exception) {
                android.util.Log.e("MedicationRepo", "Failed to delete medication from Firestore: ${e.message}", e)
            }
        }
    }

    override suspend fun fetchDrugInfoFromNetwork(drugName: String): Result<String> {
        return try {
            val response = drugApiService.searchDrugLabel("openfda.brand_name:$drugName")
            val purpose = response.results?.firstOrNull()?.purpose?.firstOrNull()
                ?: "FDA Approved Prescription Medication for therapeutic treatment."
            Result.success(purpose)
        } catch (e: Exception) {
            Result.success("FDA Approved Prescription Medication for therapeutic treatment.")
        }
    }

    private fun Medication.toFirestoreMap(): Map<String, Any?> {
        val currentDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return mapOf(
            "id" to id,
            "name" to name,
            "dosage" to dosage,
            "form" to form,
            "frequency" to frequency,
            "timeOfDay" to timeOfDay,
            "scheduledTime" to scheduledTime,
            "remainingPills" to remainingPills,
            "totalPills" to totalPills,
            "accentColorHex" to accentColorHex,
            "isTakenToday" to isTakenToday,
            "lastTakenTime" to lastTakenTime,
            "lastTakenDate" to if (isTakenToday) currentDateStr else null,
            "prescriberName" to prescriberName,
            "prescriberSpecialty" to prescriberSpecialty,
            "prescriberPhone" to prescriberPhone,
            "pharmacyName" to pharmacyName,
            "pharmacyAddress" to pharmacyAddress,
            "rxNumber" to rxNumber,
            "refillsLeft" to refillsLeft
        )
    }

    private fun firestoreDocumentToEntity(doc: DocumentSnapshot): MedicationEntity? {
        val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: return null
        return MedicationEntity(
            id = id,
            name = doc.getString("name") ?: "",
            dosage = doc.getString("dosage") ?: "",
            form = doc.getString("form") ?: "Tablet",
            frequency = doc.getString("frequency") ?: "Daily",
            timeOfDay = doc.getString("timeOfDay") ?: "Morning",
            scheduledTime = doc.getString("scheduledTime") ?: "08:00 AM",
            remainingPills = doc.getLong("remainingPills")?.toInt() ?: 30,
            totalPills = doc.getLong("totalPills")?.toInt() ?: 30,
            accentColorHex = doc.getString("accentColorHex") ?: "#009688",
            isTakenToday = doc.getBoolean("isTakenToday") ?: false,
            lastTakenTime = doc.getString("lastTakenTime"),
            prescriberName = doc.getString("prescriberName") ?: "",
            prescriberSpecialty = doc.getString("prescriberSpecialty") ?: "",
            prescriberPhone = doc.getString("prescriberPhone") ?: "",
            pharmacyName = doc.getString("pharmacyName") ?: "",
            pharmacyAddress = doc.getString("pharmacyAddress") ?: "",
            rxNumber = doc.getString("rxNumber") ?: "",
            refillsLeft = doc.getLong("refillsLeft")?.toInt() ?: 0
        )
    }
}
