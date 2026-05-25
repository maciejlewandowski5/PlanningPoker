package com.example.poker.api

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.fetch.RequestInit

private val json = Json { ignoreUnknownKeys = true }

private fun baseUrl(): String {
    val loc = window.location
    return "${loc.protocol}//${loc.host}"
}

private suspend fun fetchJson(url: String, method: String, body: String? = null): String {
    val opts = RequestInit(
        method = method,
        headers = if (body != null) {
            js("({'Content-Type': 'application/json'})")
        } else {
            js("({})")
        }
    )
    if (body != null) opts.body = body
    val response = window.fetch("${baseUrl()}$url", opts).await()
    if (!response.ok) error("HTTP ${response.status}: ${response.statusText}")
    return response.text().await()
}

suspend fun createRoom(): CreateRoomResponse {
    val text = fetchJson("/rooms", "POST")
    return json.decodeFromString(text)
}

suspend fun joinRoom(code: String, displayName: String): JoinRoomResponse {
    val body = json.encodeToString(JoinRoomRequest(displayName))
    val text = fetchJson("/rooms/$code/join", "POST", body)
    return json.decodeFromString(text)
}
