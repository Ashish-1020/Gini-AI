package com.aashu.offlinechatbot.data.llm

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteRtLlmEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : LlmEngine {
    private val mutex = Mutex()

    @Volatile private var engine: Engine? = null
    @Volatile private var conversation: Conversation? = null

    private val _runState = MutableStateFlow(EngineRunState.STOPPED)
    override val runState: StateFlow<EngineRunState> = _runState.asStateFlow()

    override val isLoaded: Boolean
        get() = conversation != null

    override suspend fun load(modelPath: String) = mutex.withLock {
        if (engine != null) return@withLock
        _runState.value = EngineRunState.LOADING
        try {
            withContext(Dispatchers.IO) {
                val newEngine = Engine(
                    EngineConfig(
                        modelPath = modelPath,
                        backend = Backend.CPU(),
                        cacheDir = context.cacheDir.path,
                    )
                )
                newEngine.initialize()
                engine = newEngine
                conversation = newEngine.createConversation(conversationConfig())
            }
            _runState.value = EngineRunState.RUNNING
            ModelSessionService.start(context)
        } catch (e: Throwable) {
            _runState.value = EngineRunState.STOPPED
            throw e
        }
    }

    override fun reply(prompt: String): Flow<String> {
        val conversation = checkNotNull(conversation) { "Model is not loaded" }
        return conversation.sendMessageAsync(prompt)
            .map { message ->
                message.contents.contents
                    .filterIsInstance<Content.Text>()
                    .joinToString("") { it.text }
            }
            .onStart { _runState.value = EngineRunState.GENERATING }
            .onCompletion {
                if (_runState.value == EngineRunState.GENERATING) _runState.value = EngineRunState.RUNNING
            }
            .flowOn(Dispatchers.IO)
    }

    override fun cancel() {
        conversation?.cancelProcess()
    }

    override suspend fun newConversation() = mutex.withLock {
        val engine = engine ?: return@withLock
        withContext(Dispatchers.IO) {
            conversation?.close()
            conversation = engine.createConversation(conversationConfig())
        }
    }

    override fun close() {
        val conversation = conversation
        val engine = engine
        this.conversation = null
        this.engine = null
        _runState.value = EngineRunState.STOPPED
        ModelSessionService.stop(context)
        try {
            conversation?.cancelProcess()
            conversation?.close()
            engine?.close()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.w(TAG, "Error while unloading the model", e)
        }
    }

    private fun conversationConfig() = ConversationConfig(
        systemInstruction = Contents.of(SYSTEM_PROMPT),
        samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.8),
    )

    private companion object {
        const val TAG = "LiteRtLlmEngine"
        const val SYSTEM_PROMPT =
            "You are Gemma, a friendly and helpful assistant running fully offline on the user's phone. " +
                "Give clear, well-structured answers and use Markdown where it helps."
    }
}
