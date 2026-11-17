package com.example.medreminder.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.medreminder.MainActivity
import com.example.medreminder.MedReminderApp
import com.example.medreminder.R
import com.example.medreminder.domain.model.Medication
import com.example.medreminder.domain.repository.MedicationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MedicationAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: MedicationRepository

    @Inject
    lateinit var alarmScheduler: MedicationAlarmScheduler

    companion object {
        private const val TAG = "MedAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "onReceive triggered with action: $action")

        val medId = intent.getLongExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_ID, -1L)
        val medName = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_NAME) ?: "Medication"
        val dosage = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_DOSAGE) ?: ""
        val scheduledTime = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_SCHEDULED_TIME) ?: "08:00 AM"
        val form = intent.getStringExtra(MedicationAlarmScheduler.EXTRA_FORM) ?: "Dose"

        val notificationId = if (medId > 0) (medId % Int.MAX_VALUE).toInt() else 1001

        when (action) {
            MedicationAlarmScheduler.ACTION_MARK_TAKEN -> {
                Log.d(TAG, "Mark as Taken clicked for medId: $medId")
                // Dismiss the notification
                try {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error cancelling notification: ${e.message}")
                }

                // Update database
                if (medId > 0) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            repository.toggleDoseTaken(medId, true)
                            Log.d(TAG, "Successfully marked medId $medId as taken in repository")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to toggle dose taken: ${e.message}", e)
                        }
                    }
                }
            }

            MedicationAlarmScheduler.ACTION_TRIGGER_ALARM -> {
                Log.d(TAG, "Alarm triggered for: $medName ($dosage) at $scheduledTime")

                // Open App Intent
                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val contentPendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Action Intent: Mark as Taken
                val markTakenIntent = Intent(context, MedicationAlarmReceiver::class.java).apply {
                    this.action = MedicationAlarmScheduler.ACTION_MARK_TAKEN
                    putExtra(MedicationAlarmScheduler.EXTRA_MEDICATION_ID, medId)
                }
                val markTakenPendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId + 100000,
                    markTakenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val contentText = if (dosage.isNotBlank()) {
                    "Time to take $dosage of $medName ($scheduledTime)"
                } else {
                    "Time to take your scheduled dose of $medName ($scheduledTime)"
                }

                val notification = NotificationCompat.Builder(context, MedReminderApp.REMINDER_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_app_icon)
                    .setContentTitle("⏰ Medication Reminder: $medName")
                    .setContentText(contentText)
                    .setStyle(NotificationCompat.BigTextStyle().bigText("$contentText\nForm: $form\nTap to open Med Reminder or tap below to mark as taken."))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(true)
                    .setContentIntent(contentPendingIntent)
                    .addAction(
                        android.R.drawable.checkbox_on_background,
                        "Mark as Taken",
                        markTakenPendingIntent
                    )
                    .build()

                try {
                    NotificationManagerCompat.from(context).notify(notificationId, notification)
                    Log.d(TAG, "Notification posted successfully for $medName")
                } catch (e: SecurityException) {
                    Log.e(TAG, "Notification permission missing: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to post notification: ${e.message}", e)
                }

                // Reschedule for tomorrow at the same scheduled time
                if (medId > 0) {
                    val dummyMed = Medication(
                        id = medId,
                        name = medName,
                        dosage = dosage,
                        form = form,
                        frequency = "Daily",
                        timeOfDay = "Morning",
                        scheduledTime = scheduledTime,
                        remainingPills = 10,
                        totalPills = 30,
                        accentColorHex = "#009688"
                    )
                    alarmScheduler.scheduleMedicationAlarm(dummyMed)
                }
            }
        }
    }
}
