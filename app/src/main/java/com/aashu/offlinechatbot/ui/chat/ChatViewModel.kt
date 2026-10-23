package com.aashu.offlinechatbot.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aashu.offlinechatbot.data.chat.ChatSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val session: ChatSession,
) : ViewModel() {
    val uiState: StateFlow<ChatUiState> = combine(
        session.status,
        session.runState,
        session.messages,
        session.isGenerating,
    ) { status, runState, messages, generating ->
        ChatUiState(session.modelName, status, runState, messages, generating)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ChatUiState(
            modelName = session.modelName,
            engineStatus = session.status.value,
            runState = session.runState.value,
            messages = session.messages.value,
            isGenerating = session.isGenerating.value,
        ),
    )

    init {
        session.ensureLoaded()
    }

    fun refreshModelStatus() = session.refreshModelStatus()
    fun loadModel() = session.ensureLoaded()
    fun send(text: String) {
        session.send(text)
    }
    fun stopGenerating() = session.stopGenerating()
    fun newChat() = session.newChat()
    fun stopModel() = session.stopModel()
    fun startModel() = session.startModel()
}
