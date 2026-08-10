package com.kaganim.fairyai.presentation.features.profile

import androidx.lifecycle.viewModelScope
import com.kaganim.fairyai.data.local.dao.ChatDao
import com.kaganim.fairyai.data.local.dao.MemoryDao
import com.kaganim.fairyai.data.local.entity.ChatEntity
import com.kaganim.fairyai.data.local.entity.MemoryEntity
import com.kaganim.fairyai.presentation.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ProfileSection {
    HISTORY, MEMORY
}

data class ProfileState(
    val selectedSection: ProfileSection = ProfileSection.HISTORY,
    val isLoading: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val chatDao: ChatDao,
    private val memoryDao: MemoryDao
) : BaseViewModel<ProfileState>(ProfileState()) {

    val chatHistory: StateFlow<List<ChatEntity>> = chatDao.getAllMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userMemories: StateFlow<List<MemoryEntity>> = memoryDao.getAllMemory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSectionSelected(section: ProfileSection) {
        updateState { copy(selectedSection = section) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatDao.deleteOldMessages(Long.MAX_VALUE)
        }
    }

    fun deleteMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            memoryDao.deleteMemory(memory)
        }
    }

    fun addManualMemory(category: String, content: String) {
        viewModelScope.launch {
            memoryDao.insertMemory(
                MemoryEntity(
                    key = category,
                    value = content,
                    category = category.uppercase()
                )
            )
        }
    }
}
