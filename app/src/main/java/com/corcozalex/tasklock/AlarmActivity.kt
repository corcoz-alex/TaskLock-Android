package com.corcozalex.tasklock

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.corcozalex.tasklock.network.NetworkClient
import com.corcozalex.tasklock.network.RepeatMode
import com.corcozalex.tasklock.network.TaskCompletionRules
import com.corcozalex.tasklock.network.TaskMetadataCodec
import com.corcozalex.tasklock.network.TaskUpdateRequest
import com.corcozalex.tasklock.scheduler.EXTRA_TASK_DESCRIPTION
import com.corcozalex.tasklock.scheduler.EXTRA_TASK_ID
import com.corcozalex.tasklock.scheduler.EXTRA_TASK_TITLE
import com.corcozalex.tasklock.service.AlarmService
import com.corcozalex.tasklock.ui.screens.AlarmActiveScreen
import com.corcozalex.tasklock.ui.theme.TaskLockTheme
import kotlinx.coroutines.launch

class AlarmActivity : ComponentActivity() {

    @Volatile
    private var completionRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetworkClient.initialize(applicationContext)
        configureAlarmWindow()

        setContent {
            TaskLockTheme {
                AlarmActiveScreen(
                    onTaskCompleted = { completeTriggeredTaskAndExit() },
                    onEmergencyStop = { completeTriggeredTaskAndExit() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        completionRequested = false
        configureAlarmWindow()
    }

    private fun completeTriggeredTaskAndExit() {
        if (completionRequested) return
        completionRequested = true

        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        if (taskId < 0) {
            exitAlarmScreen()
            return
        }

        val title = intent.getStringExtra(EXTRA_TASK_TITLE).orEmpty().ifBlank { "Task" }
        val rawDescription = intent.getStringExtra(EXTRA_TASK_DESCRIPTION)
        val metadata = TaskMetadataCodec.decode(rawDescription)
        val nowMillis = System.currentTimeMillis()

        val updatedDescription = if (metadata != null) {
            val temporaryUntil = TaskCompletionRules.completionUntilAfterSuccess(metadata, nowMillis)
            TaskMetadataCodec.encode(
                notes = metadata.notes,
                scheduledAtMillis = metadata.scheduledAtMillis,
                repeatMode = metadata.repeatMode,
                repeatDayOfWeek = metadata.repeatDayOfWeek,
                requiredObject = metadata.requiredObject,
                completedUntilMillis = temporaryUntil
            )
        } else {
            rawDescription
        }

        val markCompletedPermanently = metadata?.repeatMode == RepeatMode.NONE || metadata == null

        lifecycleScope.launch {
            runCatching {
                NetworkClient.api.updateTask(
                    taskId,
                    TaskUpdateRequest(
                        title = title,
                        description = updatedDescription,
                        is_completed = markCompletedPermanently
                    )
                )
            }
            exitAlarmScreen()
        }
    }

    private fun exitAlarmScreen() {
        stopService(Intent(this, AlarmService::class.java))
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(mainIntent)
        finish()
    }

    private fun configureAlarmWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
