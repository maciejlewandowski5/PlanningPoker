package com.example

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomService(
    private val repo: RoomRepository,
    private val registry: SessionRegistry
) {
    suspend fun createRoom(): Pair<String, String> = repo.createRoom()

    suspend fun joinRoom(roomId: String, displayName: String): String =
        repo.addParticipant(roomId, displayName)

    suspend fun vote(roomId: String, participantId: String, value: String) {
        repo.upsertVote(participantId, value)
        broadcast(roomId)
    }

    suspend fun reveal(roomId: String) {
        repo.setRevealed(roomId, true)
        broadcast(roomId)
    }

    suspend fun hide(roomId: String) {
        repo.setRevealed(roomId, false)
        broadcast(roomId)
    }

    suspend fun reset(roomId: String) {
        repo.resetRound(roomId)
        broadcast(roomId)
    }

    suspend fun broadcastState(roomId: String) = broadcast(roomId)

    private suspend fun broadcast(roomId: String) {
        val state = repo.getRoomState(roomId) ?: return
        registry.broadcast(roomId, Json.encodeToString(state))
    }
}
