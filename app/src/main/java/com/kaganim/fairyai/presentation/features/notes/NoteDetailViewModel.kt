package com.kaganim.fairyai.presentation.features.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
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
    val isSaved: Boolean = false
)

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val noteUseCases: NoteUseCases,
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
}
