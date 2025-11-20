package com.example.localllmchat.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.localllmchat.model.LocalModel
import com.example.localllmchat.model.ModelDownloadState
import com.example.localllmchat.model.ModelManager
import com.example.localllmchat.model.ModelsCatalog
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ModelSelectionUiState(
    val models: List<LocalModel> = emptyList(),
    val downloadStates: Map<String, ModelDownloadState> = emptyMap()
)

class ModelSelectionViewModel(app: Application): AndroidViewModel(app) {

    private val manager = ModelManager(app.applicationContext)
    private val _state = MutableStateFlow(ModelSelectionUiState(models = ModelsCatalog.all))
    val state: StateFlow<ModelSelectionUiState> = _state

    init {
        ModelsCatalog.all.forEach { model ->
            viewModelScope.launch {
                manager.observe(model).collect { ds ->
                    _state.update {
                        it.copy(downloadStates = it.downloadStates + (model.id to ds))
                    }
                }
            }
        }
    }

    fun ensureDownloaded(model: LocalModel, onReady: (String)->Unit) {
        viewModelScope.launch {
            manager.ensureDownloaded(model).onSuccess {
                onReady(it.absolutePath)
            }
        }
    }

    fun isDownloaded(model: LocalModel) = manager.isDownloaded(model)
}
