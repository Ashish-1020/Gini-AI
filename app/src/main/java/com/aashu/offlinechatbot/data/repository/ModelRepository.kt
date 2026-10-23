package com.aashu.offlinechatbot.data.repository

import com.aashu.offlinechatbot.data.model.ModelDownloadState
import com.aashu.offlinechatbot.data.model.ModelInfo
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ModelRepository {
    val model: ModelInfo

    val modelFile: File

    fun isModelReady(): Boolean

    fun downloadState(): Flow<ModelDownloadState>

    suspend fun startDownload()

    suspend fun cancelDownload()

    suspend fun deleteModel()
}

class InsufficientStorageException(val requiredBytes: Long, val availableBytes: Long) :
    Exception("Not enough free space for the model")
