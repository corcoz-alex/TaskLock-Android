package com.corcozalex.tasklock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.corcozalex.tasklock.service.AlarmService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("TaskLockAlarm", "ALARM TRIGGERED! Catching the broadcast...")

        // hold the CPU awake just long enough for the Service to take over
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TaskLock::ReceiverWakeLock"
        )
        wakeLock.acquire(5000) // Hold CPU awake for exactly 5 seconds

        val serviceIntent = Intent(context, AlarmService::class.java)
        context.startForegroundService(serviceIntent)
    }
}