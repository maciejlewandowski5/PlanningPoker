package com.example

import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

fun Route.webSocketRoutes(repo: RoomRepository, service: RoomService, registry: SessionRegistry) {
    webSocket("/rooms/{code}/ws") {
        val code = call.parameters["code"]!!
        val participantId = call.request.queryParameters["participantId"]
        if (participantId == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing participantId"))
            return@webSocket
        }

        val room = repo.findRoomByCode(code)
        if (room == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Room not found"))
            return@webSocket
        }
        val roomId = room[Rooms.id]

        val participant = repo.findParticipant(participantId)
        if (participant == null || participant[Participants.roomId] != roomId) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid participantId"))
            return@webSocket
        }

        registry.add(roomId, this)
        try {
            service.broadcastState(roomId)

            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val text = frame.readText()
                val json = try {
                    Json.parseToJsonElement(text).jsonObject
                } catch (e: Exception) {
                    send(Frame.Text(Json.encodeToString(ErrorMessage(message = "Invalid JSON"))))
                    continue
                }
                when (val type = json["type"]?.jsonPrimitive?.content) {
                    "vote" -> {
                        val value = json["value"]?.jsonPrimitive?.contentOrNull
                        if (value.isNullOrBlank()) {
                            send(Frame.Text(Json.encodeToString(ErrorMessage(message = "Missing value"))))
                        } else {
                            service.vote(roomId, participantId, value)
                        }
                    }
                    "reveal" -> service.reveal(roomId)
                    "hide" -> service.hide(roomId)
                    "reset" -> service.reset(roomId)
                    else -> send(Frame.Text(Json.encodeToString(ErrorMessage(message = "Unknown action: $type"))))
                }
            }
        } finally {
            registry.remove(roomId, this)
        }
    }
}
