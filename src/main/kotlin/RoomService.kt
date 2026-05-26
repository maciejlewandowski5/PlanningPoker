package com.example

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val broadcastJson = Json { encodeDefaults = true }

class RoomService(
    private val repo: RoomRepository,
    private val registry: SessionRegistry
) {
    suspend fun createRoom(votingScale: String): Pair<String, String> = repo.createRoom(votingScale)

    suspend fun joinRoom(roomId: String, displayName: String): String =
        repo.addParticipant(roomId, displayName)

    suspend fun vote(roomId: String, participantId: String, value: String) {
        repo.upsertVote(participantId, value)
        registry.broadcast(roomId, broadcastJson.encodeToString(VotedDelta(participantId = participantId)))
    }

    suspend fun reveal(roomId: String) {
        repo.setRevealed(roomId, true)
        broadcastState(roomId)
    }

    suspend fun hide(roomId: String) {
        repo.setRevealed(roomId, false)
        broadcastState(roomId)
    }

    suspend fun reset(roomId: String) {
        repo.resetRound(roomId)
        broadcastState(roomId)
    }

    suspend fun broadcastState(roomId: String) {
        val state = repo.getRoomState(roomId) ?: return
        registry.broadcast(roomId, broadcastJson.encodeToString(state))
    }
}
