package com.example

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val repo = RoomRepository()
    val registry = SessionRegistry()
    val service = RoomService(repo, registry)
    routing {
        // API routes must be registered before staticResources. The static
        // default("index.html") fallback is a catch-all GET handler; any route
        // registered after it would never be reached for GET requests.
        roomRoutes(repo, service)
        sseRoutes(repo, service, registry)
        staticResources("/", "static") {
            default("index.html")
        }
    }
}
