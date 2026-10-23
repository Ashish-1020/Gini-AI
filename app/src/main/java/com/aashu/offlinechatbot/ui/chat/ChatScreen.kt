package com.aashu.offlinechatbot.ui.chat

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aashu.offlinechatbot.data.chat.ChatMessage
import com.aashu.offlinechatbot.data.chat.EngineStatus
import com.aashu.offlinechatbot.data.llm.EngineRunState
import com.aashu.offlinechatbot.ui.components.ActionChip
import com.aashu.offlinechatbot.ui.components.AppIcons
import com.aashu.offlinechatbot.ui.components.GlassCircleButton
import com.aashu.offlinechatbot.ui.components.ModelStatusIndicator
import com.aashu.offlinechatbot.ui.components.PrimaryPillButton
import com.aashu.offlinechatbot.ui.components.StarMascot
import com.aashu.offlinechatbot.ui.theme.AppBackgroundBrush
import com.aashu.offlinechatbot.ui.theme.CardWhite
import com.aashu.offlinechatbot.ui.theme.Cream
import com.aashu.offlinechatbot.ui.theme.ErrorRed
import com.aashu.offlinechatbot.ui.theme.GlassWhite
import com.aashu.offlinechatbot.ui.theme.Hairline
import com.aashu.offlinechatbot.ui.theme.Ink
import com.aashu.offlinechatbot.ui.theme.InkGradient
import com.aashu.offlinechatbot.ui.theme.OfflineChatBotTheme
import com.aashu.offlinechatbot.ui.theme.TextFaint
import com.aashu.offlinechatbot.ui.theme.TextMuted
import kotlinx.coroutines.launch

private data class QuickAction(val label: String, val prefix: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val quickActions = listOf(
    QuickAction("Summarize", "Summarize this:", AppIcons.Document),
    QuickAction("Analyze", "Analyze this:", AppIcons.TrendingUp),
    QuickAction("Rewrite", "Rewrite this more clearly:", AppIcons.Edit),
    QuickAction("Ideas", "Give me some ideas for", AppIcons.Sparkle),
)

private val suggestions = listOf(
    "Explain how black holes form, simply",
    "Write a short poem about monsoon rain",
    "Plan a 3-day trip to Jaipur",
    "Tips to learn Kotlin coroutines faster",
)

@Composable
fun ChatRoute(
    onManageModel: () -> Unit,
    onOpenVoice: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refreshModelStatus()
        onPauseOrDispose { }
    }
    ChatScreen(
        state = state,
        onSend = viewModel::send,
        onStop = viewModel::stopGenerating,
        onNewChat = viewModel::newChat,
        onRetryLoad = viewModel::loadModel,
        onStopModel = viewModel::stopModel,
        onStartModel = viewModel::startModel,
        onManageModel = onManageModel,
        onOpenVoice = onOpenVoice,
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onNewChat: () -> Unit,
    onRetryLoad: () -> Unit,
    onStopModel: () -> Unit,
    onStartModel: () -> Unit,
    onManageModel: () -> Unit,
    onOpenVoice: () -> Unit = {},
) {
    var input by rememberSaveable { mutableStateOf("") }
    val isReady = state.engineStatus == EngineStatus.Ready
    val context = LocalContext.current

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (spoken.isNotBlank()) input = listOf(input.trim(), spoken).filter { it.isNotEmpty() }.joinToString(" ")
        }
    }
    val startVoiceInput = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask Gemma anything")
        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Voice input isn't available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    fun submit(text: String) {
        if (text.isBlank()) return
        onSend(text)
        input = ""
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(AppBackgroundBrush)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            ChatTopBar(
                state = state,
                onNewChat = onNewChat,
                onStopModel = onStopModel,
                onStartModel = onStartModel,
                onManageModel = onManageModel,
                onOpenVoice = onOpenVoice,
            )

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.35f))
                    .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(32.dp))
            ) {
                when (val status = state.engineStatus) {
                    EngineStatus.Loading -> CenteredCard {
                        StarMascot(Modifier.size(96.dp))
                        Spacer(Modifier.height(16.dp))
                        CircularProgressIndicator(color = Ink, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Loading ${state.modelName}…", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Warming up the model on your device. This takes a few seconds.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                        )
                    }

                    EngineStatus.ModelMissing -> CenteredCard {
                        StarMascot(Modifier.size(96.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Model not downloaded", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Download it once to start chatting offline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(20.dp))
                        PrimaryPillButton("Download model", onClick = onManageModel, icon = AppIcons.Download)
                    }

                    EngineStatus.Stopped -> CenteredCard {
                        StarMascot(Modifier.size(96.dp), animated = false)
                        Spacer(Modifier.height(16.dp))
                        Text("Model is stopped", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "It isn't running in the background or using memory. Start it to chat again.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(20.dp))
                        PrimaryPillButton("Start model", onClick = onStartModel, icon = AppIcons.Sparkle)
                    }

                    is EngineStatus.Error -> CenteredCard {
                        Text("Couldn't load the model", style = MaterialTheme.typography.titleMedium, color = ErrorRed)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            status.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(20.dp))
                        PrimaryPillButton("Try again", onClick = onRetryLoad)
                    }

                    EngineStatus.Ready -> if (state.messages.isEmpty()) {
                        WelcomeCard(onSuggestion = ::submit)
                    } else {
                        MessageList(state.messages)
                    }
                }
            }

            QuickActionRow(
                enabled = isReady && !state.isGenerating,
                onAction = { action ->
                    if (input.isNotBlank()) submit("${action.prefix}\n${input.trim()}")
                    else input = "${action.prefix} "
                },
            )

            InputBar(
                value = input,
                onValueChange = { input = it },
                enabled = isReady,
                isGenerating = state.isGenerating,
                onSend = { submit(input) },
                onStop = onStop,
                onMic = startVoiceInput,
                onOpenVoice = onOpenVoice,
            )
        }
    }
}

@Composable
private fun ChatTopBar(
    state: ChatUiState,
    onNewChat: () -> Unit,
    onStopModel: () -> Unit,
    onStartModel: () -> Unit,
    onManageModel: () -> Unit,
    onOpenVoice: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val subtitle = when (state.engineStatus) {
        EngineStatus.ModelMissing -> "Model not downloaded"
        is EngineStatus.Error -> "Model failed to load"
        else -> when (state.runState) {
            EngineRunState.RUNNING -> "Running · ${state.modelName}"
            EngineRunState.GENERATING -> "Gemma is typing…"
            EngineRunState.LOADING -> "Starting ${state.modelName}…"
            EngineRunState.STOPPED -> "Model stopped"
        }
    }
    val modelInMemory = state.runState == EngineRunState.RUNNING || state.runState == EngineRunState.GENERATING

    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(GlassWhite)
                .border(1.dp, CardWhite, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            StarMascot(Modifier.size(34.dp), animated = false)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Gemma Chat", style = MaterialTheme.typography.titleLarge, color = Ink)
            ModelStatusIndicator(runState = state.runState, label = subtitle)
        }
        Box {
            GlassCircleButton(AppIcons.MoreVert, contentDescription = "Menu", onClick = { menuOpen = true })
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = CardWhite,
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Model status", style = MaterialTheme.typography.labelMedium, color = Ink)
                    Spacer(Modifier.height(4.dp))
                    ModelStatusIndicator(runState = state.runState)
                }
                HorizontalDivider(color = Hairline)
                if (modelInMemory) {
                    DropdownMenuItem(
                        text = { Text("Stop model") },
                        leadingIcon = { Icon(AppIcons.Stop, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        onClick = { menuOpen = false; onStopModel() },
                    )
                } else if (state.engineStatus == EngineStatus.Stopped) {
                    DropdownMenuItem(
                        text = { Text("Start model") },
                        leadingIcon = { Icon(AppIcons.Sparkle, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        onClick = { menuOpen = false; onStartModel() },
                    )
                }
                DropdownMenuItem(
                    text = { Text("New chat") },
                    enabled = state.engineStatus == EngineStatus.Ready,
                    leadingIcon = { Icon(AppIcons.NewChat, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    onClick = { menuOpen = false; onNewChat() },
                )
                DropdownMenuItem(
                    text = { Text("Manage model") },
                    leadingIcon = { Icon(AppIcons.Chip, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    onClick = { menuOpen = false; onManageModel() },
                )
            }
        }
    }
}

@Composable
private fun CenteredCard(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x1A000000), spotColor = Color(0x1A000000))
                .clip(RoundedCornerShape(28.dp))
                .background(CardWhite)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { content() }
    }
}

@Composable
private fun WelcomeCard(onSuggestion: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x1A000000), spotColor = Color(0x1A000000))
                .clip(RoundedCornerShape(28.dp))
                .background(CardWhite)
                .padding(20.dp)
        ) {
            Text("GEMMA CHAT", style = MaterialTheme.typography.labelMedium, color = Ink)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StarMascot(Modifier.size(64.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("How can I help today?", style = MaterialTheme.typography.headlineSmall, color = Ink)
                    Text(
                        "Private & offline. Nothing leaves your phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Try asking", style = MaterialTheme.typography.titleMedium, color = Ink)
            Spacer(Modifier.height(8.dp))
            suggestions.forEach { suggestion ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Cream.copy(alpha = 0.6f))
                        .clickable { onSuggestion(suggestion) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("•", color = Ink, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(10.dp))
                    Text(suggestion, color = Ink, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun MessageList(messages: List<ChatMessage>) {
    val listState = rememberLazyListState()
    val reversed = remember(messages) { messages.asReversed() }

    LaunchedEffect(messages.size) {
        listState.animateScrollToItem(0)
    }

    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
    ) {
        items(reversed, key = { it.id }) { message ->
            if (message.fromUser) UserBubble(message) else ModelBubble(message)
        }
    }
}

@Composable
private fun UserBubble(message: ChatMessage) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            message.text,
            color = CardWhite,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 6.dp, bottomStart = 22.dp, bottomEnd = 22.dp))
                .background(InkGradient)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun ModelBubble(message: ChatMessage) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(topStart = 6.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 22.dp), ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 22.dp))
            .background(CardWhite)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StarMascot(Modifier.size(22.dp), animated = false)
            Spacer(Modifier.width(6.dp))
            Text("Gemma", style = MaterialTheme.typography.labelMedium, color = Ink)
        }
        Spacer(Modifier.height(8.dp))

        when {
            message.isStreaming && message.text.isEmpty() -> TypingDots()
            message.isError -> Text(message.text, color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
            else -> MarkdownText(message.text, color = Ink)
        }

        if (!message.isStreaming && !message.isError && message.text.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .clip(CircleShape)
                    .clickable {
                        scope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Gemma reply", message.text)))
                        }
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(AppIcons.Copy, contentDescription = null, tint = TextFaint, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Copy", style = MaterialTheme.typography.bodySmall, color = TextFaint)
            }
        }
    }
}

@Composable
private fun TypingDots() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(vertical = 6.dp)) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, delayMillis = index * 150),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$index",
            )
            Box(
                Modifier
                    .size(8.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(Ink)
            )
        }
    }
}

@Composable
private fun QuickActionRow(enabled: Boolean, onAction: (QuickAction) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        quickActions.forEach { action ->
            ActionChip(
                text = action.label,
                icon = action.icon,
                onClick = { if (enabled) onAction(action) },
            )
        }
    }
}

@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    isGenerating: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onMic: () -> Unit,
    onOpenVoice: () -> Unit,
) {
    val canSend = enabled && !isGenerating && value.isNotBlank()

    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp)
            .shadow(8.dp, RoundedCornerShape(30.dp), ambientColor = Color(0x1F000000), spotColor = Color(0x1F000000))
            .clip(RoundedCornerShape(30.dp))
            .background(CardWhite)
            .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(enabled = enabled, onClick = onMic),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Mic, contentDescription = "Voice input", tint = TextMuted, modifier = Modifier.size(22.dp))
        }

        Box(
            Modifier
                .weight(1f)
                .heightIn(min = 44.dp)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    if (enabled) "Ask anything about ….." else "Waiting for the model…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextFaint,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
                cursorBrush = SolidColor(Ink),
                maxLines = 5,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send,
                ),
                keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
            )
        }

        val action = when {
            isGenerating -> TrailingAction.Stop
            value.isNotBlank() -> TrailingAction.Send
            else -> TrailingAction.Voice
        }
        val actionEnabled = when (action) {
            TrailingAction.Stop -> true
            TrailingAction.Send -> canSend
            TrailingAction.Voice -> true
        }
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Ink)
                .clickable(enabled = actionEnabled) {
                    when (action) {
                        TrailingAction.Stop -> onStop()
                        TrailingAction.Send -> onSend()
                        TrailingAction.Voice -> onOpenVoice()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = action,
                transitionSpec = { (fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.7f)) togetherWith fadeOut(tween(100)) },
                label = "trailingAction",
            ) { target ->
                when (target) {
                    TrailingAction.Stop -> Icon(
                        AppIcons.Stop,
                        contentDescription = "Stop",
                        tint = CardWhite,
                        modifier = Modifier.size(20.dp),
                    )
                    TrailingAction.Send -> Icon(
                        AppIcons.Send,
                        contentDescription = "Send",
                        tint = CardWhite,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(-35f),
                    )
                    TrailingAction.Voice -> Icon(
                        AppIcons.Waveform,
                        contentDescription = "Voice chat",
                        tint = CardWhite,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

private enum class TrailingAction { Send, Stop, Voice }

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ChatPreview() {
    OfflineChatBotTheme {
        ChatScreen(
            state = ChatUiState(
                modelName = "Gemma 4 E2B",
                engineStatus = EngineStatus.Ready,
                messages = listOf(
                    ChatMessage(0, "What makes Gemma run offline?", fromUser = true),
                    ChatMessage(
                        1,
                        "Gemma runs **entirely on your phone** using LiteRT-LM.\n\n- No internet needed\n- Your data stays private",
                        fromUser = false,
                    ),
                ),
            ),
            onSend = {}, onStop = {}, onNewChat = {}, onRetryLoad = {},
            onStopModel = {}, onStartModel = {}, onManageModel = {},
        )
    }
}
