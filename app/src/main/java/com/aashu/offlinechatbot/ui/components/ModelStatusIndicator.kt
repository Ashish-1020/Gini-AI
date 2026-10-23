package com.aashu.offlinechatbot.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aashu.offlinechatbot.data.llm.EngineRunState
import com.aashu.offlinechatbot.ui.theme.SuccessGreen
import com.aashu.offlinechatbot.ui.theme.TextFaint
import com.aashu.offlinechatbot.ui.theme.TextMuted

private val Amber = Color(0xFFD69A2D)

fun EngineRunState.label(): String = when (this) {
    EngineRunState.STOPPED -> "Stopped · not using memory"
    EngineRunState.LOADING -> "Starting…"
    EngineRunState.RUNNING -> "Running in memory"
    EngineRunState.GENERATING -> "Running · generating reply"
}

@Composable
fun ModelStatusIndicator(
    runState: EngineRunState,
    modifier: Modifier = Modifier,
    label: String = runState.label(),
) {
    val color = when (runState) {
        EngineRunState.STOPPED -> TextFaint
        EngineRunState.LOADING -> Amber
        EngineRunState.RUNNING, EngineRunState.GENERATING -> SuccessGreen
    }
    val busy = runState == EngineRunState.LOADING || runState == EngineRunState.GENERATING
    val pulse = if (busy) {
        rememberInfiniteTransition(label = "status").animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "pulse",
        ).value
    } else {
        1f
    }

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .alpha(pulse)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
