package com.example.localllmchat.model.inference

import kotlinx.coroutines.flow.Flow

interface ModelRunner {
    suspend fun loadModel(path: String, contextLength: Int, gpuLayers: Int = 0): Boolean
    fun generateStream(
        systemPrompt: String?,
        history: List<Pair<String,String>>, // (role, content)
        userMessage: String,
        temperature: Float = 0.7f,
        topP: Float = 0.9f,
        maxTokens: Int = 512
    ): Flow<String>

    fun unload()
}

