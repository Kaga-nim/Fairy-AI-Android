package com.kaganim.fairyai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kaganim.fairyai.domain.model.Todo

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val title: String,
    val timestamp: Long,
    val isCompleted: Boolean
) {
    fun toTodo(): Todo = Todo(id, title, timestamp, isCompleted)

    companion object {
        fun fromTodo(todo: Todo): TodoEntity = TodoEntity(
            id = todo.id,
            title = todo.title,
            timestamp = todo.timestamp,
            isCompleted = todo.isCompleted
        )
    }
}
