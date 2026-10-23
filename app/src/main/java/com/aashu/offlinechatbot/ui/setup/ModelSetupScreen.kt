package com.aashu.offlinechatbot.ui.setup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import com.aashu.offlinechatbot.data.llm.EngineRunState
import com.aashu.offlinechatbot.data.model.ModelCatalog
import com.aashu.offlinechatbot.ui.components.ModelStatusIndicator
import com.aashu.offlinechatbot.data.model.ModelDownloadState
import com.aashu.offlinechatbot.ui.components.AppIcons
import com.aashu.offlinechatbot.ui.components.GlassCircleButton
import com.aashu.offlinechatbot.ui.components.PrimaryPillButton
import com.aashu.offlinechatbot.ui.components.SecondaryPillButton
import com.aashu.offlinechatbot.ui.components.StarMascot
import com.aashu.offlinechatbot.ui.theme.AppBackgroundBrush
import com.aashu.offlinechatbot.ui.theme.CardWhite
import com.aashu.offlinechatbot.ui.theme.Cream
import com.aashu.offlinechatbot.ui.theme.ErrorRed
import com.aashu.offlinechatbot.ui.theme.Hairline
import com.aashu.offlinechatbot.ui.theme.Ink
import com.aashu.offlinechatbot.ui.theme.OfflineChatBotTheme
import com.aashu.offlinechatbot.ui.theme.SuccessGreen
import com.aashu.offlinechatbot.ui.theme.TextMuted
import com.aashu.offlinechatbot.ui.util.formatBytes

@Composable
fun ModelSetupRoute(
    onStartChat: () -> Unit,
    onBack: (() -> Unit)?,
    viewModel: ModelSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ModelSetupScreen(
        state = state,
        onDownload = viewModel::download,
        onCancel = viewModel::cancel,
        onDelete = viewModel::deleteModel,
        onStopModel = viewModel::stopModel,
        onStartChat = onStartChat,
        onBack = onBack,
    )
}

@Composable
fun ModelSetupScreen(
    state: ModelSetupUiState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onStopModel: () -> Unit,
    onStartChat: () -> Unit,
    onBack: (() -> Unit)?,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(AppBackgroundBrush)
            .systemBarsPadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                if (onBack != null) {
                    GlassCircleButton(AppIcons.ArrowBack, contentDescription = "Back", onClick = onBack)
                }
            }

            StarMascot(Modifier.size(132.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                "Set up your offline AI",
                style = MaterialTheme.typography.headlineSmall,
                color = Ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Download the model once. After that, every chat runs on your phone — no internet needed.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            ModelCard(state, onStopModel)

            Spacer(Modifier.height(28.dp))
            Actions(state, onDownload, onCancel, onDelete, onStartChat)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ModelCard(state: ModelSetupUiState, onStopModel: () -> Unit) {
    val model = state.model
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x22000000), spotColor = Color(0x22000000))
            .clip(RoundedCornerShape(28.dp))
            .background(CardWhite)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Cream),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AppIcons.Chip, contentDescription = null, tint = Ink, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(model.displayName, style = MaterialTheme.typography.titleLarge, color = Ink)
                Text(
                    "${model.publisher} · ${formatBytes(model.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }
            if (state.download is ModelDownloadState.Ready) {
                Icon(AppIcons.CheckCircle, contentDescription = "Downloaded", tint = SuccessGreen)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(model.description, style = MaterialTheme.typography.bodyMedium, color = Ink)
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
        Spacer(Modifier.height(14.dp))
        FeatureRow(AppIcons.WifiOff, "Works with no internet connection")
        FeatureRow(AppIcons.Lock, "Your chats never leave this device")
        FeatureRow(AppIcons.Sparkle, "Streams answers in real time")

        Spacer(Modifier.height(8.dp))
        if (state.download is ModelDownloadState.Ready) {
            Spacer(Modifier.height(8.dp))
            RunStatusRow(state.runState, onStopModel)
        }
        AnimatedContent(targetState = state.download::class, label = "download") { _ ->
            DownloadStatus(state)
        }
    }
}

@Composable
private fun RunStatusRow(runState: EngineRunState, onStopModel: () -> Unit) {
    val inMemory = runState == EngineRunState.RUNNING || runState == EngineRunState.GENERATING
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Cream)
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
            .heightIn(min = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ModelStatusIndicator(runState, Modifier.weight(1f))
        if (inMemory) {
            Text(
                "Stop",
                style = MaterialTheme.typography.labelMedium,
                color = ErrorRed,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onStopModel)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, text: String) {
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}

@Composable
private fun DownloadStatus(state: ModelSetupUiState) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        when (val download = state.download) {
            is ModelDownloadState.Downloading -> ProgressBlock(
                progress = download.progress,
                label = "Downloading…",
                detail = "${formatBytes(download.downloadedBytes)} of ${formatBytes(download.totalBytes)}",
            )

            is ModelDownloadState.Paused -> ProgressBlock(
                progress = if (download.totalBytes > 0) download.downloadedBytes.toFloat() / download.totalBytes else 0f,
                label = download.reason,
                detail = "${formatBytes(download.downloadedBytes)} of ${formatBytes(download.totalBytes)}",
            )

            is ModelDownloadState.Failed -> Text(
                download.message,
                style = MaterialTheme.typography.bodySmall,
                color = ErrorRed,
            )

            ModelDownloadState.Ready -> Text(
                "Ready to use offline",
                style = MaterialTheme.typography.labelMedium,
                color = SuccessGreen,
            )

            ModelDownloadState.NotDownloaded -> Text(
                "Wi-Fi recommended. The download continues in the background.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
        state.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
        }
    }
}

@Composable
private fun ProgressBlock(progress: Float, label: String, detail: String) {
    val animated by animateFloatAsState(progress, label = "progress")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Ink)
        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = Ink)
    }
    Spacer(Modifier.height(8.dp))
    LinearProgressIndicator(
        progress = { animated },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape),
        color = Ink,
        trackColor = Hairline,
        strokeCap = StrokeCap.Round,
        drawStopIndicator = {},
    )
    Spacer(Modifier.height(6.dp))
    Text(detail, style = MaterialTheme.typography.bodySmall, color = TextMuted)
}

@Composable
private fun Actions(
    state: ModelSetupUiState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onStartChat: () -> Unit,
) {
    when (state.download) {
        ModelDownloadState.NotDownloaded -> PrimaryPillButton(
            text = if (state.isStarting) "Starting…" else "Download model · ${formatBytes(state.model.sizeBytes)}",
            icon = AppIcons.Download,
            onClick = onDownload,
            enabled = !state.isStarting,
        )

        is ModelDownloadState.Failed -> PrimaryPillButton(
            text = "Try again",
            icon = AppIcons.Download,
            onClick = onDownload,
            enabled = !state.isStarting,
        )

        is ModelDownloadState.Downloading,
        is ModelDownloadState.Paused -> SecondaryPillButton(
            text = "Cancel download",
            icon = AppIcons.Close,
            onClick = onCancel,
        )

        ModelDownloadState.Ready -> Column {
            PrimaryPillButton(text = "Start chatting", icon = AppIcons.Sparkle, onClick = onStartChat)
            Spacer(Modifier.height(14.dp))
            SecondaryPillButton(
                text = "Delete model",
                icon = AppIcons.Delete,
                onClick = onDelete,
                contentColor = ErrorRed,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ModelSetupPreview() {
    OfflineChatBotTheme {
        ModelSetupScreen(
            state = ModelSetupUiState(
                model = ModelCatalog.default,
                download = ModelDownloadState.Downloading(1_100_000_000, ModelCatalog.default.sizeBytes),
            ),
            onDownload = {}, onCancel = {}, onDelete = {}, onStopModel = {}, onStartChat = {}, onBack = {},
        )
    }
}
