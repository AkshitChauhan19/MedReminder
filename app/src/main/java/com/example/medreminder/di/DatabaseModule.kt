package com.example.medreminder.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.medreminder.data.local.MedReminderDatabase
import com.example.medreminder.data.local.dao.DoseLogDao
import com.example.medreminder.data.local.dao.MedicationDao
import com.example.medreminder.data.local.entity.DoseLogEntity
import com.example.medreminder.data.local.entity.MedicationEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        databaseProvider: Provider<MedReminderDatabase>
    ): MedReminderDatabase {
        return Room.databaseBuilder(
            context,
            MedReminderDatabase::class.java,
            "med_reminder_database"
        )
        .fallbackToDestructiveMigration()
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    val database = databaseProvider.get()
                    seedDatabase(database.medicationDao(), database.doseLogDao())
                }
            }
        })
        .build()
    }

    private suspend fun seedDatabase(medicationDao: MedicationDao, doseLogDao: DoseLogDao) {
        val med1 = MedicationEntity(
            id = 1,
            name = "Lisinopril",
            dosage = "10mg",
            form = "Tablet",
            frequency = "Daily",
            timeOfDay = "Morning",
            scheduledTime = "08:00 AM",
            remainingPills = 24,
            totalPills = 30,
            accentColorHex = "#009688",
            isTakenToday = true,
            lastTakenTime = "8:05 AM"
        )

        val med2 = MedicationEntity(
            id = 2,
            name = "Atorvastatin",
            dosage = "20mg",
            form = "Capsule",
            frequency = "Nightly",
            timeOfDay = "Morning",
            scheduledTime = "08:00 AM",
            remainingPills = 5,
            totalPills = 30,
            accentColorHex = "#E53935",
            isTakenToday = true,
            lastTakenTime = "8:06 AM"
        )

        val med3 = MedicationEntity(
            id = 3,
            name = "Metformin",
            dosage = "500mg",
            form = "Tablet",
            frequency = "Twice Daily",
            timeOfDay = "Afternoon",
            scheduledTime = "01:00 PM",
            remainingPills = 48,
            totalPills = 60,
            accentColorHex = "#7C4DFF",
            specialInstructions = "Take with food"
        )

        val med4 = MedicationEntity(
            id = 4,
            name = "Amlodipine",
            dosage = "5mg",
            form = "Tablet",
            frequency = "Daily",
            timeOfDay = "Evening",
            scheduledTime = "08:00 PM",
            remainingPills = 30,
            totalPills = 30,
            accentColorHex = "#5C6BC0"
        )

        val med5 = MedicationEntity(
            id = 5,
            name = "Amoxicillin",
            dosage = "500mg",
            form = "Capsule",
            frequency = "3 times a day",
            timeOfDay = "Morning",
            scheduledTime = "08:00 AM",
            remainingPills = 14,
            totalPills = 30,
            accentColorHex = "#7C4DFF",
            specialInstructions = "Take with food. To prevent stomach upset. Finish the entire prescribed course even if symptoms disappear.",
            prescriberName = "Dr. Sarah Jenkins",
            prescriberSpecialty = "General Practice",
            pharmacyName = "CityHealth Pharmacy",
            rxNumber = "89234100-B",
            refillsLeft = 0
        )

        medicationDao.insertMedication(med1)
        medicationDao.insertMedication(med2)
        medicationDao.insertMedication(med3)
        medicationDao.insertMedication(med4)
        medicationDao.insertMedication(med5)

        doseLogDao.insertDoseLog(
            DoseLogEntity(
                medicationId = 1,
                medicationName = "Took Vitamin D (2000 IU)",
                dosage = "2000 IU",
                timestamp = "8:05 AM",
                dayOfWeek = "Today",
                isTaken = true
            )
        )

        doseLogDao.insertDoseLog(
            DoseLogEntity(
                medicationId = 1,
                medicationName = "Took Lisinopril (10mg)",
                dosage = "10mg",
                timestamp = "7:30 AM",
                dayOfWeek = "Today",
                isTaken = true
            )
        )

        doseLogDao.insertDoseLog(
            DoseLogEntity(
                medicationId = 3,
                medicationName = "Missed Metformin (500mg)",
                dosage = "500mg",
                timestamp = "8:00 PM",
                dayOfWeek = "Yesterday",
                isTaken = false
            )
        )
    }

    @Provides
    fun provideMedicationDao(database: MedReminderDatabase): MedicationDao {
        return database.medicationDao()
    }

    @Provides
    fun provideDoseLogDao(database: MedReminderDatabase): DoseLogDao {
        return database.doseLogDao()
    }
}
