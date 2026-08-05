package id.kaganim.fairyai.domain.repository

import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(): Flow<List<String>>
    suspend fun sendMessage(message: String)
}
