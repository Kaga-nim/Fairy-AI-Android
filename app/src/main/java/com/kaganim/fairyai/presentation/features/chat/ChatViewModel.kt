package com.kaganim.fairyai.presentation.features.chat

import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.*
import com.kaganim.fairyai.domain.model.ChatMessage
import com.kaganim.fairyai.domain.model.Participant
import com.kaganim.fairyai.domain.usecase.AddTodoUseCase
import com.kaganim.fairyai.presentation.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val inputText: String = ""
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val addTodoUseCase: AddTodoUseCase
) : BaseViewModel<ChatState>(ChatState()) {

    // 1. Define Function for Gemini
    private val createReminderDeclaration = defineFunction(
        name = "createReminder",
        description = "Create a reminder or todo for the user with a title and specific time.",
        parameters = listOf(
            Schema.str("title", "The short summary of the reminder"),
            Schema.num("timestamp", "The unix timestamp in milliseconds when the reminder should occur")
        )
    )

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = "YOUR_API_KEY", // Should be moved to BuildConfig or Secrets
        tools = listOf(Tool(listOf(createReminderDeclaration)))
    )

    private val chat = generativeModel.startChat()

    fun onInputTextChange(text: String) {
        updateState { copy(inputText = text) }
    }

    fun sendMessage() {
        val userMessage = uiState.value.inputText
        if (userMessage.isBlank()) return

        updateState {
            copy(
                messages = messages + ChatMessage(userMessage, Participant.USER),
                inputText = "",
                isLoading = true
            )
        }

        viewModelScope.launch {
            try {
                val response = chat.sendMessage(userMessage)
                
                // 2. Catch programatic response (FunctionCallPart)
                val functionCalls = response.candidates.first().content.parts.filterIsInstance<FunctionCallPart>()
                
                if (functionCalls.isNotEmpty()) {
                    functionCalls.forEach { call ->
                        if (call.name == "createReminder") {
                            val title = call.args["title"] as? String ?: "No Title"
                            val timestamp = (call.args["timestamp"] as? Double)?.toLong() ?: System.currentTimeMillis()
                            
                            // 3. Route to Room Use Case
                            addTodoUseCase(title, timestamp)
                            
                            // Respond back to AI that function was executed
                            chat.sendMessage(
                                content {
                                    part(FunctionResponsePart("createReminder", mapOf("status" to "success")))
                                }
                            )
                        }
                    }
                    
                    // Get the final textual response after function execution
                    val finalResponse = chat.history.last().parts.filterIsInstance<TextPart>().lastOrNull()?.text
                    finalResponse?.let {
                        addModelMessage(it)
                    }
                } else {
                    response.text?.let { addModelMessage(it) }
                }
            } catch (e: Exception) {
                addModelMessage("Error: ${e.message}", Participant.ERROR)
            } finally {
                updateState { copy(isLoading = false) }
            }
        }
    }

    private fun addModelMessage(text: String, participant: Participant = Participant.MODEL) {
        updateState {
            copy(messages = messages + ChatMessage(text, participant))
        }
    }
}
