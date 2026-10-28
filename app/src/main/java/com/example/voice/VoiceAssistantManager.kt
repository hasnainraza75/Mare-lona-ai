package com.example.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

enum class AssistantVoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

data class VoiceProfile(
    val id: String,
    val name: String,
    val gender: String, // Female, Male
    val locale: Locale,
    val description: String,
    val pitch: Float = 1.0f,
    val speed: Float = 1.0f
)

class VoiceAssistantManager(
    private val context: Context,
    private val scope: CoroutineScope
) : TextToSpeech.OnInitListener {

    private val _voiceState = MutableStateFlow(AssistantVoiceState.IDLE)
    val voiceState: StateFlow<AssistantVoiceState> = _voiceState.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    // Supported neural voice presets
    val supportedVoices = listOf(
        VoiceProfile("lona_ko_female", "Korean Female (Lona)", "Female", Locale.KOREAN, "LONA neural female voice, polite & sci-fi", pitch = 1.15f, speed = 1.05f),
        VoiceProfile("nova_en_female", "Nova Female", "Female", Locale.US, "Vibrant energetic neural voice", pitch = 1.05f, speed = 1.0f),
        VoiceProfile("echo_en_male", "Echo Male", "Male", Locale.US, "Deep resonance futuristic male voice", pitch = 0.85f, speed = 0.95f),
        VoiceProfile("lona_ur_female", "Urdu Assistant", "Female", Locale("ur", "PK"), "Urdu voice model for Pakistani & South Asian regions", pitch = 1.0f, speed = 1.0f),
        VoiceProfile("lona_hi_female", "Hindi Assistant", "Female", Locale("hi", "IN"), "Hindi voice model", pitch = 1.0f, speed = 1.0f),
        VoiceProfile("cyber_uk_male", "Cyber British", "Male", Locale.UK, "Sophisticated British AI accent", pitch = 0.95f, speed = 1.0f)
    )

    private val _selectedVoice = MutableStateFlow(supportedVoices[0])
    val selectedVoice: StateFlow<VoiceProfile> = _selectedVoice.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(1.0f)
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("Auto")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    init {
        try {
            textToSpeech = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e("VoiceManager", "Failed to create TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            applyVoiceSettings()
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _voiceState.value = AssistantVoiceState.SPEAKING
                    _statusMessage.value = "Speaking..."
                    startSpeakingWaveformSimulation()
                }

                override fun onDone(utteranceId: String?) {
                    _voiceState.value = AssistantVoiceState.IDLE
                    _audioAmplitude.value = 0f
                    _statusMessage.value = "Listening paused"
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _voiceState.value = AssistantVoiceState.ERROR
                    _audioAmplitude.value = 0f
                    _statusMessage.value = "Speech synthesis error"
                }
            })
        } else {
            isTtsInitialized = false
            Log.e("VoiceManager", "TTS initialization failed: status $status")
        }
    }

    fun selectVoice(profile: VoiceProfile) {
        _selectedVoice.value = profile
        applyVoiceSettings()
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        applyVoiceSettings()
    }

    fun setSpeechPitch(pitch: Float) {
        _speechPitch.value = pitch
        applyVoiceSettings()
    }

    fun setSelectedLanguage(lang: String) {
        _selectedLanguage.value = lang
    }

    private fun applyVoiceSettings() {
        if (!isTtsInitialized || textToSpeech == null) return
        val voice = _selectedVoice.value
        try {
            val result = textToSpeech?.setLanguage(voice.locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.setLanguage(Locale.US)
            }
            textToSpeech?.setPitch(voice.pitch * _speechPitch.value)
            textToSpeech?.setSpeechRate(voice.speed * _speechRate.value)
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error applying voice settings", e)
        }
    }

    fun speak(text: String, onFinished: (() -> Unit)? = null) {
        if (!isTtsInitialized || textToSpeech == null) {
            _statusMessage.value = "TTS Engine not ready"
            return
        }
        applyVoiceSettings()
        _voiceState.value = AssistantVoiceState.SPEAKING
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "lona_${System.currentTimeMillis()}")
        }
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "lona_speech")
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _voiceState.value = AssistantVoiceState.IDLE
        _audioAmplitude.value = 0f
    }

    fun previewVoice(profile: VoiceProfile) {
        selectVoice(profile)
        val previewPhrase = when (profile.locale.language) {
            "ko" -> "안녕하세요. 저는 로나 AI입니다. 무엇을 도와드릴까요?"
            "ur" -> "السلام علیکم! میں لونا اے آئی ہوں، آپ کا ذاتی اسسٹنٹ۔"
            "hi" -> "नमस्ते! मैं लोना एआई हूँ, आपकी सहायक।"
            else -> "Hello! I am LONA AI, your advanced neural assistant."
        }
        speak(previewPhrase)
    }

    /**
     * Starts listening with SpeechRecognizer and microphone audio capture.
     */
    fun startListening(onQueryRecognized: (String) -> Unit) {
        stopSpeaking()
        stopAudioRecord()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceState.value = AssistantVoiceState.ERROR
            _statusMessage.value = "Speech recognition service unavailable on device"
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

            val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                val lang = when (_selectedLanguage.value) {
                    "Urdu" -> "ur-PK"
                    "Roman Urdu" -> "ur"
                    "Hindi" -> "hi-IN"
                    "Korean" -> "ko-KR"
                    "English" -> "en-US"
                    else -> Locale.getDefault().toLanguageTag()
                }
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _voiceState.value = AssistantVoiceState.LISTENING
                    _statusMessage.value = "Listening to your voice..."
                }

                override fun onBeginningOfSpeech() {
                    _statusMessage.value = "Detecting speech..."
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Normalize -2dB to +10dB into 0f..1f for visualization
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    _audioAmplitude.value = normalized
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _voiceState.value = AssistantVoiceState.PROCESSING
                    _statusMessage.value = "Processing query..."
                    _audioAmplitude.value = 0.3f
                }

                override fun onError(error: Int) {
                    _audioAmplitude.value = 0f
                    val errorDesc = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Tap to try again."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout. Tap to speak."
                        SpeechRecognizer.ERROR_NETWORK -> "Network issue connecting speech recognizer."
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                        else -> "Voice recognition notice (code $error)"
                    }
                    _statusMessage.value = errorDesc
                    _voiceState.value = AssistantVoiceState.IDLE
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull().orEmpty()
                    _audioAmplitude.value = 0f
                    if (text.isNotBlank()) {
                        _recognizedText.value = text
                        _voiceState.value = AssistantVoiceState.PROCESSING
                        _statusMessage.value = "Analyzing: \"$text\""
                        onQueryRecognized(text)
                    } else {
                        _voiceState.value = AssistantVoiceState.IDLE
                        _statusMessage.value = "Ready"
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull().orEmpty()
                    if (partial.isNotBlank()) {
                        _recognizedText.value = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer?.startListening(intent)
            _voiceState.value = AssistantVoiceState.LISTENING
            _statusMessage.value = "Initializing microphone..."
            startMicAmplitudeSampling()
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error starting listening", e)
            _voiceState.value = AssistantVoiceState.ERROR
            _statusMessage.value = "Could not access microphone: ${e.message}"
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error stopping speech recognizer", e)
        }
        stopAudioRecord()
        _voiceState.value = AssistantVoiceState.IDLE
        _audioAmplitude.value = 0f
        _statusMessage.value = "Ready"
    }

    private fun startMicAmplitudeSampling() {
        recordingJob?.cancel()
        recordingJob = scope.launch(Dispatchers.Default) {
            val bufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (bufferSize <= 0) return@launch
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    16000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
                if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord?.startRecording()
                    val buffer = ShortArray(bufferSize / 2)
                    while (isActive && _voiceState.value == AssistantVoiceState.LISTENING) {
                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (read > 0) {
                            var maxAmp = 0
                            for (i in 0 until read) {
                                val absVal = Math.abs(buffer[i].toInt())
                                if (absVal > maxAmp) maxAmp = absVal
                            }
                            val norm = (maxAmp / 16000f).coerceIn(0f, 1f)
                            _audioAmplitude.value = norm
                        }
                        delay(60)
                    }
                }
            } catch (e: SecurityException) {
                Log.w("VoiceManager", "Mic permission missing for direct AudioRecord: ${e.message}")
            } catch (e: Exception) {
                Log.w("VoiceManager", "AudioRecord notice: ${e.message}")
            }
        }
    }

    private fun stopAudioRecord() {
        recordingJob?.cancel()
        recordingJob = null
        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            // Ignore clean up errors
        }
    }

    private fun startSpeakingWaveformSimulation() {
        scope.launch {
            while (_voiceState.value == AssistantVoiceState.SPEAKING) {
                // Organic harmonic oscillation while speaking
                val simulated = 0.2f + (Math.sin(System.currentTimeMillis() / 90.0).toFloat() * 0.4f).coerceAtLeast(0f)
                _audioAmplitude.value = simulated
                delay(80)
            }
        }
    }

    fun release() {
        stopListening()
        stopSpeaking()
        speechRecognizer?.destroy()
        speechRecognizer = null
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
