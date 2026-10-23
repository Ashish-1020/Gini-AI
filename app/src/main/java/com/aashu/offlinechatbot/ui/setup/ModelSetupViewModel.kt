package com.aashu.offlinechatbot.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aashu.offlinechatbot.data.llm.EngineRunState
import com.aashu.offlinechatbot.data.llm.LlmEngine
import com.aashu.offlinechatbot.data.model.ModelDownloadState
import com.aashu.offlinechatbot.data.model.ModelInfo
import com.aashu.offlinechatbot.data.repository.InsufficientStorageException
import com.aashu.offlinechatbot.data.repository.ModelRepository
import com.aashu.offlinechatbot.ui.util.formatBytes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelSetupUiState(
    val model: ModelInfo,
    val download: ModelDownloadState,
    val error: String? = null,
    val isStarting: Boolean = false,
    val runState: EngineRunState = EngineRunState.STOPPED,
)

@HiltViewModel
class ModelSetupViewModel @Inject constructor(
    private val repository: ModelRepository,
    private val llmEngine: LlmEngine,
) : ViewModel() {
    private val error = MutableStateFlow<String?>(null)
    private val isStarting = MutableStateFlow(false)

    val uiState: StateFlow<ModelSetupUiState> =
        combine(repository.downloadState(), error, isStarting, llmEngine.runState) { download, error, starting, runState ->
            ModelSetupUiState(repository.model, download, error, starting, runState)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ModelSetupUiState(
                model = repository.model,
                download = if (repository.isModelReady()) ModelDownloadState.Ready else ModelDownloadState.NotDownloaded,
            ),
        )

    fun download() {
        if (isStarting.value) return
        viewModelScope.launch {
            error.value = null
            isStarting.value = true
            try {
                repository.startDownload()
            } catch (e: InsufficientStorageException) {
                error.value = "Not enough space. The model needs ${formatBytes(e.requiredBytes)} free, " +
                    "but only ${formatBytes(e.availableBytes)} is available."
            } catch (e: Exception) {
                error.value = "Couldn't start the download: ${e.message}"
            } finally {
                isStarting.value = false
            }
        }
    }

    fun cancel() {
        viewModelScope.launch { repository.cancelDownload() }
    }

    fun stopModel() {
        llmEngine.close()
    }

    fun deleteModel() {
        viewModelScope.launch {
            llmEngine.close()
            repository.deleteModel()
        }
    }
}
