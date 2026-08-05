package com.kaganim.fairyai.presentation.features.notes

import androidx.lifecycle.viewModelScope
import com.kaganim.fairyai.domain.model.Note
import com.kaganim.fairyai.domain.usecase.NoteUseCases
import com.kaganim.fairyai.presentation.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotesState(
    val notes: List<Note> = emptyList(),
    val searchQuery: String = ""
)

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteUseCases: NoteUseCases
) : BaseViewModel<NotesState>(NotesState()) {

    init {
        getNotes()
    }

    private fun getNotes() {
        noteUseCases.getNotes()
            .onEach { notes ->
                updateState { copy(notes = notes) }
            }
            .launchIn(viewModelScope)
    }

    fun onDeleteNote(note: Note) {
        viewModelScope.launch {
            noteUseCases.deleteNote(note)
        }
    }
    
    fun onSearchQueryChange(query: String) {
        updateState { copy(searchQuery = query) }
    }
}
