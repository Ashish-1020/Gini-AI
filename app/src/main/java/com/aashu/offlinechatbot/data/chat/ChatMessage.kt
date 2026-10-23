package com.aashu.offlinechatbot.data.chat

data class ChatMessage(
    val id: Long,
    val text: String,
    val fromUser: Boolean,
    val isStreaming: Boolean = false,
    val isError: Boolean = false,
    val viaVoice: Boolean = false,
)

sealed interface EngineStatus {
    data object Loading : EngineStatus
    data object Ready : EngineStatus
    data object Stopped : EngineStatus
    data object ModelMissing : EngineStatus
    data class Error(val message: String) : EngineStatus
}
