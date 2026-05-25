package com.example.poker.ws

import com.example.poker.api.RoomState
import kotlinx.browser.window
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket
import org.w3c.dom.events.Event

private val json = Json { ignoreUnknownKeys = true }

sealed class WsEvent {
    data class State(val roomState: RoomState) : WsEvent()
    object Connected : WsEvent()
    object Disconnected : WsEvent()
}

fun roomWebSocketFlow(code: String, participantId: String): Flow<WsEvent> = callbackFlow {
    var ws: WebSocket? = null
    var attempt = 0
    val producer = this

    fun connect() {
        val loc = window.location
        val protocol = if (loc.protocol == "https:") "wss:" else "ws:"
        val url = "$protocol//${loc.host}/rooms/$code/ws?participantId=$participantId"
        ws = WebSocket(url)

        ws!!.onopen = {
            attempt = 0
            trySend(WsEvent.Connected)
        }

        ws!!.onmessage = { event: MessageEvent ->
            val text = event.data.toString()
            try {
                val state = json.decodeFromString<RoomState>(text)
                trySend(WsEvent.State(state))
            } catch (_: Exception) {}
        }

        ws!!.onclose = { _: Event ->
            trySend(WsEvent.Disconnected)
            val delaySec = minOf(1L shl attempt, 30L)
            attempt++
            producer.launch {
                delay(delaySec * 1000)
                if (!producer.isClosedForSend) connect()
            }
        }

        ws!!.onerror = {}
    }

    connect()

    awaitClose {
        ws?.let { if (it.readyState != WebSocket.CLOSED) it.close() }
    }
}

fun WebSocket.sendMessage(type: String, value: String? = null) {
    val msg = if (value != null) """{"type":"$type","value":"$value"}"""
              else """{"type":"$type"}"""
    send(msg)
}
