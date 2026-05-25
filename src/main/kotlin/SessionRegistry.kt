package com.example

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

class SessionRegistry {
    private val sessions = ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketServerSession>>()

    fun add(roomId: String, session: WebSocketServerSession) {
        sessions.getOrPut(roomId) { CopyOnWriteArraySet() }.add(session)
    }

    fun remove(roomId: String, session: WebSocketServerSession) {
        sessions[roomId]?.remove(session)
    }

    suspend fun broadcast(roomId: String, message: String) {
        val roomSessions = sessions[roomId] ?: return
        val dead = mutableListOf<WebSocketServerSession>()
        for (session in roomSessions) {
            try {
                session.send(Frame.Text(message))
            } catch (e: Exception) {
                dead.add(session)
            }
        }
        roomSessions.removeAll(dead.toSet())
    }
}
