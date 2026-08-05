package com.kaganim.fairyai.domain.usecase

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.kaganim.fairyai.domain.model.Todo
import com.kaganim.fairyai.domain.repository.TodoRepository
import com.kaganim.fairyai.presentation.features.chat.ReminderReceiver
import javax.inject.Inject

class AddTodoUseCase @Inject constructor(
    private val repository: TodoRepository,
    private val application: Application
) {
    suspend operator fun invoke(title: String, timestamp: Long) {
        repository.insertTodo(Todo(title = title, timestamp = timestamp))
        scheduleReminder(title, timestamp)
    }

    private fun scheduleReminder(title: String, timestamp: Long) {
        val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(application, ReminderReceiver::class.java).apply {
            putExtra("EXTRA_TITLE", title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            application,
            timestamp.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use setExactAndAllowWhileIdle for precision even in Doze mode
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timestamp,
            pendingIntent
        )
    }
}
