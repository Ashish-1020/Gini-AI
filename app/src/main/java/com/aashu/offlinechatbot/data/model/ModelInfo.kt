package com.aashu.offlinechatbot.data.model

data class ModelInfo(
    val id: String,
    val displayName: String,
    val publisher: String,
    val description: String,
    val fileName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
)

object ModelCatalog {
    val GEMMA_4_E2B = ModelInfo(
        id = "gemma-4-e2b-it",
        displayName = "Gemma 4 E2B",
        publisher = "Google",
        description = "Fast, capable chat model that runs entirely on your phone.",
        fileName = "gemma-4-E2B-it.litertlm",
        downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
        sizeBytes = 2_588_147_712L,
    )

    val default: ModelInfo = GEMMA_4_E2B
}
