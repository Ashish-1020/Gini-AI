package com.aashu.offlinechatbot.ui.components

import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashu.offlinechatbot.ui.theme.Blush
import com.aashu.offlinechatbot.ui.theme.CircuitBlue
import com.aashu.offlinechatbot.ui.theme.MascotFill
import com.aashu.offlinechatbot.ui.theme.MascotStroke
import com.aashu.offlinechatbot.ui.theme.TextMuted
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val RIBBON_TEXT = "Gemma AI  •  Offline Chat  •  "

@Composable
fun OnboardingIllustration(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "illustration")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "bob",
    )
    val scroll by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "scroll",
    )
    val textPaint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            color = TextMuted.toArgb()
            letterSpacing = 0.06f
        }
    }

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        drawOpenBook(center = Offset(w * 0.47f, h * 0.65f), width = w * 0.62f)

        val ribbon = Path().apply {
            moveTo(-w * 0.08f, h * 0.50f)
            cubicTo(w * 0.28f, h * 0.86f, w * 0.68f, h * 0.60f, w * 1.08f, h * 0.56f)
        }
        drawPath(
            ribbon,
            color = MascotStroke.copy(alpha = 0.18f),
            style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round),
        )
        drawPath(
            ribbon,
            color = Color.White.copy(alpha = 0.92f),
            style = Stroke(width = 26.dp.toPx(), cap = StrokeCap.Round),
        )

        textPaint.textSize = 13.sp.toPx()
        val unit = textPaint.measureText(RIBBON_TEXT)
        val length = PathMeasure().apply { setPath(ribbon, false) }.length
        val repeated = RIBBON_TEXT.repeat((length / unit).toInt() + 2)
        val baselineShift = textPaint.textSize * 0.35f
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawTextOnPath(
                repeated,
                ribbon.asAndroidPath(),
                -scroll * unit,
                baselineShift,
                textPaint,
            )
        }

        val starRadius = w * 0.22f
        val floatY = (bob - 0.5f) * 10.dp.toPx()
        drawStarMascot(center = Offset(w * 0.5f, h * 0.30f + floatY), radius = starRadius, tilt = -6f)
    }
}

@Composable
fun StarMascot(modifier: Modifier = Modifier, animated: Boolean = true) {
    val bob = if (animated) {
        val transition = rememberInfiniteTransition(label = "star")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2400), RepeatMode.Reverse),
            label = "bob",
        ).value
    } else {
        0.5f
    }
    Canvas(modifier) {
        val radius = size.minDimension * 0.46f
        val floatY = (bob - 0.5f) * size.minDimension * 0.05f
        drawStarMascot(center = Offset(size.width / 2, size.height / 2 + floatY), radius = radius, tilt = -6f)
    }
}

private fun DrawScope.drawStarMascot(center: Offset, radius: Float, tilt: Float) {
    val strokeWidth = (radius * 0.035f).coerceAtLeast(1.5f)
    rotate(tilt, pivot = center) {
        val star = roundedStarPath(center, outer = radius, inner = radius * 0.55f)
        drawPath(star, MascotFill)
        drawPath(star, MascotStroke, style = Stroke(width = strokeWidth, join = StrokeJoin.Round))

        val circuitStroke = Stroke(width = strokeWidth * 0.9f, cap = StrokeCap.Round)
        val cx = center.x - radius * 0.02f
        val cy = center.y - radius * 0.42f
        for (i in 0..2) {
            val y = cy + i * radius * 0.08f
            val start = Offset(cx + radius * 0.05f, y)
            val end = Offset(cx + radius * (0.22f + i * 0.06f), y - radius * 0.04f)
            drawLine(CircuitBlue, start, end, strokeWidth = circuitStroke.width, cap = StrokeCap.Round)
            drawCircle(CircuitBlue, radius = strokeWidth * 1.3f, center = end)
        }

        val eyeY = center.y + radius * 0.02f
        val eyeDx = radius * 0.17f
        val eyeSize = Size(radius * 0.11f, radius * 0.16f)
        for (sign in listOf(-1f, 1f)) {
            val eyeCenter = Offset(center.x + sign * eyeDx, eyeY)
            drawOval(
                color = Color(0xFF5B6878),
                topLeft = eyeCenter - Offset(eyeSize.width / 2, eyeSize.height / 2),
                size = eyeSize,
            )
            drawCircle(
                Color.White,
                radius = eyeSize.width * 0.22f,
                center = eyeCenter + Offset(eyeSize.width * 0.15f, -eyeSize.height * 0.2f),
            )
            drawOval(
                color = Blush.copy(alpha = 0.75f),
                topLeft = Offset(center.x + sign * radius * 0.30f - radius * 0.07f, eyeY + radius * 0.12f),
                size = Size(radius * 0.14f, radius * 0.07f),
            )
        }
        val smile = Path().apply {
            moveTo(center.x - radius * 0.06f, eyeY + radius * 0.14f)
            quadraticTo(center.x, eyeY + radius * 0.21f, center.x + radius * 0.06f, eyeY + radius * 0.14f)
        }
        drawPath(smile, Color(0xFF5B6878), style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
    }
}

private fun roundedStarPath(center: Offset, outer: Float, inner: Float): Path {
    val vertices = List(10) { i ->
        val r = if (i % 2 == 0) outer else inner
        val angle = -PI / 2 + i * PI / 5
        Offset(center.x + (r * cos(angle)).toFloat(), center.y + (r * sin(angle)).toFloat())
    }
    fun lerp(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

    return Path().apply {
        vertices.forEachIndexed { i, v ->
            val prev = vertices[(i + 9) % 10]
            val next = vertices[(i + 1) % 10]
            val k = if (i % 2 == 0) 0.30f else 0.22f
            val a = lerp(v, prev, k)
            val b = lerp(v, next, k)
            if (i == 0) moveTo(a.x, a.y) else lineTo(a.x, a.y)
            quadraticTo(v.x, v.y, b.x, b.y)
        }
        close()
    }
}

private fun DrawScope.drawOpenBook(center: Offset, width: Float) {
    val pageW = width / 2
    val pageH = width * 0.56f
    val stroke = Stroke(width = 1.4.dp.toPx(), join = StrokeJoin.Round)
    val lineColor = MascotStroke.copy(alpha = 0.45f)

    rotate(-10f, pivot = center) {
        translate(center.x, center.y - pageH / 2) {
            for (sign in listOf(-1f, 1f)) {
                val page = Path().apply {
                    moveTo(0f, pageH * 0.08f)
                    quadraticTo(sign * pageW * 0.5f, -pageH * 0.10f, sign * pageW, pageH * 0.02f)
                    lineTo(sign * pageW, pageH)
                    quadraticTo(sign * pageW * 0.5f, pageH * 0.86f, 0f, pageH * 1.04f)
                    close()
                }
                drawPath(page, Color.White)
                drawPath(page, MascotStroke, style = stroke)
                for (i in 0..4) {
                    val y = pageH * (0.28f + i * 0.13f)
                    val startX = sign * pageW * 0.14f
                    val endX = sign * pageW * (if (i == 4) 0.55f else 0.84f)
                    drawLine(lineColor, Offset(startX, y), Offset(endX, y - pageH * 0.02f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                }
            }
            val tagSize = Size(pageW * 0.30f, pageH * 0.18f)
            val tagTopLeft = Offset(-pageW * 0.80f, pageH * 0.04f)
            drawRoundRect(
                color = MascotStroke,
                topLeft = tagTopLeft,
                size = tagSize,
                cornerRadius = CornerRadius(tagSize.height * 0.25f),
            )
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.WHITE
                    textSize = tagSize.height * 0.62f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                canvas.nativeCanvas.drawText(
                    "AI",
                    tagTopLeft.x + tagSize.width / 2,
                    tagTopLeft.y + tagSize.height * 0.72f,
                    paint,
                )
            }
        }
    }
}
