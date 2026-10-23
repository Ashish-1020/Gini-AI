package com.aashu.offlinechatbot.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aashu.offlinechatbot.ui.theme.Cream

@Composable
fun MarkdownText(text: String, color: Color, modifier: Modifier = Modifier) {
    val blocks = remember(text) { parseBlocks(text) }
    val body = MaterialTheme.typography.bodyMedium

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is Block.Heading -> Text(
                    inline(block.text),
                    color = color,
                    style = body.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = when (block.level) { 1 -> 20.sp; 2 -> 18.sp; else -> 16.sp },
                    ),
                )

                is Block.Bullet -> Row {
                    Text(block.marker, color = color, style = body, modifier = Modifier.width(22.dp))
                    Text(inline(block.text), color = color, style = body)
                }

                is Block.Code -> Text(
                    block.code,
                    color = color,
                    style = body.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    softWrap = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Cream)
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp),
                )

                is Block.Paragraph -> Text(inline(block.text), color = color, style = body)
            }
        }
    }
}

private sealed interface Block {
    data class Heading(val level: Int, val text: String) : Block
    data class Bullet(val marker: String, val text: String) : Block
    data class Code(val code: String) : Block
    data class Paragraph(val text: String) : Block
}

private val numbered = Regex("""^(\d+)[.)]\s+(.*)""")

private fun parseBlocks(text: String): List<Block> {
    val blocks = mutableListOf<Block>()
    val paragraph = StringBuilder()
    val code = StringBuilder()
    var inCode = false

    fun flushParagraph() {
        if (paragraph.isNotBlank()) blocks += Block.Paragraph(paragraph.toString().trim())
        paragraph.clear()
    }

    for (line in text.lines()) {
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            if (inCode) {
                blocks += Block.Code(code.toString().trimEnd())
                code.clear()
            } else {
                flushParagraph()
            }
            inCode = !inCode
            continue
        }
        if (inCode) {
            code.appendLine(line)
            continue
        }
        when {
            trimmed.isEmpty() -> flushParagraph()
            trimmed.startsWith("#") -> {
                flushParagraph()
                val level = trimmed.takeWhile { it == '#' }.length
                blocks += Block.Heading(level, trimmed.drop(level).trim())
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                flushParagraph()
                blocks += Block.Bullet("•", trimmed.drop(2).trim())
            }
            numbered.matches(trimmed) -> {
                flushParagraph()
                val match = numbered.find(trimmed)!!
                blocks += Block.Bullet("${match.groupValues[1]}.", match.groupValues[2])
            }
            else -> {
                if (paragraph.isNotEmpty()) paragraph.append('\n')
                paragraph.append(trimmed)
            }
        }
    }
    if (inCode && code.isNotEmpty()) blocks += Block.Code(code.toString().trimEnd())
    flushParagraph()
    return blocks
}

private fun inline(text: String): AnnotatedString = buildAnnotatedString {
    var bold = false
    var italic = false
    var code = false
    var i = 0
    val run = StringBuilder()

    fun flush() {
        if (run.isEmpty()) return
        withStyle(
            SpanStyle(
                fontWeight = if (bold) FontWeight.Bold else null,
                fontStyle = if (italic) FontStyle.Italic else null,
                fontFamily = if (code) FontFamily.Monospace else null,
                background = if (code) Cream else Color.Unspecified,
            )
        ) { append(run.toString()) }
        run.clear()
    }

    while (i < text.length) {
        val c = text[i]
        val opensItalic = c == '*' && i + 1 < text.length && !text[i + 1].isWhitespace()
        when {
            c == '`' -> { flush(); code = !code; i++ }
            !code && text.startsWith("**", i) -> { flush(); bold = !bold; i += 2 }
            !code && c == '*' && (italic || opensItalic) -> { flush(); italic = !italic; i++ }
            else -> { run.append(c); i++ }
        }
    }
    flush()
}
