package com.example.localllmchat.repository

import com.example.localllmchat.data.ChatDao
import com.example.localllmchat.data.ChatMessageEntity
import com.example.localllmchat.data.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val dao: ChatDao) {
    fun sessions(): Flow<List<ChatSessionEntity>> = dao.observeSessions()
    suspend fun createSession(modelId: String, title: String): Long =
        dao.insertSession(ChatSessionEntity(modelId = modelId, title = title))
    fun messages(sessionId: Long): Flow<List<ChatMessageEntity>> = dao.observeMessages(sessionId)
    suspend fun appendMessage(sessionId: Long, role: String, content: String) {
        dao.insertMessage(ChatMessageEntity(sessionId = sessionId, role = role, content = content))
    }
    suspend fun getSession(id: Long) = dao.getSession(id)
}
