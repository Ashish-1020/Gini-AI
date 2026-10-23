package com.aashu.offlinechatbot.data.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class EngineRunState {
    STOPPED,

    LOADING,

    RUNNING,

    GENERATING,
}

interface LlmEngine {
    val isLoaded: Boolean

    val runState: StateFlow<EngineRunState>

    suspend fun load(modelPath: String)

    fun reply(prompt: String): Flow<String>

    fun cancel()

    suspend fun newConversation()

    fun close()
}
