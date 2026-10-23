package com.aashu.offlinechatbot.data.voice

fun stripMarkdown(text: String): String = text
    .replace(Regex("```[\\s\\S]*?```"), " ")
    .replace(Regex("(?m)^\\s*#{1,6}\\s*"), "")
    .replace(Regex("(?m)^\\s*[-*•]\\s+"), "")
    .replace(Regex("\\[(.*?)]\\((.*?)\\)"), "$1")
    .replace(Regex("[*_`#>]"), "")
    .trim()

fun lastSentenceEnd(text: String, from: Int): Int {
    var end = from
    for (i in from until text.length - 1) {
        val c = text[i]
        if ((c == '.' || c == '!' || c == '?' || c == '\n') && text[i + 1].isWhitespace()) end = i + 1
    }
    return end
}
