package com.corcozalex.tasklock.network

data class Task(
    val id: Int,
    val title: String,
    val description: String?,
    val is_completed: Boolean
)

data class TaskCreateRequest(
    val title: String,
    val description: String?
)