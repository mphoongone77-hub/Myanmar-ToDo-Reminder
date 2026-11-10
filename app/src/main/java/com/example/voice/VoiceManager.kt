package com.example.voice

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException
import java.util.Locale

sealed class SpeechState {
    object Idle : SpeechState()
    object Ready : SpeechState()
    data class Listening(val rmsDb: Float = 0f) : SpeechState()
    data class Success(val text: String, val parsed: ParsedVoiceTask) : SpeechState()
    data class Error(val message: String) : SpeechState()
}

class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    // Voice Recorder for recording Burmese voice memos
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordPath: String? = null
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    // Voice Player for playing recorded voice memos
    private var mediaPlayer: MediaPlayer? = null
    private val _currentlyPlayingPath = MutableStateFlow<String?>(null)
    val currentlyPlayingPath: StateFlow<String?> = _currentlyPlayingPath.asStateFlow()

    // Text to Speech
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    val isRecognitionSupported: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error initializing TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val myanmarLocale = Locale.forLanguageTag("my-MM")
            val result = tts?.setLanguage(myanmarLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default locale
                tts?.language = Locale.getDefault()
            }
            isTtsReady = true
        }
    }

    fun speakText(text: String) {
        if (!isTtsReady || text.isBlank()) return
        tts?.stop()
        _isSpeaking.value = true
        val params = Bundle()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "TODO_TTS")
        _isSpeaking.value = false
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    /**
     * Initializes and starts SpeechRecognizer specifically configured for
     * capturing Burmese voice input and commands (my-MM) on the Main Looper.
     */
    fun startListening() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { startListening() }
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _speechState.value = SpeechState.Error("စက်တွင် အသံဖမ်းစနစ် (Speech Recognition) မရှိပါ (အောက်ပါ ကီးဘုတ် မိုက်ကရိုဖုန်းကို အသုံးပြုနိုင်ပါသည်)")
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _speechState.value = SpeechState.Ready
                    }

                    override fun onBeginningOfSpeech() {
                        _speechState.value = SpeechState.Listening(0f)
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        if (_speechState.value is SpeechState.Listening) {
                            _speechState.value = SpeechState.Listening(rmsdB)
                        }
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _speechState.value = SpeechState.Ready
                    }

                    override fun onError(error: Int) {
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "အသံဖမ်း၍ မရပါ (မြန်မာလို ပြန်လည်ပြောဆိုပါ)"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "အချိန်ကုန်ဆုံးသွားပါပြီ"
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> 
                                "အင်တာနက် မရှိပါ (အောက်ပါ အသံမှတ်တမ်းတင်စနစ် (Voice Memo) ဖြင့် အော့ဖ်လိုင်း တိုက်ရိုက် မှတ်သားနိုင်ပါသည်)"
                            SpeechRecognizer.ERROR_AUDIO -> "အသံဖမ်းယူမှု အမှားအယွင်း ဖြစ်ပွားခဲ့သည်"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "မိုက်ကရိုဖုန်း ခွင့်ပြုချက် လိုအပ်ပါသည်"
                            else -> "အသံမှတ်သားခြင်း အဆင်မပြေပါ ($error)"
                        }
                        _speechState.value = SpeechState.Error(errorMsg)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val text = matches[0]
                            val parsed = BurmeseVoiceCommandParser.parseBurmeseCommand(text)
                            _speechState.value = SpeechState.Success(text, parsed)
                        } else {
                            _speechState.value = SpeechState.Error("အသံဖမ်းယူ၍ မရရှိပါ")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _speechState.value = SpeechState.Listening(5f)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "my-MM")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "my-MM")
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("my-MM", "en-US", "en-GB"))
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "မြန်မာလို အသံဖြင့် အလုပ်အသစ်ပြောဆိုပါ...")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            }

            speechRecognizer?.startListening(intent)
            _speechState.value = SpeechState.Ready
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error starting speech recognition", e)
            _speechState.value = SpeechState.Error("အသံဖမ်းစနစ် စတင်မရပါ: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { stopListening() }
            return
        }
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error stopping recognizer", e)
        }
        speechRecognizer = null
    }

    fun resetSpeechState() {
        _speechState.value = SpeechState.Idle
    }

    // --- Audio Memo Recording ---
    fun startRecordingVoiceMemo(): String? {
        stopRecordingVoiceMemo()
        stopAudioPlayback()

        val voiceDir = File(context.filesDir, "voice_memos")
        if (!voiceDir.exists()) {
            voiceDir.mkdirs()
        }

        val outputFile = File(voiceDir, "memo_${System.currentTimeMillis()}.m4a")
        currentRecordPath = outputFile.absolutePath

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(currentRecordPath)
            try {
                prepare()
                start()
                _isRecording.value = true
                Log.d("VoiceManager", "Recording started: $currentRecordPath")
            } catch (e: IOException) {
                Log.e("VoiceManager", "prepare() failed", e)
                _isRecording.value = false
                currentRecordPath = null
                return null
            } catch (e: IllegalStateException) {
                Log.e("VoiceManager", "start() failed", e)
                _isRecording.value = false
                currentRecordPath = null
                return null
            }
        }

        return currentRecordPath
    }

    fun stopRecordingVoiceMemo(): String? {
        val path = currentRecordPath
        try {
            if (_isRecording.value) {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            }
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error stopping recorder", e)
        }
        mediaRecorder = null
        _isRecording.value = false
        currentRecordPath = null
        return path
    }

    // --- Audio Memo Playback ---
    fun playVoiceMemo(path: String, onCompletion: () -> Unit = {}) {
        if (_currentlyPlayingPath.value == path) {
            stopAudioPlayback()
            return
        }

        stopAudioPlayback()

        val file = File(path)
        if (!file.exists()) {
            Log.w("VoiceManager", "Audio file does not exist: $path")
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                setOnCompletionListener {
                    _currentlyPlayingPath.value = null
                    onCompletion()
                }
                start()
            }
            _currentlyPlayingPath.value = path
        } catch (e: Exception) {
            Log.e("VoiceManager", "Failed to play voice memo", e)
            _currentlyPlayingPath.value = null
        }
    }

    fun stopAudioPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error stopping player", e)
        }
        mediaPlayer = null
        _currentlyPlayingPath.value = null
    }

    fun release() {
        stopListening()
        stopRecordingVoiceMemo()
        stopAudioPlayback()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error releasing TTS", e)
        }
    }
}
