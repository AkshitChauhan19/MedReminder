package com.example.medreminder.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.medreminder.MainActivity
import com.example.medreminder.domain.model.Medication
import com.example.medreminder.util.TimeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    companion object {
        private const val TAG = "MedAlarmScheduler"
        const val ACTION_TRIGGER_ALARM = "com.example.medreminder.TRIGGER_MEDICATION_ALARM"
        const val ACTION_MARK_TAKEN = "com.example.medreminder.ACTION_MARK_TAKEN"
        const val EXTRA_MEDICATION_ID = "extra_medication_id"
        const val EXTRA_MEDICATION_NAME = "extra_medication_name"
        const val EXTRA_DOSAGE = "extra_dosage"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
        const val EXTRA_FORM = "extra_form"
    }

    /**
     * Schedules a precise exact alarm for the medication.
     */
    fun scheduleMedicationAlarm(medication: Medication) {
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager service unavailable")
            return
        }

        val (hour, minute) = TimeUtils.getHourAndMinute(medication.scheduledTime)
        val now = Calendar.getInstance()
        val alarmCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the scheduled time for today has already elapsed, schedule for tomorrow
        if (alarmCal.timeInMillis <= now.timeInMillis) {
            alarmCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        val requestCode = (medication.id % Int.MAX_VALUE).toInt()

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
            putExtra(EXTRA_MEDICATION_ID, medication.id)
            putExtra(EXTRA_MEDICATION_NAME, medication.name)
            putExtra(EXTRA_DOSAGE, medication.dosage)
            putExtra(EXTRA_SCHEDULED_TIME, TimeUtils.formatTo12Hour(medication.scheduledTime))
            putExtra(EXTRA_FORM, medication.form)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    val alarmClockInfo = AlarmManager.AlarmClockInfo(alarmCal.timeInMillis, showPendingIntent)
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                    Log.d(TAG, "Exact alarm clock set for '${medication.name}' at ${alarmCal.time}")
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmCal.timeInMillis, pendingIntent)
                    Log.d(TAG, "Inexact idle alarm set for '${medication.name}' at ${alarmCal.time}")
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(alarmCal.timeInMillis, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                Log.d(TAG, "Exact alarm clock set for '${medication.name}' at ${alarmCal.time}")
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmCal.timeInMillis, pendingIntent)
                Log.d(TAG, "Legacy exact alarm set for '${medication.name}' at ${alarmCal.time}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm for ${medication.name}: ${e.message}", e)
        }
    }

    /**
     * Cancels an existing alarm for the specified medication ID.
     */
    fun cancelMedicationAlarm(medicationId: Long) {
        if (alarmManager == null) return
        val requestCode = (medicationId % Int.MAX_VALUE).toInt()
        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for medication ID: $medicationId")
        }
    }
}
