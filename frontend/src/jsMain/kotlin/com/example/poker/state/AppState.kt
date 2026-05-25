package com.example.poker.state

import com.example.poker.api.RoomState

sealed class Screen {
    object Home : Screen()
    data class Join(val code: String) : Screen()
    data class Room(val code: String, val participantId: String, val displayName: String) : Screen()
}

data class RoomUiState(
    val roomState: RoomState? = null,
    val connected: Boolean = false,
    val myVote: String? = null
)
