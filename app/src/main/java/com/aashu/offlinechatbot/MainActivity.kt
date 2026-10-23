package com.aashu.offlinechatbot

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.aashu.offlinechatbot.data.llm.LlmEngine
import com.aashu.offlinechatbot.ui.MainViewModel
import com.aashu.offlinechatbot.ui.navigation.AppNavHost
import com.aashu.offlinechatbot.ui.theme.OfflineChatBotTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    @Inject
    lateinit var llmEngine: LlmEngine

    override fun onDestroy() {
        if (isFinishing) llmEngine.close()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            OfflineChatBotTheme {
                AppNavHost(startDestination = viewModel.startDestination)
            }
        }
    }
}
