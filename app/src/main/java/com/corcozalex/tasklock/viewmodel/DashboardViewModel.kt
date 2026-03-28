package com.corcozalex.tasklock.viewmodel

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corcozalex.tasklock.network.NetworkClient
import com.corcozalex.tasklock.network.RepeatMode
import com.corcozalex.tasklock.network.Task
import com.corcozalex.tasklock.network.TaskCreateRequest
import com.corcozalex.tasklock.network.TaskMetadataCodec
import com.corcozalex.tasklock.network.UserProfile
import com.corcozalex.tasklock.receiver.AlarmReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class DashboardState {
    object Loading : DashboardState()
    data class Success(val userProfile: UserProfile, val tasks: List<Task>) : DashboardState()
    data class Error(val error: String) : DashboardState()
}

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    init {
        fetchUserProfile()
    }

    private fun fetchUserProfile() {
        _uiState.value = DashboardState.Loading
        viewModelScope.launch {
            try {
                val profile = NetworkClient.api.getMyProfile()
                val taskList = NetworkClient.api.getTasks()
                _uiState.value = DashboardState.Success(profile, taskList)
            } catch (e: Exception) {
                _uiState.value = DashboardState.Error(e.message ?: "Failed to load profile. Are you sure the token is valid?")
            }
        }
    }

    fun createTask(
        title: String,
        description: String,
        scheduledAtMillis: Long,
        repeatMode: RepeatMode,
        repeatDayOfWeek: Int?,
        requiredObject: String
    ) {
        _uiState.value = DashboardState.Loading
        viewModelScope.launch {
            try{
                val descToSave = TaskMetadataCodec.encode(
                    notes = description,
                    scheduledAtMillis = scheduledAtMillis,
                    repeatMode = repeatMode,
                    repeatDayOfWeek = repeatDayOfWeek,
                    requiredObject = requiredObject
                )
                val request = TaskCreateRequest(title = title, description = descToSave)
                NetworkClient.api.createTask(request)
                fetchUserProfile()
            } catch (e: Exception) {
                _uiState.value = DashboardState.Error("Failed to create task: ${e.message}")
            }
        }
    }
    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            try {
                val updatedTask = task.copy(is_completed = !task.is_completed)
                NetworkClient.api.updateTask(task.id, updatedTask)
                fetchUserProfile()
            } catch (e: Exception) {
                _uiState.value = DashboardState.Error("Failed to update task: ${e.message}")
            }
        }
    }
    fun deleteTask(taskId: Int) {
        _uiState.value = DashboardState.Loading
        viewModelScope.launch {
            try {
                NetworkClient.api.deleteTask(taskId)
                fetchUserProfile()
            } catch (e: Exception) {
                _uiState.value = DashboardState.Error("Failed to delete task: ${e.message}")
            }
        }
    }

    fun scheduleTestAlarm(context: Context){
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            val settingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            if (!notificationManager.canUseFullScreenIntent()) {
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = Calendar.getInstance().apply {
            add(Calendar.SECOND, 15)
        }
        val alarmClockInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }
}