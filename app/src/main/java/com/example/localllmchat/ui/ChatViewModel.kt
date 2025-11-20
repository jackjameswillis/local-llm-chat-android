package com.example.localllmchat.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.localllmchat.data.ChatDatabase
import com.example.localllmchat.model.LocalModel
import com.example.localllmchat.model.inference.LlamaCppModelRunner
import com.example.localllmchat.repository.ChatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatUiState(
    val sessionId: Long? = null,
    val model: LocalModel? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isLoadingModel: Boolean = false,
    val isGenerating: Boolean = false,
    val error: String? = null
)

class ChatViewModel(app: Application) : AndroidViewModel(app) {
    private val db = ChatDatabase.get(app)
    private val repo = ChatRepository(db.chatDao())
    private val runner = LlamaCppModelRunner()

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state

    fun initNewSession(model: LocalModel, title: String) {
        viewModelScope.launch {
            val id = repo.createSession(model.id, title)
            _state.update { it.copy(sessionId = id, model = model) }
            observeMessages(id)
        }
    }

    fun loadExisting(sessionId: Long, model: LocalModel) {
        _state.update { it.copy(sessionId = sessionId, model = model) }
        observeMessages(sessionId)
    }

    private fun observeMessages(sessionId: Long) {
        viewModelScope.launch {
            repo.messages(sessionId).collect { list ->
                _state.update {
                    it.copy(messages = list.map { m -> ChatMessage(m.role, m.content) })
                }
            }
        }
    }

    fun loadModel(path: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingModel = true) }
            withContext(Dispatchers.IO) {
                runner.loadModel(path, _state.value.model?.contextLength ?: 2048)
            }
            _state.update { it.copy(isLoadingModel = false) }
        }
    }

    fun sendMessage(text: String) {
        val sessionId = _state.value.sessionId ?: return
        viewModelScope.launch {
            repo.appendMessage(sessionId, "user", text)
            generateAssistant(text)
        }
    }

    private fun buildHistory(): List<Pair<String,String>> {
        return _state.value.messages.map { it.role to it.content }
    }

    private fun generateAssistant(userMessage: String) {
        val sessionId = _state.value.sessionId ?: return
        _state.update { it.copy(isGenerating = true, error = null) }
        viewModelScope.launch {
            try {
                val buffer = StringBuilder()
                runner.generateStream(systemPrompt = "You are a helpful assistant.",
                    history = buildHistory(),
                    userMessage = userMessage
                ).collect { token ->
                    buffer.append(token)
                    _state.update { s ->
                        val base = s.messages
                        val lastIsAssistant = base.lastOrNull()?.role == "assistant"
                        val newMessages = if (lastIsAssistant) {
                            base.dropLast(1) + ChatMessage("assistant", buffer.toString())
                        } else {
                            base + ChatMessage("assistant", buffer.toString())
                        }
                        s.copy(messages = newMessages)
                    }
                }
                repo.appendMessage(sessionId, "assistant", buffer.toString())
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            } finally {
                _state.update { it.copy(isGenerating = false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        runner.unload()
    }
}
