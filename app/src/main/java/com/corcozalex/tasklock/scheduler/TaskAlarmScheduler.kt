package com.corcozalex.tasklock.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.corcozalex.tasklock.network.RepeatMode
import com.corcozalex.tasklock.network.Task
import com.corcozalex.tasklock.network.TaskMetadataCodec
import com.corcozalex.tasklock.receiver.AlarmReceiver
import java.util.Calendar

private const val TASK_ALARM_ACTION = "com.corcozalex.tasklock.ACTION_TASK_ALARM"

const val EXTRA_TASK_ID = "extra_task_id"
const val EXTRA_TASK_TITLE = "extra_task_title"
const val EXTRA_TASK_DESCRIPTION = "extra_task_description"
const val EXTRA_REQUIRED_OBJECT = "extra_required_object"
const val EXTRA_REPEAT_MODE = "extra_repeat_mode"
const val EXTRA_REPEAT_DAY_OF_WEEK = "extra_repeat_day_of_week"
const val EXTRA_SCHEDULED_AT_MILLIS = "extra_scheduled_at_millis"

object TaskAlarmScheduler {

    fun syncTaskAlarms(context: Context, tasks: List<Task>) {
        tasks.forEach { task ->
            scheduleTaskAlarm(context, task)
        }
    }

    fun scheduleTaskAlarm(context: Context, task: Task) {
        val metadata = TaskMetadataCodec.decode(task.description)
        if (metadata == null || task.is_completed) {
            cancelTaskAlarm(context, task.id)
            return
        }

        val triggerAtMillis = nextTriggerAtMillis(
            scheduledAtMillis = metadata.scheduledAtMillis,
            repeatMode = metadata.repeatMode,
            repeatDayOfWeek = metadata.repeatDayOfWeek,
            nowMillis = System.currentTimeMillis()
        )

        if (triggerAtMillis == null) {
            cancelTaskAlarm(context, task.id)
            return
        }

        val pendingIntent = buildTaskPendingIntent(
            context = context,
            taskId = task.id,
            taskTitle = task.title,
            taskDescription = task.description,
            requiredObject = metadata.requiredObject,
            scheduledAtMillis = metadata.scheduledAtMillis,
            repeatMode = metadata.repeatMode,
            repeatDayOfWeek = metadata.repeatDayOfWeek,
            updateCurrent = true
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }

    fun cancelTaskAlarm(context: Context, taskId: Int) {
        val pendingIntent = buildTaskPendingIntent(
            context = context,
            taskId = taskId,
            taskTitle = null,
            taskDescription = null,
            requiredObject = null,
            scheduledAtMillis = null,
            repeatMode = null,
            repeatDayOfWeek = null,
            updateCurrent = false
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun rescheduleFromTrigger(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        if (taskId < 0) return

        val repeatMode = intent.getStringExtra(EXTRA_REPEAT_MODE)
            ?.let { runCatching { RepeatMode.valueOf(it) }.getOrNull() }
            ?: RepeatMode.NONE
        if (repeatMode == RepeatMode.NONE) return

        val scheduledAtMillis = intent.getLongExtra(EXTRA_SCHEDULED_AT_MILLIS, -1L)
        if (scheduledAtMillis <= 0L) return

        val repeatDay = intent.getIntExtra(EXTRA_REPEAT_DAY_OF_WEEK, 0).takeIf { it in 1..7 }
        val title = intent.getStringExtra(EXTRA_TASK_TITLE)
        val description = intent.getStringExtra(EXTRA_TASK_DESCRIPTION)
        val requiredObject = intent.getStringExtra(EXTRA_REQUIRED_OBJECT)

        val nextTriggerAtMillis = nextTriggerAtMillis(
            scheduledAtMillis = scheduledAtMillis,
            repeatMode = repeatMode,
            repeatDayOfWeek = repeatDay,
            nowMillis = System.currentTimeMillis()
        ) ?: return

        val pendingIntent = buildTaskPendingIntent(
            context = context,
            taskId = taskId,
            taskTitle = title,
            taskDescription = description,
            requiredObject = requiredObject,
            scheduledAtMillis = scheduledAtMillis,
            repeatMode = repeatMode,
            repeatDayOfWeek = repeatDay,
            updateCurrent = true
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmClockInfo = AlarmManager.AlarmClockInfo(nextTriggerAtMillis, pendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }

    private fun nextTriggerAtMillis(
        scheduledAtMillis: Long,
        repeatMode: RepeatMode,
        repeatDayOfWeek: Int?,
        nowMillis: Long
    ): Long? {
        val base = Calendar.getInstance().apply { timeInMillis = scheduledAtMillis }

        return when (repeatMode) {
            RepeatMode.NONE -> scheduledAtMillis.takeIf { it > nowMillis }
            RepeatMode.DAILY -> {
                val candidate = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    set(Calendar.HOUR_OF_DAY, base.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, base.get(Calendar.MINUTE))
                    set(Calendar.SECOND, base.get(Calendar.SECOND))
                    set(Calendar.MILLISECOND, base.get(Calendar.MILLISECOND))
                }
                if (candidate.timeInMillis <= nowMillis) {
                    candidate.add(Calendar.DAY_OF_YEAR, 1)
                }
                candidate.timeInMillis
            }
            RepeatMode.WEEKLY -> {
                val targetDay = repeatDayOfWeek ?: base.get(Calendar.DAY_OF_WEEK)
                val candidate = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    set(Calendar.HOUR_OF_DAY, base.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, base.get(Calendar.MINUTE))
                    set(Calendar.SECOND, base.get(Calendar.SECOND))
                    set(Calendar.MILLISECOND, base.get(Calendar.MILLISECOND))
                }

                var dayOffset = (targetDay - candidate.get(Calendar.DAY_OF_WEEK) + 7) % 7
                if (dayOffset == 0 && candidate.timeInMillis <= nowMillis) {
                    dayOffset = 7
                }
                candidate.add(Calendar.DAY_OF_YEAR, dayOffset)
                candidate.timeInMillis
            }
            RepeatMode.MONTHLY -> {
                val candidate = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    val baseDay = base.get(Calendar.DAY_OF_MONTH)
                    set(Calendar.DAY_OF_MONTH, baseDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
                    set(Calendar.HOUR_OF_DAY, base.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, base.get(Calendar.MINUTE))
                    set(Calendar.SECOND, base.get(Calendar.SECOND))
                    set(Calendar.MILLISECOND, base.get(Calendar.MILLISECOND))
                }
                if (candidate.timeInMillis <= nowMillis) {
                    candidate.add(Calendar.MONTH, 1)
                    val baseDay = base.get(Calendar.DAY_OF_MONTH)
                    val maxDay = candidate.getActualMaximum(Calendar.DAY_OF_MONTH)
                    candidate.set(Calendar.DAY_OF_MONTH, baseDay.coerceAtMost(maxDay))
                }
                candidate.timeInMillis
            }
        }
    }

    private fun buildTaskPendingIntent(
        context: Context,
        taskId: Int,
        taskTitle: String?,
        taskDescription: String?,
        requiredObject: String?,
        scheduledAtMillis: Long?,
        repeatMode: RepeatMode?,
        repeatDayOfWeek: Int?,
        updateCurrent: Boolean
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = TASK_ALARM_ACTION
            putExtra(EXTRA_TASK_ID, taskId)
            taskTitle?.let { putExtra(EXTRA_TASK_TITLE, it) }
            taskDescription?.let { putExtra(EXTRA_TASK_DESCRIPTION, it) }
            requiredObject?.let { putExtra(EXTRA_REQUIRED_OBJECT, it) }
            scheduledAtMillis?.let { putExtra(EXTRA_SCHEDULED_AT_MILLIS, it) }
            repeatMode?.let { putExtra(EXTRA_REPEAT_MODE, it.name) }
            repeatDayOfWeek?.let { putExtra(EXTRA_REPEAT_DAY_OF_WEEK, it) }
        }

        val flags = if (updateCurrent) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        }

        return PendingIntent.getBroadcast(context, taskId, intent, flags)
            ?: PendingIntent.getBroadcast(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
    }
}

