package com.aashu.offlinechatbot.data.repository

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.StatFs
import com.aashu.offlinechatbot.data.model.ModelCatalog
import com.aashu.offlinechatbot.data.model.ModelDownloadState
import com.aashu.offlinechatbot.data.model.ModelInfo
import com.aashu.offlinechatbot.data.prefs.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManagerModelRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloadManager: DownloadManager,
    private val prefs: AppPreferences,
) : ModelRepository {
    override val model: ModelInfo = ModelCatalog.default

    private val modelDir: File
        get() = (context.getExternalFilesDir(MODEL_DIR) ?: File(context.filesDir, MODEL_DIR)).apply { mkdirs() }

    override val modelFile: File
        get() = File(modelDir, model.fileName)

    private val partFile: File
        get() = File(modelDir, model.fileName + PART_SUFFIX)

    override fun isModelReady(): Boolean {
        val file = modelFile
        return file.exists() && file.length() == model.sizeBytes
    }

    override fun downloadState(): Flow<ModelDownloadState> = flow {
        while (true) {
            val state = queryState()
            emit(state)
            if (state is ModelDownloadState.Ready) break
            delay(POLL_INTERVAL_MS)
        }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

    override suspend fun startDownload() = withContext(Dispatchers.IO) {
        if (isModelReady()) return@withContext
        clearActiveDownload()
        partFile.delete()

        val required = model.sizeBytes + STORAGE_HEADROOM_BYTES
        val available = StatFs(modelDir.path).availableBytes
        if (available < required) throw InsufficientStorageException(required, available)

        val request = DownloadManager.Request(Uri.parse(model.downloadUrl))
            .setTitle("${model.displayName} model")
            .setDescription("Downloading for offline chat")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setDestinationInExternalFilesDir(context, MODEL_DIR, partFile.name)

        prefs.activeDownloadId = downloadManager.enqueue(request)
    }

    override suspend fun cancelDownload() = withContext(Dispatchers.IO) {
        clearActiveDownload()
        partFile.delete()
        Unit
    }

    override suspend fun deleteModel() = withContext(Dispatchers.IO) {
        clearActiveDownload()
        partFile.delete()
        modelFile.delete()
        Unit
    }

    private fun clearActiveDownload() {
        val id = prefs.activeDownloadId
        if (id != NO_DOWNLOAD) downloadManager.remove(id)
        prefs.activeDownloadId = NO_DOWNLOAD
    }

    private fun queryState(): ModelDownloadState {
        if (isModelReady()) return ModelDownloadState.Ready

        val id = prefs.activeDownloadId
        if (id == NO_DOWNLOAD) return ModelDownloadState.NotDownloaded

        downloadManager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
            if (cursor == null || !cursor.moveToFirst()) {
                prefs.activeDownloadId = NO_DOWNLOAD
                return ModelDownloadState.NotDownloaded
            }
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                .takeIf { it > 0 } ?: model.sizeBytes

            return when (status) {
                DownloadManager.STATUS_PENDING,
                DownloadManager.STATUS_RUNNING -> ModelDownloadState.Downloading(downloaded, total)

                DownloadManager.STATUS_PAUSED -> ModelDownloadState.Paused(pausedReason(reason), downloaded, total)

                DownloadManager.STATUS_SUCCESSFUL -> finishDownload()

                else -> {
                    prefs.activeDownloadId = NO_DOWNLOAD
                    downloadManager.remove(id)
                    ModelDownloadState.Failed(failedReason(reason))
                }
            }
        }
    }

    private fun finishDownload(): ModelDownloadState {
        val part = partFile
        val target = modelFile
        if (part.exists() && part.length() == model.sizeBytes) {
            target.delete()
            if (!part.renameTo(target)) {
                return ModelDownloadState.Failed("Couldn't save the model file")
            }
        }
        downloadManager.remove(prefs.activeDownloadId)
        prefs.activeDownloadId = NO_DOWNLOAD

        return if (isModelReady()) {
            ModelDownloadState.Ready
        } else {
            part.delete()
            ModelDownloadState.Failed("Download was incomplete. Please try again.")
        }
    }

    private fun pausedReason(reason: Int) = when (reason) {
        DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "Waiting for network…"
        DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "Waiting for Wi-Fi…"
        DownloadManager.PAUSED_WAITING_TO_RETRY -> "Connection lost, retrying…"
        else -> "Paused"
    }

    private fun failedReason(reason: Int) = when (reason) {
        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Not enough storage space"
        DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Storage not available"
        DownloadManager.ERROR_CANNOT_RESUME -> "Download couldn't resume. Please try again."
        DownloadManager.ERROR_HTTP_DATA_ERROR,
        DownloadManager.ERROR_TOO_MANY_REDIRECTS,
        DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Server error. Please try again."
        else -> "Download failed (code $reason)"
    }

    private companion object {
        const val MODEL_DIR = "models"
        const val PART_SUFFIX = ".part"
        const val NO_DOWNLOAD = -1L
        const val POLL_INTERVAL_MS = 500L
        const val STORAGE_HEADROOM_BYTES = 300L * 1024 * 1024
    }
}
