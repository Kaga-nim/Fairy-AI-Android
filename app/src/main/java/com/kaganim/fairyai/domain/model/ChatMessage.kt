package com.kaganim.fairyai.domain.model

enum class Participant {
    USER, MODEL, ERROR
}

data class ChatMessage(
    val text: String,
    val participant: Participant,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null
)
