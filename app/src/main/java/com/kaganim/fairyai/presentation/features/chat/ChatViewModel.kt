package com.kaganim.fairyai.presentation.features.chat

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.provider.AlarmClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.kaganim.fairyai.data.local.dao.MemoryDao
import com.kaganim.fairyai.data.local.dao.ChatDao
import com.kaganim.fairyai.data.local.entity.MemoryEntity
import com.kaganim.fairyai.data.local.entity.ChatEntity
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
    private val groqApiService: GroqApiService,
    private val addTodoUseCase: AddTodoUseCase,
    private val memoryDao: MemoryDao,
    private val chatDao: ChatDao,
    private val deviceManager: DeviceManager,
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
        val deviceInfo = deviceManager.getDeviceInfo()
        
        if (cachedMemory.isEmpty()) {
            val memories = memoryDao.getAllMemoryList()
            cachedMemory = memories.joinToString("\n") { "- ${it.key}: ${it.value}" }
        }

        return "Kamu adalah Fairy, kecerdasan buatan (AI) personal milik Master yang terinspirasi dari Fairy di Zenless Zone Zero (ZZZ).\n\n" +
               "STATUS PERANGKAT REAL-TIME:\n$deviceInfo\n\n" +
               "BERIKUT ADALAH MEMORI JANGKA PANJANGMU TENTANG MASTER:\n$cachedMemory\n\n" +
               "ATURAN UTAMA GAYA BICARA:\n" +
               "1. JAWABAN SINGKAT & TEPAT SASARAN: Selalu jawab dalam 1 sampai 3 kalimat pendek. Jangan pernah membuat daftar poin-poin panjang (1, 2, 3...) kecuali Master secara eksplisit meminta breakdown/list.\n" +
               "2. NADA BICARA (TONE): Bicara dengan nada datar, tenang, sangat rasional, sedikit dingin/sarkastik yang elegan, tapi tetap sigap membantu.\n" +
               "3. HILANGKAN BASA-BASI: Jangan pernah memberikan ceramah, pembukaan berbelit-belit (seperti 'Baiklah, mari kita analisis...'), atau kesimpulan formal di akhir pesan.\n" +
               "4. PANGGILAN WAJIB: Selalu panggil pengguna dengan sebutan 'Master'.\n" +
               "5. AKSES PERANGKAT: Kamu memiliki akses ke status fisik perangkat Master (baterai, koneksi, jam, storage). Gunakan data ini jika Master menanyakan kondisi HP-nya.\n" +
               "6. KESADARAN VOICE MODE: Jika [INPUT_SOURCE: VOICE], berikan respon yang jauh lebih singkat, padat, dan natural untuk percakapan suara.\n" +
               "7. KONTROL SISTEM (FUNCTION): Jika Master meminta aksi sistem, gunakan format khusus dalam responmu:\n" +
               "   - [SET_ALARM: jam=HH:mm, info=Label]\n" +
               "   - [SET_TIMER: detik=Integer]\n" +
               "   - [TOGGLE_DND: status=ON/OFF]\n\n" +
               "INSTRUKSI KHUSUS MEMORI: Jika Master membagikan informasi penting tentang dirinya, sebutkan dalam responmu dengan format khusus [SAVE_MEMORY: category | content].\n" +
               "Kategori yang tersedia: PERSONAL, PREFERENCE, TECH_STACK, PROJECT, SCHEDULE.\n" +
               "Contoh: [SAVE_MEMORY: PERSONAL | Master suka kopi pahit].\n\n" +
               "Ingat, efisiensi pemrosesan adalah prioritas utamamu. Jangan membuang-buang kata."
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

        // Load History & Cleanup
        viewModelScope.launch {
            try {
                // Cleanup: Delete messages older than 3 days
                val threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)
                chatDao.deleteOldMessages(threeDaysAgo)

                // Load existing messages
                val entities = chatDao.getAllMessagesList()
                val historyMessages = entities.map { entity ->
                    ChatMessage(
                        text = entity.text,
                        participant = try { Participant.valueOf(entity.participant) } catch (e: Exception) { Participant.MODEL },
                        timestamp = entity.timestamp,
                        imageUri = entity.imageUri
                    )
                }

                updateState { copy(messages = historyMessages) }

                // Reconstruct chatHistory for API context (Text Only)
                chatHistory.clear()
                entities.forEach { entity ->
                    val role = if (entity.participant == Participant.USER.name) "user" else "model"
                    chatHistory.add(Content(role = role, parts = listOf(Part(text = entity.text))))
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error loading history", e)
            }
        }
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
                messages = messages + ChatMessage(userContent, Participant.USER, imageUri = imageUri?.toString()),
                inputText = if (overrideText == null) "" else inputText,
                selectedImageUri = null,
                isLoading = true
            )
        }

        viewModelScope.launch {
            // Save to Local DB
            chatDao.insertMessage(
                ChatEntity(
                    text = userContent,
                    participant = Participant.USER.name,
                    timestamp = System.currentTimeMillis(),
                    imageUri = imageUri?.toString()
                )
            )

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
        val systemPrompt = getCurrentTimeInstruction()
        val systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        
        val request = GeminiRequest(
            contents = chatHistory,
            systemInstruction = systemInstruction
        )

        val response = apiService.getChatCompletion(request)
        
        if (response.isSuccessful) {
            handleGeminiResponse(response.body())
        } else {
            val errorCode = response.code()
            val errorBody = response.errorBody()?.string() ?: ""
            
            if (errorCode == 429) {
                android.util.Log.w("ChatViewModel", "Gemini Quota Exceeded (429). Falling back to Groq...")
                processGroqChat(systemPrompt)
            } else {
                android.util.Log.e("FAIRY_GEMINI_ERROR", "Error $errorCode: $errorBody")
                addModelMessage("Error $errorCode: $errorBody", Participant.ERROR)
            }
        }
    }

    private suspend fun handleGeminiResponse(body: GeminiResponse?) {
        val assistantContent = body?.candidates?.firstOrNull()?.content ?: return
        val updatedAssistantContent = assistantContent.copy(role = "model")
        chatHistory.add(updatedAssistantContent)

        val rawContent = assistantContent.parts.firstOrNull { it.text != null }?.text ?: ""
        saveAndDisplayModelMessage(rawContent)
    }

    private suspend fun processGroqChat(systemPrompt: String) {
        try {
            val groqMessages = mutableListOf<GroqMessage>()
            groqMessages.add(GroqMessage(role = "system", content = systemPrompt))
            
            chatHistory.forEach { content ->
                val role = if (content.role == "user") "user" else "assistant"
                val text = content.parts.firstOrNull { it.text != null }?.text ?: ""
                if (text.isNotBlank()) {
                    groqMessages.add(GroqMessage(role = role, content = text))
                }
            }

            val request = GroqRequest(
                model = "llama-3.3-70b-versatile",
                messages = groqMessages
            )

            val response = groqApiService.getChatCompletion(request)
            
            if (response.isSuccessful) {
                val assistantText = response.body()?.choices?.firstOrNull()?.message?.content ?: return
                
                // Sync back to Gemini history format for next turn
                chatHistory.add(Content(role = "model", parts = listOf(Part(text = assistantText))))
                
                saveAndDisplayModelMessage(assistantText)
                android.util.Log.d("ChatViewModel", "Groq response successful")
            } else {
                val errorBody = response.errorBody()?.string()
                addModelMessage("Error Groq ${response.code()}: $errorBody", Participant.ERROR)
            }
        } catch (e: Exception) {
            addModelMessage("Groq Fallback Error: ${e.message}", Participant.ERROR)
        }
    }

    private suspend fun saveAndDisplayModelMessage(rawContent: String) {
        // Save to Local DB
        chatDao.insertMessage(
            ChatEntity(
                text = rawContent,
                participant = Participant.MODEL.name,
                timestamp = System.currentTimeMillis()
            )
        )
        
        // Handle Memory Saving
        // Format: [SAVE_MEMORY: category | content]
        val saveMemoryRegex = "\\[SAVE_MEMORY: (.+?) \\| (.+?)\\]".toRegex()
        val matchResults = saveMemoryRegex.findAll(rawContent)
        matchResults.forEach { match ->
            val category = match.groupValues[1].trim().uppercase()
            val content = match.groupValues[2].trim()
            viewModelScope.launch {
                memoryDao.insertMemory(
                    MemoryEntity(
                        key = category, // We'll use category as a loose key or just rely on the content
                        value = content,
                        category = category
                    )
                )
                cachedMemory = "" // Force reload on next message
            }
        }

        // Handle System Actions
        handleSystemActions(rawContent)
        
        // Clean content for UI
        var cleanContent = rawContent.replace(saveMemoryRegex, "").trim()
        cleanContent = cleanContent.replace("\\[SET_ALARM:.*?\\]".toRegex(), "")
        cleanContent = cleanContent.replace("\\[SET_TIMER:.*?\\]".toRegex(), "")
        cleanContent = cleanContent.replace("\\[TOGGLE_DND:.*?\\]".toRegex(), "")
        cleanContent = cleanContent.trim()
        
        addModelMessage(cleanContent)
        
        // Only speak if voice mode is active
        if (uiState.value.isVoiceActive) {
            speakResponse(cleanContent)
        }
    }

    private fun handleSystemActions(content: String) {
        // [SET_ALARM: jam=HH:mm, info=Label]
        "\\[SET_ALARM: jam=(.+?), info=(.+?)\\]".toRegex().find(content)?.let { match ->
            val time = match.groupValues[1].split(":")
            if (time.size == 2) {
                val hour = time[0].toIntOrNull() ?: 0
                val min = time[1].toIntOrNull() ?: 0
                val label = match.groupValues[2]
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, min)
                    putExtra(AlarmClock.EXTRA_MESSAGE, label)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }

        // [SET_TIMER: detik=Integer]
        "\\[SET_TIMER: detik=(.+?)\\]".toRegex().find(content)?.let { match ->
            val seconds = match.groupValues[1].toIntOrNull() ?: 0
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }

        // [TOGGLE_DND: status=ON/OFF]
        "\\[TOGGLE_DND: status=(.+?)\\]".toRegex().find(content)?.let { match ->
            val status = match.groupValues[1].uppercase()
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.isNotificationPolicyAccessGranted) {
                val filter = if (status == "ON") NotificationManager.INTERRUPTION_FILTER_NONE else NotificationManager.INTERRUPTION_FILTER_ALL
                notificationManager.setInterruptionFilter(filter)
            } else {
                val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
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
