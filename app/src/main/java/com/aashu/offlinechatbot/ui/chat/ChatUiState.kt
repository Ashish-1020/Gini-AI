package com.aashu.offlinechatbot.ui.chat

import com.aashu.offlinechatbot.data.chat.ChatMessage
import com.aashu.offlinechatbot.data.chat.EngineStatus
import com.aashu.offlinechatbot.data.llm.EngineRunState

data class ChatUiState(
    val modelName: String,
    val engineStatus: EngineStatus = EngineStatus.Loading,
    val runState: EngineRunState = EngineRunState.STOPPED,
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
)
