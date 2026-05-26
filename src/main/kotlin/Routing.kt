package com.example

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val repo = RoomRepository()
    val registry = SessionRegistry()
    val service = RoomService(repo, registry)
    routing {
        staticResources("/", "static") {
            default("index.html")
        }
        roomRoutes(repo, service)
        sseRoutes(repo, service, registry)
    }
}
