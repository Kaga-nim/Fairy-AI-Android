package id.kaganim.fairyai.data.repository

import id.kaganim.fairyai.data.local.AppDatabase
import id.kaganim.fairyai.data.remote.ApiService
import id.kaganim.fairyai.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val database: AppDatabase
) : ChatRepository {
    override fun getMessages(): Flow<List<String>> = flow {
        emit(emptyList())
    }

    override suspend fun sendMessage(message: String) {
        // Implementation
    }
}
