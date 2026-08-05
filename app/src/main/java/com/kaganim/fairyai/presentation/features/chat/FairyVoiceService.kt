package com.kaganim.fairyai.presentation.features.chat

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.util.*

class FairyVoiceService : Service(), TextToSpeech.OnInitListener {

    companion object {
        const val ACTION_SPEECH_RECOGNIZED = "com.kaganim.fairyai.SPEECH_RECOGNIZED"
        const val ACTION_SPEAK = "com.kaganim.fairyai.SPEAK"
        const val ACTION_STATUS_CHANGED = "com.kaganim.fairyai.STATUS_CHANGED"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_IS_ACTIVE = "extra_is_active"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val CHANNEL_ID = "fairy_voice_service"
    private val NOTIFICATION_ID = 1
    private val TAG = "FairyVoiceService"
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false
    
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    
    private var timeoutRunnable: Runnable? = null

    private fun showDebugToast(message: String) {
        mainHandler.post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
        Log.d(TAG, "DEBUG: $message")
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        showDebugToast("⚙️ Inisialisasi Fairy Voice...")
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Fairy sedang bersiap..."))
        
        tts = TextToSpeech(this, this)
        setupSpeechRecognizer()
        sendStatusBroadcast(true)
    }

    private fun setupTtsListener() {
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                showDebugToast("🔊 Fairy berbicara...")
                cancelTimeoutTimer()
                mainHandler.post {
                    speechRecognizer?.stopListening()
                    isListening = false
                }
            }

            override fun onDone(utteranceId: String?) {
                showDebugToast("✅ Fairy selesai bicara, mendengarkan kembali...")
                mainHandler.post {
                    startListening()
                }
            }

            override fun onError(utteranceId: String?) {
                showDebugToast("❌ Error TTS")
                mainHandler.post {
                    startListening()
                }
            }
        })
    }

    private fun updateNotification(status: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(status))
    }

    private fun setupSpeechRecognizer() {
        mainHandler.post {
            try {
                // Double check permission inside service
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) 
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    updateNotification("Fairy Error: Izin Mikrofon tidak ada")
                    return@post
                }

                if (speechRecognizer != null) {
                    speechRecognizer?.destroy()
                    speechRecognizer = null
                }
                
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                speechRecognizer?.setRecognitionListener(createRecognitionListener())
                
                updateNotification("Fairy sedang memulai pendengaran...")
                startListening()
            } catch (e: Exception) {
                Log.e(TAG, "Error in setupSpeechRecognizer", e)
                updateNotification("Fairy Error: Gagal inisialisasi")
                scheduleRestart(2000)
            }
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "onReadyForSpeech")
            isListening = true
            showDebugToast("🎙️ Fairy mendengarkan...")
            updateNotification("Fairy sedang mendengarkan...")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech")
            cancelTimeoutTimer()
            updateNotification("Fairy mendeteksi suara...")
        }

        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        
        override fun onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech")
            isListening = false
            cancelTimeoutTimer()
        }

        override fun onError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer Busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                else -> "Unknown error"
            }
            Log.e(TAG, "onError: $message")
            
            isListening = false
            cancelTimeoutTimer()
            
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH, 
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    showDebugToast("🔇 Hening, Fairy berhenti mendengarkan.")
                    stopSelf()
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY, 
                SpeechRecognizer.ERROR_CLIENT,
                SpeechRecognizer.ERROR_AUDIO -> {
                    showDebugToast("❌ Error Mic: $message")
                    Log.d(TAG, "Critical error, recreating recognizer...")
                    setupSpeechRecognizer() // Recreate
                }
                else -> {
                    showDebugToast("❌ Error Mic: $message")
                    updateNotification("Fairy Error: $message")
                    scheduleRestart(2000)
                }
            }
        }

        override fun onResults(results: Bundle?) {
            cancelTimeoutTimer()
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            Log.d(TAG, "onResults: $matches")
            val text = matches?.firstOrNull()
            if (!text.isNullOrBlank()) {
                showDebugToast("📝 Suara ditangkap: $text")
                // Send broadcast to ChatViewModel
                val intent = Intent(ACTION_SPEECH_RECOGNIZED).apply {
                    putExtra(EXTRA_TEXT, text)
                    putExtra("is_voice", true)
                    setPackage(packageName)
                }
                sendBroadcast(intent)
            }
            updateNotification("Fairy sedang menunggu jawaban...")
        }

        override fun onPartialResults(partialResults: Bundle?) {}

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun isWakeWordDetected(text: String): Boolean {
        val lowerText = text.lowercase()
        val wakeWords = listOf("fairy", "fery", "feri", "feiri", "peri", "pery", "tes", "test")
        return wakeWords.any { lowerText.contains(it) }
    }

    private fun startListening() {
        showDebugToast("🎙️ Mic Aktif (Menunggu suara...)")
        if (!requestAudioFocus()) {
            Log.w(TAG, "Could not acquire audio focus")
        }

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        try {
            speechRecognizer?.startListening(recognizerIntent)
            startTimeoutTimer()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting listening", e)
            setupSpeechRecognizer()
        }
    }

    private fun startTimeoutTimer() {
        cancelTimeoutTimer()
        timeoutRunnable = Runnable {
            if (isListening) {
                showDebugToast("🔇 Hening, mikrofon dimatikan otomatis.")
                speechRecognizer?.stopListening()
                isListening = false
                stopSelf()
            }
        }
        mainHandler.postDelayed(timeoutRunnable!!, 5000L)
    }

    private fun cancelTimeoutTimer() {
        timeoutRunnable?.let {
            mainHandler.removeCallbacks(it)
            timeoutRunnable = null
        }
    }

    private fun scheduleRestart(delay: Long) {
        mainHandler.removeCallbacksAndMessages("restart_token")
        mainHandler.postAtTime({
            if (!isListening) startListening()
        }, "restart_token", SystemClock.uptimeMillis() + delay)
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            Log.d(TAG, "Audio focus lost, stopping listening")
                            speechRecognizer?.stopListening()
                        }
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            Log.d(TAG, "Audio focus gained, restarting listening")
                            scheduleRestart(500)
                        }
                    }
                }
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun speak(text: String) {
        if (tts == null || tts?.run { language == null } == true) {
            Log.e(TAG, "TTS not ready, delaying speak")
            mainHandler.postDelayed({ speak(text) }, 500)
            return
        }

        Log.d(TAG, "Speaking: $text")
        updateNotification("Fairy berbicara...")
        
        // Stop listening while speaking to avoid hearing itself
        speechRecognizer?.stopListening()
        isListening = false
        
        // Try to find a soft female voice
        try {
            val voices = tts?.voices
            val femaleVoice = voices?.find { 
                it.name.lowercase().contains("female") || 
                it.name.lowercase().contains("soft") ||
                (it.locale.language == "id" && it.name.contains("network")) // Google ID network voices are often better
            }
            if (femaleVoice != null) {
                tts?.voice = femaleVoice
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting voice", e)
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "fairy_tts")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setLanguage(Locale("id", "ID"))
            setupTtsListener()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Fairy Voice Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Status monitoring for Fairy AI Voice Service"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, com.kaganim.fairyai.presentation.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fairy Voice Listener")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun sendStatusBroadcast(isActive: Boolean) {
        val intent = Intent(ACTION_STATUS_CHANGED).apply {
            putExtra(EXTRA_IS_ACTIVE, isActive)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SPEAK) {
            val text = intent.getStringExtra(EXTRA_TEXT)
            if (!text.isNullOrBlank()) {
                speak(text)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        sendStatusBroadcast(false)
        mainHandler.removeCallbacksAndMessages(null)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
        }

        speechRecognizer?.let {
            it.stopListening()
            it.cancel()
            it.destroy()
        }
        tts?.let {
            it.stop()
            it.shutdown()
        }
        super.onDestroy()
    }
}
