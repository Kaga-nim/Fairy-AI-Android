package id.kaganim.fairyai.presentation.screen.chat

data class ChatUiState(
    val messages: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
