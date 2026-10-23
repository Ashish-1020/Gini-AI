package com.aashu.offlinechatbot.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aashu.offlinechatbot.data.chat.EngineStatus
import com.aashu.offlinechatbot.data.voice.stripMarkdown
import com.aashu.offlinechatbot.ui.components.AppIcons
import com.aashu.offlinechatbot.ui.components.GlassCircleButton
import com.aashu.offlinechatbot.ui.theme.AppBackgroundBrush
import com.aashu.offlinechatbot.ui.theme.CardWhite
import com.aashu.offlinechatbot.ui.theme.Cream
import com.aashu.offlinechatbot.ui.theme.GlassWhite
import com.aashu.offlinechatbot.ui.theme.Hairline
import com.aashu.offlinechatbot.ui.theme.Ink
import com.aashu.offlinechatbot.ui.theme.OfflineChatBotTheme
import com.aashu.offlinechatbot.ui.theme.TextFaint
import com.aashu.offlinechatbot.ui.theme.TextMuted
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

@Composable
fun VoiceRoute(
    onBack: () -> Unit,
    onOpenChat: () -> Unit,
    viewModel: VoiceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.onMicTap() else viewModel.onPermissionDenied()
    }
    val onMic = {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.onMicTap() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    LifecycleStartEffect(Unit) {
        onStopOrDispose { viewModel.endSession() }
    }

    VoiceScreen(
        state = state,
        onMic = onMic,
        onBack = { viewModel.endSession(); onBack() },
        onOpenChat = { viewModel.endSession(); onOpenChat() },
        onNewConversation = viewModel::newConversation,
    )
}

@Composable
fun VoiceScreen(
    state: VoiceUiState,
    onMic: () -> Unit,
    onBack: () -> Unit,
    onOpenChat: () -> Unit,
    onNewConversation: () -> Unit = {},
) {
    val conversing = state.phase != VoicePhase.Idle || state.reply.isNotEmpty()
    val waveHeight by animateDpAsState(if (conversing) 110.dp else 170.dp, tween(300), label = "waveHeight")
    val waveGap by animateDpAsState(if (conversing) 12.dp else 28.dp, tween(300), label = "waveGap")

    Box(
        Modifier
            .fillMaxSize()
            .background(AppBackgroundBrush)
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            VoiceTopBar(onBack = onBack, onNewConversation = onNewConversation)

            Spacer(Modifier.height(waveGap))
            VoiceWaveform(
                phase = state.phase,
                level = state.level,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(waveHeight)
                    .padding(horizontal = 40.dp),
            )
            Spacer(Modifier.height(waveGap))

            StatusPill(statusLabel(state))
            Spacer(Modifier.height(12.dp))

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .padding(horizontal = 28.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Transcript(state)
            }

            BottomSheet(state = state, onMic = onMic, onOpenChat = onOpenChat, onClose = onBack)
        }
    }
}

@Composable
private fun VoiceTopBar(onBack: () -> Unit, onNewConversation: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassCircleButton(AppIcons.ArrowBack, contentDescription = "Back", onClick = onBack)
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Ink)) { append("Voice ") }
                withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = TextFaint)) { append("Chat") }
            },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        GlassCircleButton(AppIcons.NewChat, contentDescription = "New conversation", onClick = onNewConversation)
    }
}

private fun statusLabel(state: VoiceUiState): String = when (state.engineStatus) {
    EngineStatus.Ready -> "${state.modelName}  •  Voice assistant"
    EngineStatus.Loading -> "Starting ${state.modelName}…"
    EngineStatus.Stopped -> "Model stopped  •  tap the mic to start"
    EngineStatus.ModelMissing -> "Model not downloaded"
    is EngineStatus.Error -> "Model failed to load"
}

@Composable
private fun StatusPill(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
        color = TextMuted,
        modifier = Modifier
            .clip(CircleShape)
            .background(Cream)
            .border(1.dp, Hairline, CircleShape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@Composable
private fun Transcript(state: VoiceUiState) {
    val active = state.phase != VoicePhase.Idle || state.reply.isNotEmpty()
    AnimatedContent(
        targetState = active,
        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
        label = "transcript",
    ) { showTranscript ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            if (!showTranscript) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = Ink)) { append("Say it, ") }
                        withStyle(
                            SpanStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, color = TextFaint)
                        ) { append("hear it") }
                    },
                    fontSize = 40.sp,
                    lineHeight = 46.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    state.hint ?: "Ask me anything out loud.\nI'll answer right away, fully offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                )
            } else {
                ConversationText(state)
            }
        }
    }
}

@Composable
private fun ConversationText(state: VoiceUiState) {
    val scroll = rememberScrollState()

    LaunchedEffect(scroll) {
        snapshotFlow { scroll.maxValue }.collect { max ->
            val nearBottom = max - scroll.value < 400
            if (nearBottom && !scroll.isScrollInProgress) scroll.scrollTo(max)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(top = 4.dp, bottom = 24.dp),
    ) {
        if (state.heard.isNotBlank()) {
            Text(
                "“${state.heard}”",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
        }
        val reply = stripMarkdown(state.reply)
        if (reply.isNotEmpty()) {
            Text(
                reply,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                fontFamily = FontFamily.Serif,
                color = Ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
        }
        state.hint?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = TextFaint, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun BottomSheet(
    state: VoiceUiState,
    onMic: () -> Unit,
    onOpenChat: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(190.dp)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val curveTop = size.height * 0.30f
            val sheet = Path().apply {
                moveTo(0f, curveTop)
                quadraticTo(size.width / 2, -curveTop * 0.9f, size.width, curveTop)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(sheet, Color.White.copy(alpha = 0.95f))
        }
        Box(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
        ) {
            MicButton(state = state, onClick = onMic, modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp))
            SmallRoundButton(
                icon = AppIcons.ChatBubble,
                contentDescription = "Open text chat",
                onClick = onOpenChat,
                modifier = Modifier.align(Alignment.BottomStart),
            )
            SmallRoundButton(
                icon = AppIcons.Close,
                contentDescription = "End voice chat",
                onClick = onClose,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

@Composable
private fun SmallRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(48.dp)
            .shadow(3.dp, CircleShape, ambientColor = Color(0x1A000000), spotColor = Color(0x1A000000))
            .clip(CircleShape)
            .background(GlassWhite)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Ink, modifier = Modifier.size(22.dp))
    }
}

private val GlowRed = Color(0xFFE0483C)

@Composable
private fun MicButton(state: VoiceUiState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val time = rememberAnimationTime()
    val listening = state.phase == VoicePhase.Listening
    val level by animateFloatAsState(if (listening) state.level else 0f, tween(120), label = "micLevel")
    val breathe = 0.5f + 0.5f * sin(time * 2.2f)
    val glow = when (state.phase) {
        VoicePhase.Listening -> 0.55f + 0.45f * level
        VoicePhase.Idle -> 0.35f + 0.15f * breathe
        else -> 0.45f + 0.2f * breathe
    }
    val busy = state.phase == VoicePhase.Thinking || state.phase == VoicePhase.Speaking

    Box(modifier.size(112.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GlowRed.copy(alpha = 0.55f * glow), GlowRed.copy(alpha = 0f)),
                    center = center,
                    radius = size.minDimension / 2,
                ),
                radius = size.minDimension / 2 * (0.82f + 0.18f * glow),
            )
        }
        Box(
            Modifier
                .size(74.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF4A2B27), Color(0xFF171010))))
                .border(2.dp, GlowRed.copy(alpha = 0.75f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (busy) AppIcons.Stop else AppIcons.Mic,
                contentDescription = when {
                    busy -> "Interrupt and talk"
                    listening -> "Done talking"
                    else -> "Start talking"
                },
                tint = Color.White,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun rememberAnimationTime(): Float {
    val time by produceState(0f) {
        val start = withFrameMillis { it }
        while (true) withFrameMillis { value = (it - start) / 1000f }
    }
    return time
}

@Composable
fun VoiceWaveform(phase: VoicePhase, level: Float, modifier: Modifier = Modifier) {
    val time = rememberAnimationTime()
    val smoothLevel by animateFloatAsState(level, tween(140), label = "level")
    val tracks = floatArrayOf(0.30f, 0.55f, 0.80f, 1.0f, 0.72f, 0.90f, 0.62f, 0.45f, 0.28f)

    Canvas(modifier) {
        val count = tracks.size
        val gap = 10.dp.toPx()
        val barW = ((size.width - gap * (count - 1)) / count).coerceAtMost(26.dp.toPx())
        val totalW = barW * count + gap * (count - 1)
        val startX = (size.width - totalW) / 2
        val midY = size.height / 2
        val radius = CornerRadius(barW / 2, barW / 2)

        tracks.forEachIndexed { i, track ->
            val x = startX + i * (barW + gap)
            val trackH = max(barW, size.height * track)
            val energy = when (phase) {
                VoicePhase.Idle -> 0.22f + 0.08f * sin(time * 1.6f + i * 0.8f)
                VoicePhase.Listening -> 0.15f + 0.85f * smoothLevel * (0.6f + 0.4f * sin(time * 9f + i * 1.1f))
                VoicePhase.Thinking -> 0.2f + 0.55f * max(0f, sin(time * 4f - i * 0.7f))
                VoicePhase.Speaking ->
                    0.35f + 0.6f * abs(sin(time * 5.2f + i * 1.3f) * sin(time * 2.9f + i * 0.5f))
            }.coerceIn(0.1f, 1f)
            val barH = max(barW, trackH * energy)

            drawRoundRect(
                color = Color.Black.copy(alpha = 0.05f),
                topLeft = Offset(x, midY - trackH / 2 + 3.dp.toPx()),
                size = Size(barW, trackH),
                cornerRadius = radius,
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color.White, Color(0xFFF1EEE6)),
                    startY = midY - trackH / 2,
                    endY = midY + trackH / 2,
                ),
                topLeft = Offset(x, midY - trackH / 2),
                size = Size(barW, trackH),
                cornerRadius = radius,
            )
            val edge = i == 0 || i == count - 1
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF5A544D), Color(0xFF1C1916)),
                    startY = midY - barH / 2,
                    endY = midY + barH / 2,
                ),
                topLeft = Offset(x + barW * 0.12f, midY - barH / 2),
                size = Size(barW * 0.76f, barH),
                cornerRadius = CornerRadius(barW * 0.38f, barW * 0.38f),
                alpha = if (edge) 0.75f else 1f,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun VoicePreview() {
    OfflineChatBotTheme {
        VoiceScreen(
            state = VoiceUiState(modelName = "Gemma 4 E2B", engineStatus = EngineStatus.Ready),
            onMic = {}, onBack = {}, onOpenChat = {},
        )
    }
}
