package com.kaganim.fairyai.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

interface GroqApiService {
    @POST("chat/completions")
    suspend fun getChatCompletion(@Body request: GroqRequest): retrofit2.Response<GroqResponse>
}

@Serializable
data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>
)

@Serializable
data class GroqMessage(
    val role: String,
    val content: String
)

@Serializable
data class GroqResponse(
    val choices: List<GroqChoice>
)

@Serializable
data class GroqChoice(
    val message: GroqMessage
)
