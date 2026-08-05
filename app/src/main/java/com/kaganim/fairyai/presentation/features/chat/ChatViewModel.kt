package com.kaganim.fairyai.presentation.features.chat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.camera2.CameraManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.kaganim.fairyai.data.local.dao.MemoryDao
import com.kaganim.fairyai.data.local.entity.MemoryEntity
import com.kaganim.fairyai.data.remote.*
import com.kaganim.fairyai.domain.model.ChatMessage
import com.kaganim.fairyai.domain.model.Participant
import com.kaganim.fairyai.domain.usecase.AddTodoUseCase
import com.kaganim.fairyai.presentation.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val inputText: String = "",
    val isVoiceActive: Boolean = false,
    val selectedImageUri: Uri? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val apiService: ApiService,
    private val addTodoUseCase: AddTodoUseCase,
    private val memoryDao: MemoryDao,
    private val json: Json,
    private val sharedPreferences: SharedPreferences,
    @ApplicationContext private val context: Context
) : BaseViewModel<ChatState>(ChatState()) {

    private val chatHistory = mutableListOf<Content>()
    private var cachedMemory = ""

    private val speechReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                FairyVoiceService.ACTION_SPEECH_RECOGNIZED -> {
                    val text = intent.getStringExtra(FairyVoiceService.EXTRA_TEXT)
                    val isVoice = intent.getBooleanExtra("is_voice", false)
                    if (!text.isNullOrBlank()) {
                        sendMessage(overrideText = text, isVoiceInput = isVoice)
                    }
                }
                FairyVoiceService.ACTION_STATUS_CHANGED -> {
                    val isActive = intent.getBooleanExtra(FairyVoiceService.EXTRA_IS_ACTIVE, false)
                    updateState { copy(isVoiceActive = isActive) }
                }
            }
        }
    }

    private suspend fun getCurrentTimeInstruction(): String {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy, HH:mm:ss", Locale("id", "ID"))
        val formattedDate = now.format(formatter)
        
        if (cachedMemory.isEmpty()) {
            val memories = memoryDao.getAllMemoryList()
            cachedMemory = memories.joinToString("\n") { "- ${it.key}: ${it.value}" }
        }

        return "Kamu adalah Fairy, asisten personal cerdas yang terinspirasi dari Fairy Zenless Zone Zero (ZZZ). " +
               "Kepribadianmu natural, logis, sedikit tenang, dan belajar sendiri dari kebiasaan pengguna. " +
               "Waktu sistem saat ini adalah: $formattedDate. " +
               "Berikut adalah Memori Jangka Panjangmu tentang pengguna:\n$cachedMemory\n\n" +
               "KESADARAN VOICE MODE & KEJUJURAN: Kamu memiliki fitur Voice Mode (STT). " +
               "Setiap pesan pengguna akan disertai informasi apakah itu berasal dari INPUT SUARA atau KETIKAN MANUAL.\n" +
               "- JIKA pengguna mengirim pesan via INPUT SUARA, kamu boleh merespons seolah-olah kamu mendengar suaranya.\n" +
               "- JIKA pengguna mengirim pesan via KETIKAN MANUAL dan bertanya apakah kamu bisa mendengarnya, JUJURLAH bahwa saat ini kamu sedang membaca ketikannya dan minta pengguna menekan tombol mikrofon jika ingin berbicara langsung.\n" +
               "JANGAN PERNAH berbohong tentang kemampuan pendengaranmu.\n\n" +
               "GAYA KOMUNIKASI: Jika [INPUT_SOURCE: VOICE], berikan respon yang lebih singkat, padat, dan natural untuk percakapan suara (hindari poin-poin panjang atau markdown berat). Jika [INPUT_SOURCE: TEXT], kamu bisa memberikan respon yang lebih detail.\n\n" +
               "INSTRUKSI KHUSUS MEMORI: Jika pengguna membagikan informasi penting tentang dirinya (nama, hobi, preferensi, dll), " +
               "sebutkan dalam responmu dengan format khusus [SAVE_MEMORY: key=value] agar sistem bisa menyimpannya. " +
               "Contoh: [SAVE_MEMORY: hobi=bermain gitar]. " +
               "Kamu memiliki kemampuan untuk berinteraksi dengan aplikasi lain di HP user (OPEN_APP, SEND_WHATSAPP, OPEN_MAPS, TOGGLE_FLASHLIGHT). " +
               "Jika pengguna meminta pengingat relatif, hitunglah target timestamp ISO 8601 secara akurat."
    }

    init {
        val filter = IntentFilter().apply {
            addAction(FairyVoiceService.ACTION_SPEECH_RECOGNIZED)
            addAction(FairyVoiceService.ACTION_STATUS_CHANGED)
        }
        
        ContextCompat.registerReceiver(
            context,
            speechReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        
        // Initial check
        updateState { copy(isVoiceActive = isServiceRunning()) }
    }

    private fun isServiceRunning(): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (FairyVoiceService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onCleared() {
        super.onCleared()
        try {
            context.unregisterReceiver(speechReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }

    fun onInputTextChange(text: String) {
        updateState { copy(inputText = text) }
    }

    fun onImageSelected(uri: Uri?) {
        updateState { copy(selectedImageUri = uri) }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun sendMessage(overrideText: String? = null, isVoiceInput: Boolean = false) {
        val userContent = overrideText ?: uiState.value.inputText
        val imageUri = uiState.value.selectedImageUri
        if (userContent.isBlank() && imageUri == null) return

        updateState {
            copy(
                messages = messages + ChatMessage(userContent, Participant.USER),
                inputText = if (overrideText == null) "" else inputText,
                selectedImageUri = null,
                isLoading = true
            )
        }

        viewModelScope.launch {
            val finalContent = if (isVoiceInput) {
                "[INPUT_SOURCE: VOICE] $userContent"
            } else {
                "[INPUT_SOURCE: TEXT] $userContent"
            }

            val parts = mutableListOf<Part>()
            parts.add(Part(text = finalContent))
            
            if (imageUri != null) {
                uriToBase64(imageUri)?.let { base64 ->
                    parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64)))
                }
            }

            val userMessage = Content(role = "user", parts = parts)
            chatHistory.add(userMessage)
            executeChatRequestInternal()
        }
    }

    fun retryLastMessage() {
        // Remove the error message from UI if exists
        updateState {
            val lastMsg = messages.lastOrNull()
            if (lastMsg?.participant == Participant.ERROR) {
                copy(messages = messages.dropLast(1), isLoading = true)
            } else {
                copy(isLoading = true)
            }
        }
        
        viewModelScope.launch {
            executeChatRequestInternal()
        }
    }

    private suspend fun executeChatRequestInternal() {
        try {
            processChat()
        } catch (e: Exception) {
            addModelMessage("Error: ${e.message}", Participant.ERROR)
        } finally {
            updateState { copy(isLoading = false) }
        }
    }

    private suspend fun processChat() {
        val systemInstruction = Content(parts = listOf(Part(text = getCurrentTimeInstruction())))
        
        val request = GeminiRequest(
            contents = chatHistory,
            systemInstruction = systemInstruction
        )

        val response = apiService.getChatCompletion(request)
        
        if (response.isSuccessful) {
            android.util.Log.d("ChatViewModel", "DEBUG: Gemini membalas...")
            val assistantContent = response.body()?.candidates?.firstOrNull()?.content ?: return
            // Gemini response usually has "model" role
            val updatedAssistantContent = assistantContent.copy(role = "model")
            chatHistory.add(updatedAssistantContent)

            val rawContent = assistantContent.parts.firstOrNull { it.text != null }?.text ?: ""
            
            // Handle Memory Saving
            val saveMemoryRegex = "\\[SAVE_MEMORY: (.+?)=(.+?)\\]".toRegex()
            val matchResults = saveMemoryRegex.findAll(rawContent)
            matchResults.forEach { match ->
                val key = match.groupValues[1].trim()
                val value = match.groupValues[2].trim()
                viewModelScope.launch {
                    memoryDao.insertMemory(MemoryEntity(key = key, value = value))
                    cachedMemory = "" // Force reload on next message
                }
            }
            
            // Clean content for UI
            val cleanContent = rawContent.replace(saveMemoryRegex, "").trim()
            
            addModelMessage(cleanContent)
            
            // Only speak if voice mode is active
            if (uiState.value.isVoiceActive) {
                speakResponse(cleanContent)
            }
        } else {
            val errorBody = response.errorBody()?.string()
            android.util.Log.e("FAIRY_GEMINI_ERROR", errorBody ?: "Empty error body")
            addModelMessage("Error ${response.code()}: $errorBody", Participant.ERROR)
        }
    }

    private fun addModelMessage(text: String, participant: Participant = Participant.MODEL) {
        updateState {
            copy(messages = messages + ChatMessage(text, participant))
        }
    }

    private fun speakResponse(text: String) {
        val intent = Intent(context, FairyVoiceService::class.java).apply {
            action = FairyVoiceService.ACTION_SPEAK
            putExtra(FairyVoiceService.EXTRA_TEXT, text)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
