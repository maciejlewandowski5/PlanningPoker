package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.sse.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val sseJson = Json { encodeDefaults = true }

fun Route.roomRoutes(repo: RoomRepository, service: RoomService) {
    post("/rooms") {
        val body = try { call.receive<CreateRoomRequest>() } catch (_: Exception) { CreateRoomRequest() }
        val (roomId, code) = service.createRoom(body.votingScale)
        call.respond(HttpStatusCode.Created, CreateRoomResponse(roomId, code))
    }

    post("/rooms/{code}/join") {
        val code = call.parameters["code"]!!
        val body = call.receive<JoinRoomRequest>()
        val room = repo.findRoomByCode(code)
            ?: return@post call.respond(HttpStatusCode.NotFound, "Room not found")
        val roomId = room[Rooms.id]
        val participantId = service.joinRoom(roomId, body.displayName)
        call.respond(JoinRoomResponse(participantId, roomId, code))
    }
}

fun Route.sseRoutes(repo: RoomRepository, service: RoomService, registry: SessionRegistry) {
    post("/rooms/{code}/action") {
        val code = call.parameters["code"]!!
        val participantId = call.request.queryParameters["participantId"]
            ?: return@post call.respond(HttpStatusCode.BadRequest, "participantId required")
        val body = call.receive<ActionRequest>()
        val room = repo.findRoomByCode(code)
            ?: return@post call.respond(HttpStatusCode.NotFound, "Room not found")
        val roomId = room[Rooms.id]
        when (body.type) {
            "vote"   -> service.vote(roomId, participantId, body.value ?: return@post call.respond(HttpStatusCode.BadRequest, "value required"))
            "reveal" -> service.reveal(roomId)
            "hide"   -> service.hide(roomId)
            "reset"  -> service.reset(roomId)
            else     -> return@post call.respond(HttpStatusCode.BadRequest, "Unknown action: ${body.type}")
        }
        call.respond(HttpStatusCode.NoContent)
    }

    sse("/rooms/{code}/events") {
        // Commit the SSE response immediately so early returns produce a clean
        // stream close rather than a 404 (Ktor defers sending headers until the
        // first send() call; without this the routing pipeline falls through).
        send(ServerSentEvent(retry = 3000L))

        val code = call.parameters["code"]!!
        val participantId = call.request.queryParameters["participantId"] ?: return@sse
        val room = repo.findRoomByCode(code) ?: return@sse
        val roomId = room[Rooms.id]
        val participant = repo.findParticipant(participantId)
        if (participant == null || participant[Participants.roomId] != roomId) return@sse

        val ch = registry.add(roomId)
        try {
            val state = repo.getRoomState(roomId) ?: return@sse
            send(ServerSentEvent(sseJson.encodeToString(state)))
            for (msg in ch) {
                send(ServerSentEvent(msg))
            }
        } finally {
            registry.remove(roomId, ch)
        }
    }
}
