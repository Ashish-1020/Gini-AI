package com.aashu.offlinechatbot.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aashu.offlinechatbot.data.chat.ChatSession
import com.aashu.offlinechatbot.data.chat.EngineStatus
import com.aashu.offlinechatbot.data.voice.Speaker
import com.aashu.offlinechatbot.data.voice.SpeechEvent
import com.aashu.offlinechatbot.data.voice.SpeechToText
import com.aashu.offlinechatbot.data.voice.lastSentenceEnd
import com.aashu.offlinechatbot.data.voice.stripMarkdown
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class VoicePhase { Idle, Listening, Thinking, Speaking }

data class VoiceUiState(
    val modelName: String,
    val engineStatus: EngineStatus,
    val phase: VoicePhase = VoicePhase.Idle,
    val heard: String = "",
    val reply: String = "",
    val level: Float = 0f,
    val hint: String? = null,
)

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val session: ChatSession,
    private val speechToText: SpeechToText,
    private val speaker: Speaker,
) : ViewModel() {
    private val _uiState = MutableStateFlow(VoiceUiState(session.modelName, session.status.value))
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private var listenJob: Job? = null
    private var replyJob: Job? = null

    init {
        session.ensureLoaded()
        viewModelScope.launch {
            session.status.collect { status -> _uiState.update { it.copy(engineStatus = status) } }
        }
    }

    fun onMicTap() {
        when (_uiState.value.phase) {
            VoicePhase.Idle -> startListening()
            VoicePhase.Listening -> stopListening()
            VoicePhase.Thinking, VoicePhase.Speaking -> {
                interrupt()
                startListening()
            }
        }
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(hint = "Allow microphone access to talk with Gemma.") }
    }

    fun endSession() {
        listenJob?.cancel()
        interrupt()
        _uiState.update { it.copy(phase = VoicePhase.Idle, level = 0f) }
    }

    fun newConversation() {
        endSession()
        session.newChat()
        _uiState.update { it.copy(heard = "", reply = "", hint = null) }
    }

    private fun startListening() {
        if (!ensureReady()) return
        if (!speechToText.isAvailable()) {
            _uiState.update { it.copy(hint = "Speech recognition isn't available on this device.") }
            return
        }
        listenJob?.cancel()
        _uiState.update { it.copy(phase = VoicePhase.Listening, heard = "", reply = "", level = 0f, hint = "Listening…") }
        listenJob = viewModelScope.launch {
            speechToText.listen().collect { event ->
                when (event) {
                    SpeechEvent.Ready -> Unit
                    is SpeechEvent.Level -> _uiState.update { it.copy(level = event.value) }
                    is SpeechEvent.Partial -> _uiState.update { it.copy(heard = event.text) }
                    is SpeechEvent.Final ->
                        if (event.text.isBlank()) idle("I didn't catch that. Tap the mic to try again.")
                        else respond(event.text)
                    is SpeechEvent.Error -> idle(
                        if (event.noSpeech) "I didn't catch that. Tap the mic to try again." else event.message
                    )
                }
            }
        }
    }

    private fun stopListening() {
        listenJob?.cancel()
        val heard = _uiState.value.heard
        if (heard.isNotBlank()) respond(heard) else idle(null)
    }

    private fun respond(text: String) {
        if (!ensureReady()) return
        listenJob?.cancel()
        val replyId = session.send(text, viaVoice = true)
        if (replyId == null) {
            idle("Gemma is busy. Try again in a moment.")
            return
        }
        _uiState.update {
            it.copy(phase = VoicePhase.Thinking, heard = text, reply = "", level = 0f, hint = "Thinking…")
        }

        replyJob = viewModelScope.launch {
            var spokenUpTo = 0
            var failed = false
            session.messages
                .mapNotNull { list -> list.firstOrNull { it.id == replyId } }
                .transformWhile { emit(it); it.isStreaming }
                .collect { msg ->
                    failed = msg.isError
                    if (msg.text.isNotEmpty() && !msg.isError) {
                        _uiState.update {
                            it.copy(phase = VoicePhase.Speaking, reply = msg.text, hint = "Tap the mic to interrupt")
                        }
                    }
                    val end = if (msg.isStreaming) lastSentenceEnd(msg.text, spokenUpTo) else msg.text.length
                    if (!msg.isError && end > spokenUpTo) {
                        speaker.speak(stripMarkdown(msg.text.substring(spokenUpTo, end)))
                        spokenUpTo = end
                    }
                }
            if (failed) {
                idle("Gemma couldn't answer that. Try again.")
                return@launch
            }
            speaker.isSpeaking.first { !it }
            startListening()
        }
    }

    private fun interrupt() {
        replyJob?.cancel()
        speaker.stop()
        if (session.isGenerating.value) session.stopGenerating()
    }

    private fun ensureReady(): Boolean {
        return when (session.status.value) {
            EngineStatus.Ready -> true
            EngineStatus.ModelMissing -> {
                idle("Download the model first from the chat screen.")
                false
            }
            EngineStatus.Loading -> {
                idle("Gemma is starting up. Try again in a few seconds.")
                false
            }
            else -> {
                session.ensureLoaded()
                idle("Starting Gemma… tap the mic again in a few seconds.")
                false
            }
        }
    }

    private fun idle(hint: String?) {
        _uiState.update { it.copy(phase = VoicePhase.Idle, level = 0f, hint = hint) }
    }

    override fun onCleared() {
        endSession()
    }
}
