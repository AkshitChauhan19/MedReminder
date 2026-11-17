package com.example.medreminder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

/**
 * APPLICATION ENTRY POINT (HILT DI CONTAINER)
 * 
 * The @HiltAndroidApp annotation triggers Hilt's code generation,
 * creating an application-level dependency container (SingletonComponent).
 */
@HiltAndroidApp
class MedReminderApp : Application() {

    companion object {
        const val REMINDER_CHANNEL_ID = "medication_reminders"
        const val REMINDER_CHANNEL_NAME = "Medication Reminders"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                REMINDER_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily medication reminder notifications and dose alerts"
                enableLights(true)
                enableVibration(true)
                setSound(soundUri, audioAttributes)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
