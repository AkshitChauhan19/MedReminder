package com.example.medreminder.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.medreminder.data.local.dao.DoseLogDao
import com.example.medreminder.data.local.dao.MedicationDao
import com.example.medreminder.data.local.entity.DoseLogEntity
import com.example.medreminder.data.local.entity.MedicationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [MedicationEntity::class, DoseLogEntity::class],
    version = 2,
    exportSchema = false
)
abstract class MedReminderDatabase : RoomDatabase() {

    abstract fun medicationDao(): MedicationDao
    abstract fun doseLogDao(): DoseLogDao

    companion object {
        fun buildDatabase(context: Context, scope: CoroutineScope): MedReminderDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MedReminderDatabase::class.java,
                "med_reminder_database"
            )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                scope.launch(Dispatchers.IO) {
                    val database = INSTANCE ?: return@launch
                }
            }
        }

        @Volatile
        var INSTANCE: MedReminderDatabase? = null
    }
}
