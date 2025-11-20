package com.example.localllmchat.model

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File

sealed class ModelDownloadState {
    data object Idle : ModelDownloadState()
    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : ModelDownloadState()
    data class Ready(val path: String) : ModelDownloadState()
    data class Error(val message: String) : ModelDownloadState()
}

class ModelManager(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient()
) {
    private val stateFlows = mutableMapOf<String, MutableStateFlow<ModelDownloadState>>()

    fun observe(model: LocalModel): Flow<ModelDownloadState> =
        stateFlows.getOrPut(model.id) { MutableStateFlow(ModelDownloadState.Idle) }

    fun getModelFile(model: LocalModel): File {
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, model.fileName)
    }

    fun isDownloaded(model: LocalModel): Boolean = getModelFile(model).exists()

    suspend fun ensureDownloaded(model: LocalModel) : Result<File> = withContext(Dispatchers.IO) {
        val flow = stateFlows.getOrPut(model.id) { MutableStateFlow(ModelDownloadState.Idle) }
        val outFile = getModelFile(model)
        if (outFile.exists() && outFile.length() > 0) {
            flow.value = ModelDownloadState.Ready(outFile.absolutePath)
            return@withContext Result.success(outFile)
        }

        flow.value = ModelDownloadState.Downloading(0L, model.sizeBytes)
        val req = Request.Builder().url(model.remoteUrl).build()
        return@withContext try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    flow.value = ModelDownloadState.Error("HTTP ${resp.code}")
                    return@use
                }
                resp.body?.let { body ->
                    outFile.outputStream().buffered().use { sink ->
                        var read: Long = 0
                        val buf = ByteArray(64 * 1024)
                        val source = body.byteStream()
                        val total = body.contentLength().takeIf { it > 0 } ?: model.sizeBytes
                        while (true) {
                            val n = source.read(buf)
                            if (n < 0) break
                            sink.write(buf, 0, n)
                            read += n
                            flow.value = ModelDownloadState.Downloading(read, total)
                        }
                    }
                    flow.value = ModelDownloadState.Ready(outFile.absolutePath)
                } ?: run {
                    flow.value = ModelDownloadState.Error("Empty body")
                }
            }
            Result.success(outFile)
        } catch (e: Exception) {
            Timber.e(e)
            flow.value = ModelDownloadState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }
}
