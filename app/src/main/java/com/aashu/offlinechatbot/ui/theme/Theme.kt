package com.aashu.offlinechatbot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = CardWhite,
    secondary = InkSoft,
    onSecondary = CardWhite,
    background = Cream,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    surfaceVariant = Cream,
    onSurfaceVariant = TextMuted,
    outline = Hairline,
    error = ErrorRed,
)

@Composable
fun OfflineChatBotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = Typography,
        content = content,
    )
}
