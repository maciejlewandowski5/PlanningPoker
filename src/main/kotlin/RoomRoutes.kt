package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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
