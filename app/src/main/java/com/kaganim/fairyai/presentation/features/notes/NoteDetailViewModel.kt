package com.kaganim.fairyai.presentation.features.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kaganim.fairyai.data.remote.*
import com.kaganim.fairyai.domain.model.Note
import com.kaganim.fairyai.domain.usecase.NoteUseCases
import com.kaganim.fairyai.presentation.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteDetailState(
    val title: String = "",
    val content: String = "",
    val id: Int? = null,
    val isSaved: Boolean = false,
    val isSummarizing: Boolean = false,
    val summary: String? = null,
    val summaryError: String? = null
)

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val noteUseCases: NoteUseCases,
    private val apiService: ApiService,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<NoteDetailState>(NoteDetailState()) {

    init {
        savedStateHandle.get<Int>("noteId")?.let { noteId ->
            if (noteId != -1) {
                viewModelScope.launch {
                    noteUseCases.getNote(noteId)?.let { note ->
                        updateState {
                            copy(
                                id = note.id,
                                title = note.title,
                                content = note.content
                            )
                        }
                    }
                }
            }
        }
    }

    fun onTitleChange(title: String) {
        updateState { copy(title = title) }
    }

    fun onContentChange(content: String) {
        updateState { copy(content = content) }
    }

    fun saveNote() {
        viewModelScope.launch {
            noteUseCases.addNote(
                Note(
                    id = uiState.value.id,
                    title = uiState.value.title,
                    content = uiState.value.content,
                    timestamp = System.currentTimeMillis()
                )
            )
            updateState { copy(isSaved = true) }
        }
    }

    fun summarizeNoteWithAI() {
        val noteContent = uiState.value.content
        if (noteContent.isBlank()) return

        viewModelScope.launch {
            updateState { copy(isSummarizing = true, summary = null, summaryError = null) }
            try {
                val response = apiService.getChatCompletion(
                    GeminiRequest(
                        contents = listOf(
                            Content(
                                role = "user",
                                parts = listOf(Part(text = noteContent))
                            )
                        ),
                        systemInstruction = Content(
                            parts = listOf(
                                Part(text = "Kamu adalah Fairy, asisten pembaca cepat yang logis dan efisien. Tugasmu adalah merangkum teks catatan yang diberikan oleh pengguna menjadi poin-poin penting yang sangat singkat, jelas, dan mudah dipahami.")
                            )
                        )
                    )
                )

                if (response.isSuccessful) {
                    val summary = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    updateState { copy(isSummarizing = false, summary = summary) }
                } else {
                    updateState { copy(isSummarizing = false, summaryError = "Gagal merangkum catatan. Silakan coba lagi.") }
                }
            } catch (e: Exception) {
                updateState { copy(isSummarizing = false, summaryError = e.message ?: "Terjadi kesalahan") }
            }
        }
    }

    fun clearSummary() {
        updateState { copy(summary = null, summaryError = null) }
    }
}
