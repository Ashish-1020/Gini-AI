package com.aashu.offlinechatbot.data.model

sealed interface ModelDownloadState {
    data object NotDownloaded : ModelDownloadState

    data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : ModelDownloadState {
        val progress: Float
            get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    }

    data class Paused(
        val reason: String,
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : ModelDownloadState

    data object Ready : ModelDownloadState

    data class Failed(val message: String) : ModelDownloadState
}
