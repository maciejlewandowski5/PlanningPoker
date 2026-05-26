package com.example.poker.api

import kotlinx.serialization.Serializable

@Serializable
data class CreateRoomRequest(val votingScale: String)

@Serializable
data class CreateRoomResponse(val roomId: String, val code: String)

@Serializable
data class JoinRoomRequest(val displayName: String)

@Serializable
data class JoinRoomResponse(val participantId: String, val roomId: String, val code: String)

@Serializable
data class ParticipantState(
    val participantId: String,
    val displayName: String,
    val hasVoted: Boolean,
    val vote: String?
)

@Serializable
data class RoomState(
    val type: String = "state",
    val roomId: String,
    val code: String,
    val votesRevealed: Boolean,
    val participants: List<ParticipantState>,
    val votingScale: String = "0,0.25,0.5,1,1.5,2,3,4,5,6,7,10,100,?"
)
