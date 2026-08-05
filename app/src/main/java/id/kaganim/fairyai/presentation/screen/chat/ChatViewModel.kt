package id.kaganim.fairyai.presentation.screen.chat

import dagger.hilt.android.lifecycle.HiltViewModel
import id.kaganim.fairyai.presentation.viewmodel.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor() : BaseViewModel<ChatUiState>(ChatUiState()) {
    
    fun sendMessage(message: String) {
        // Implementation later
    }
}
