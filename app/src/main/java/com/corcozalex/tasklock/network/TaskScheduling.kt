package com.corcozalex.tasklock.network

import java.net.URLDecoder
import java.net.URLEncoder

private const val TASK_METADATA_PREFIX = "__TASKLOCK_META__"

enum class RepeatMode {
    NONE,
    DAILY,
    WEEKLY
}

data class TaskScheduleMetadata(
    val scheduledAtMillis: Long,
    val repeatMode: RepeatMode,
    val repeatDayOfWeek: Int?,
    val requiredObject: String,
    val notes: String?
)

val COMMON_REQUIRED_OBJECTS = listOf(
    "Toothbrush",
    "Backpack",
    "Laptop",
    "Phone",
    "Wallet",
    "Keys",
    "Water Bottle",
    "Book",
    "Shoes",
    "Mug"
)

object TaskMetadataCodec {
    fun encode(
        notes: String?,
        scheduledAtMillis: Long,
        repeatMode: RepeatMode,
        repeatDayOfWeek: Int?,
        requiredObject: String
    ): String {
        val encodedObject = URLEncoder.encode(requiredObject, Charsets.UTF_8.name())
        val day = repeatDayOfWeek ?: 0
        val header = "$TASK_METADATA_PREFIX|$scheduledAtMillis|${repeatMode.name}|$day|$encodedObject"
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

        val parts = firstLine.split("|")
        if (parts.size < 5) return null

        val scheduledAtMillis = parts[1].toLongOrNull() ?: return null
        val repeatMode = runCatching { RepeatMode.valueOf(parts[2]) }.getOrNull() ?: RepeatMode.NONE
        val repeatDay = parts[3].toIntOrNull()?.takeIf { it in 1..7 }
        val requiredObject = runCatching {
            URLDecoder.decode(parts[4], Charsets.UTF_8.name())
        }.getOrDefault("Object")

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
            notes = notes
        )
    }

    fun plainDescription(description: String?): String? {
        return decode(description)?.notes ?: description?.trim()?.ifBlank { null }
    }
}

