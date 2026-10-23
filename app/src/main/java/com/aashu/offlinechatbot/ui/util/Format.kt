package com.aashu.offlinechatbot.ui.util

import java.util.Locale

fun formatBytes(bytes: Long): String {
    val gb = bytes / 1_073_741_824.0
    if (gb >= 1) return String.format(Locale.US, "%.2f GB", gb)
    val mb = bytes / 1_048_576.0
    return String.format(Locale.US, "%.0f MB", mb)
}
