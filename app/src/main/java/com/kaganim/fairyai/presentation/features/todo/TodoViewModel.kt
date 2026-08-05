package com.kaganim.fairyai.presentation.features.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaganim.fairyai.domain.model.Todo
import com.kaganim.fairyai.domain.repository.TodoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TodoState(
    val todos: List<Todo> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val repository: TodoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodoState())
    val uiState: StateFlow<TodoState> = _uiState.asStateFlow()

    init {
        loadTodos()
    }

    private fun loadTodos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getTodos().collectLatest { todos ->
                _uiState.value = TodoState(todos = todos, isLoading = false)
            }
        }
    }

    fun toggleTodo(todo: Todo) {
        viewModelScope.launch {
            repository.insertTodo(todo.copy(isCompleted = !todo.isCompleted))
        }
    }

    fun deleteTodo(todo: Todo) {
        viewModelScope.launch {
            repository.deleteTodo(todo)
        }
    }
}
