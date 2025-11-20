package com.example.localllmchat.model

data class LocalModel(
    val id: String,
    val displayName: String,
    val sizeBytes: Long,
    val remoteUrl: String,
    val fileName: String,
    val contextLength: Int = 2048,
    val gpuLayers: Int = 0
)

object ModelsCatalog {
    // Replace remoteUrl with actual hosted GGUF files
    val Gemma270M = LocalModel(
        id = "gemma-270m",
        displayName = "Gemma 270M (Q4)",
        sizeBytes = 400_000_000,
        remoteUrl = "https://your-host/gemma-270m-q4.gguf",
        fileName = "gemma-270m-q4.gguf",
        contextLength = 2048
    )
    val Gemma1B = LocalModel(
        id = "gemma-1b",
        displayName = "Gemma 1B (Q4)",
        sizeBytes = 1_400_000_000,
        remoteUrl = "https://your-host/gemma-1b-q4.gguf",
        fileName = "gemma-1b-q4.gguf",
        contextLength = 2048
    )

    val all = listOf(Gemma270M, Gemma1B)
}
