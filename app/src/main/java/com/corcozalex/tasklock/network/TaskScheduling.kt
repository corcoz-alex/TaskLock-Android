package com.corcozalex.tasklock.network

import java.net.URLDecoder
import java.net.URLEncoder

private const val TASK_METADATA_PREFIX = "__TASKLOCK_META__"

enum class RepeatMode {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY
}

data class TaskScheduleMetadata(
    val scheduledAtMillis: Long,
    val repeatMode: RepeatMode,
    val repeatDayOfWeek: Int?,
    val requiredObject: String,
    val completedUntilMillis: Long?,
    val notes: String?
)

val COMMON_REQUIRED_OBJECTS = listOf(
    "Toothbrush",
    "Backpack",
    "Laptop",
    "Shoes",
)

object TaskMetadataCodec {
    fun encode(
        notes: String?,
        scheduledAtMillis: Long,
        repeatMode: RepeatMode,
        repeatDayOfWeek: Int?,
        requiredObject: String,
        completedUntilMillis: Long? = null
    ): String {
        val encodedObject = URLEncoder.encode(requiredObject, Charsets.UTF_8.name())
        val day = repeatDayOfWeek ?: 0
        val completedUntilPart = completedUntilMillis ?: 0L
        val header = "$TASK_METADATA_PREFIX|$scheduledAtMillis|${repeatMode.name}|$day|$encodedObject|$completedUntilPart"
        return if (notes.isNullOrBlank()) {
            header
        } else {
            "$header\n${notes.trim()}"
        }
    }

    fun decode(description: String?): TaskScheduleMetadata? {
        if (description.isNullOrBlank()) return null

        val firstLine = description.lineSequence().firstOrNull()?.trim() ?: return null
        if (!firstLine.startsWith(TASK_METADATA_PREFIX)) return null

        val parts = firstLine.split('|')
        if (parts.size < 5) return null

        val scheduledAtMillis = parts[1].toLongOrNull() ?: return null
        val repeatMode = runCatching { RepeatMode.valueOf(parts[2]) }.getOrNull() ?: RepeatMode.NONE
        val repeatDay = parts[3].toIntOrNull()?.takeIf { it in 1..7 }
        val requiredObject = runCatching {
            URLDecoder.decode(parts[4], Charsets.UTF_8.name())
        }.getOrDefault("Object")
        val completedUntilMillis = parts.getOrNull(5)
            ?.toLongOrNull()
            ?.takeIf { it > 0L }

        val notes = description
            .lineSequence()
            .drop(1)
            .joinToString("\n")
            .trim()
            .ifBlank { null }

        return TaskScheduleMetadata(
            scheduledAtMillis = scheduledAtMillis,
            repeatMode = repeatMode,
            repeatDayOfWeek = repeatDay,
            requiredObject = requiredObject,
            completedUntilMillis = completedUntilMillis,
            notes = notes
        )
    }

    fun plainDescription(description: String?): String? {
        val decodedNotes = decode(description)?.notes
        if (!decodedNotes.isNullOrBlank()) return decodedNotes

        val trimmed = description?.trim().orEmpty()
        if (trimmed.startsWith(TASK_METADATA_PREFIX)) {
            return null
        }
        return trimmed.ifBlank { null }
    }
}

object TaskCompletionRules {
    fun isTemporarilyCompleted(metadata: TaskScheduleMetadata, nowMillis: Long = System.currentTimeMillis()): Boolean {
        return metadata.completedUntilMillis?.let { it > nowMillis } == true
    }

    fun completionUntilAfterSuccess(
        metadata: TaskScheduleMetadata,
        nowMillis: Long = System.currentTimeMillis()
    ): Long? {
        return when (metadata.repeatMode) {
            RepeatMode.NONE -> null
            RepeatMode.DAILY -> nextDailyOccurrence(metadata.scheduledAtMillis, nowMillis)
            RepeatMode.WEEKLY -> nextWeeklyOccurrence(
                metadata.scheduledAtMillis,
                metadata.repeatDayOfWeek,
                nowMillis
            )
            RepeatMode.MONTHLY -> {
                val next = nextMonthlyOccurrence(metadata.scheduledAtMillis, nowMillis)
                (next - TWO_DAYS_MILLIS).coerceAtLeast(nowMillis)
            }
        }
    }

    private fun nextDailyOccurrence(scheduledAtMillis: Long, nowMillis: Long): Long {
        val base = java.util.Calendar.getInstance().apply { timeInMillis = scheduledAtMillis }
        val candidate = java.util.Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(java.util.Calendar.HOUR_OF_DAY, base.get(java.util.Calendar.HOUR_OF_DAY))
            set(java.util.Calendar.MINUTE, base.get(java.util.Calendar.MINUTE))
            set(java.util.Calendar.SECOND, base.get(java.util.Calendar.SECOND))
            set(java.util.Calendar.MILLISECOND, base.get(java.util.Calendar.MILLISECOND))
        }
        if (candidate.timeInMillis <= nowMillis) {
            candidate.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return candidate.timeInMillis
    }

    private fun nextWeeklyOccurrence(scheduledAtMillis: Long, repeatDayOfWeek: Int?, nowMillis: Long): Long {
        val base = java.util.Calendar.getInstance().apply { timeInMillis = scheduledAtMillis }
        val targetDay = repeatDayOfWeek ?: base.get(java.util.Calendar.DAY_OF_WEEK)
        val candidate = java.util.Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(java.util.Calendar.HOUR_OF_DAY, base.get(java.util.Calendar.HOUR_OF_DAY))
            set(java.util.Calendar.MINUTE, base.get(java.util.Calendar.MINUTE))
            set(java.util.Calendar.SECOND, base.get(java.util.Calendar.SECOND))
            set(java.util.Calendar.MILLISECOND, base.get(java.util.Calendar.MILLISECOND))
        }

        var dayOffset = (targetDay - candidate.get(java.util.Calendar.DAY_OF_WEEK) + 7) % 7
        if (dayOffset == 0 && candidate.timeInMillis <= nowMillis) {
            dayOffset = 7
        }
        candidate.add(java.util.Calendar.DAY_OF_YEAR, dayOffset)
        return candidate.timeInMillis
    }

    private fun nextMonthlyOccurrence(scheduledAtMillis: Long, nowMillis: Long): Long {
        val base = java.util.Calendar.getInstance().apply { timeInMillis = scheduledAtMillis }
        val candidate = java.util.Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(java.util.Calendar.DAY_OF_MONTH, base.get(java.util.Calendar.DAY_OF_MONTH).coerceAtMost(getActualMaximum(java.util.Calendar.DAY_OF_MONTH)))
            set(java.util.Calendar.HOUR_OF_DAY, base.get(java.util.Calendar.HOUR_OF_DAY))
            set(java.util.Calendar.MINUTE, base.get(java.util.Calendar.MINUTE))
            set(java.util.Calendar.SECOND, base.get(java.util.Calendar.SECOND))
            set(java.util.Calendar.MILLISECOND, base.get(java.util.Calendar.MILLISECOND))
        }
        if (candidate.timeInMillis <= nowMillis) {
            candidate.add(java.util.Calendar.MONTH, 1)
            val baseDay = base.get(java.util.Calendar.DAY_OF_MONTH)
            val maxDay = candidate.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            candidate.set(java.util.Calendar.DAY_OF_MONTH, baseDay.coerceAtMost(maxDay))
        }
        return candidate.timeInMillis
    }

    private const val TWO_DAYS_MILLIS = 2 * 24 * 60 * 60 * 1000L
}

