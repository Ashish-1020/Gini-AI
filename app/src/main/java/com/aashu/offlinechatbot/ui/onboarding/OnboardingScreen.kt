package com.aashu.offlinechatbot.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aashu.offlinechatbot.ui.components.AppIcons
import com.aashu.offlinechatbot.ui.components.OnboardingIllustration
import com.aashu.offlinechatbot.ui.components.PrimaryPillButton
import com.aashu.offlinechatbot.ui.theme.AppBackgroundBrush
import com.aashu.offlinechatbot.ui.theme.Ink
import com.aashu.offlinechatbot.ui.theme.OfflineChatBotTheme
import com.aashu.offlinechatbot.ui.theme.TextMuted

@Composable
fun OnboardingRoute(
    onContinue: (modelReady: Boolean) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    OnboardingScreen(onGetStarted = { onContinue(viewModel.completeOnboarding()) })
}

@Composable
fun OnboardingScreen(onGetStarted: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(AppBackgroundBrush)
            .systemBarsPadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OnboardingIllustration(Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 16.dp)
            )

            Text(
                text = "Deep insights, simple conversations",
                style = MaterialTheme.typography.displaySmall,
                color = Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Chat with Gemma, privately on your device",
                style = MaterialTheme.typography.bodyLarge,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(36.dp))

            PrimaryPillButton(text = "Get Started", icon = AppIcons.Sparkle, onClick = onGetStarted)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun OnboardingPreview() {
    OfflineChatBotTheme { OnboardingScreen(onGetStarted = {}) }
}
