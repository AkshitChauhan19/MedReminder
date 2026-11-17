package com.example.medreminder.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.medreminder.domain.repository.MedicationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: MedicationRepository

    @Inject
    lateinit var alarmScheduler: MedicationAlarmScheduler

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Device reboot detected ($action). Restoring all medication alarms...")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val meds = repository.getAllMedications().firstOrNull() ?: emptyList()
                    meds.forEach { med ->
                        alarmScheduler.scheduleMedicationAlarm(med)
                    }
                    Log.d(TAG, "Restored alarms for ${meds.size} medications after reboot.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore alarms after reboot: ${e.message}", e)
                }
            }
        }
    }
}
