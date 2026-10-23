package com.aashu.offlinechatbot

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aashu.offlinechatbot.data.llm.EngineRunState
import com.aashu.offlinechatbot.ui.chat.ChatScreen
import com.aashu.offlinechatbot.ui.chat.ChatUiState
import com.aashu.offlinechatbot.data.chat.EngineStatus
import com.aashu.offlinechatbot.ui.theme.OfflineChatBotTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatInputBarTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun sendButtonStaysVisibleWhileTyping() {
        rule.setContent {
            OfflineChatBotTheme {
                ChatScreen(
                    state = ChatUiState(
                        modelName = "Gemma 4 E2B",
                        engineStatus = EngineStatus.Ready,
                        runState = EngineRunState.RUNNING,
                    ),
                    onSend = {}, onStop = {}, onNewChat = {}, onRetryLoad = {},
                    onStopModel = {}, onStartModel = {}, onManageModel = {},
                )
            }
        }
        rule.onNodeWithContentDescription("Send").assertIsDisplayed()
        rule.onNode(hasSetTextAction()).performTextInput("Hello there, how are you doing today?")
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Send").assertIsDisplayed()
    }
}
