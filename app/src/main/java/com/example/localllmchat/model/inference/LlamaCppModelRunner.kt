package com.example.localllmchat.model.inference

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

class LlamaCppModelRunner : ModelRunner {

    external fun nativeInit(path: String, ctx: Int, gpuLayers: Int): Boolean
    external fun nativeGenerateToken(prompt: String): String?
    external fun nativeRelease()

    private var loaded = false

    override suspend fun loadModel(path: String, contextLength: Int, gpuLayers: Int): Boolean {
        loaded = nativeInit(path, contextLength, gpuLayers)
        return loaded
    }

    override fun generateStream(
        systemPrompt: String?,
        history: List<Pair<String, String>>, // (role, content)
        userMessage: String,
        temperature: Float,
        topP: Float,
        maxTokens: Int
    ): Flow<String> = callbackFlow {
        if (!loaded) {
            close(IllegalStateException("Model not loaded"))
            return@callbackFlow
        }
        val sb = StringBuilder()
        systemPrompt?.let { sb.append("<<SYS>>\n").append(it).append("\n<</SYS>>\n") }
        history.forEach { (role, content) ->
            sb.append(role).append(": ").append(content).append("\n")
        }
        sb.append("user: ").append(userMessage).append("\nassistant:")

        var tokens = 0
        try {
            while (tokens < maxTokens) {
                val token = nativeGenerateToken(sb.toString()) ?: break
                if (token.isEmpty()) break
                trySend(token)
                tokens += 1
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
        awaitClose { }
    }

    override fun unload() {
        if (loaded) {
            nativeRelease()
            loaded = false
        }
    }

    companion object {
        init {
            System.loadLibrary("llama_jni")
        }
    }
}
