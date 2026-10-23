package com.aashu.offlinechatbot.data.chat

import android.util.Log
import com.aashu.offlinechatbot.data.llm.EngineRunState
import com.aashu.offlinechatbot.data.llm.LlmEngine
import com.aashu.offlinechatbot.data.repository.ModelRepository
import com.aashu.offlinechatbot.di.ApplicationScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatSession @Inject constructor(
    private val modelRepository: ModelRepository,
    private val engine: LlmEngine,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    val modelName: String = modelRepository.model.displayName

    private val _status = MutableStateFlow<EngineStatus>(
        if (modelRepository.isModelReady()) EngineStatus.Stopped else EngineStatus.ModelMissing
    )
    val status: StateFlow<EngineStatus> = _status.asStateFlow()

    val runState: StateFlow<EngineRunState> = engine.runState

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private var loadJob: Job? = null
    private var generationJob: Job? = null
    private var nextId = 0L

    init {
        scope.launch {
            engine.runState.collect { runState ->
                if (runState == EngineRunState.STOPPED && _status.value == EngineStatus.Ready) {
                    _status.value = EngineStatus.Stopped
                }
            }
        }
    }

    fun ensureLoaded() {
        if (loadJob?.isActive == true) return
        if (_status.value == EngineStatus.Ready && engine.isLoaded) return
        load()
    }

    fun refreshModelStatus() {
        when {
            !modelRepository.isModelReady() -> _status.value = EngineStatus.ModelMissing
            _status.value == EngineStatus.ModelMissing -> load()
        }
    }

    fun stopModel() {
        stopGenerating()
        engine.close()
    }

    fun startModel() = ensureLoaded()

    private fun load() {
        if (!modelRepository.isModelReady()) {
            _status.value = EngineStatus.ModelMissing
            return
        }
        loadJob = scope.launch {
            _status.value = EngineStatus.Loading
            try {
                if (!engine.isLoaded) _messages.value = emptyList()
                engine.load(modelRepository.modelFile.absolutePath)
                _status.value = EngineStatus.Ready
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to load model", e)
                _status.value = EngineStatus.Error(e.message ?: "Failed to load the model")
            }
        }
    }

    fun send(rawText: String, viaVoice: Boolean = false): Long? {
        val text = rawText.trim()
        if (text.isEmpty() || _isGenerating.value || _status.value != EngineStatus.Ready) return null
        if (!engine.isLoaded) {
            load()
            return null
        }

        val userId = nextId++
        val replyId = nextId++
        _isGenerating.value = true
        _messages.update {
            it + ChatMessage(id = userId, text = text, fromUser = true, viaVoice = viaVoice) +
                ChatMessage(id = replyId, text = "", fromUser = false, isStreaming = true, viaVoice = viaVoice)
        }

        generationJob = scope.launch {
            try {
                engine.reply(text).collect { chunk ->
                    updateMessage(replyId) { it.copy(text = it.text + chunk) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "Generation failed", e)
                updateMessage(replyId) { it.copy(text = "Something went wrong: ${e.message}", isError = true) }
            } finally {
                updateMessage(replyId) { msg ->
                    if (msg.text.isBlank() && !msg.isError) msg.copy(text = "(stopped)", isStreaming = false)
                    else msg.copy(isStreaming = false)
                }
                _isGenerating.value = false
            }
        }
        return replyId
    }

    fun stopGenerating() {
        engine.cancel()
        generationJob?.cancel()
    }

    fun newChat() {
        stopGenerating()
        scope.launch {
            generationJob?.join()
            engine.newConversation()
            _messages.value = emptyList()
        }
    }

    private inline fun updateMessage(id: Long, crossinline transform: (ChatMessage) -> ChatMessage) {
        _messages.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    private companion object {
        const val TAG = "ChatSession"
    }
}
