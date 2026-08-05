package com.kaganim.fairyai.domain.model

data class Todo(
    val id: Int? = null,
    val title: String,
    val timestamp: Long,
    val isCompleted: Boolean = false
)
