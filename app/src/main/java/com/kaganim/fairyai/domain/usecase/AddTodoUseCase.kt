package com.kaganim.fairyai.domain.usecase

import com.kaganim.fairyai.domain.model.Todo
import com.kaganim.fairyai.domain.repository.TodoRepository
import javax.inject.Inject

class AddTodoUseCase @Inject constructor(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(title: String, timestamp: Long) {
        repository.insertTodo(Todo(title = title, timestamp = timestamp))
    }
}
