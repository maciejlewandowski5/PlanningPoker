package com.example.poker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.poker.api.RoomState
import com.example.poker.ui.components.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.EventSource
import org.w3c.dom.MessageEvent
import org.w3c.fetch.RequestInit

private val json = Json { ignoreUnknownKeys = true }

@Composable
fun RoomScreen(
    code: String,
    participantId: String,
    scope: kotlinx.coroutines.CoroutineScope,
    onLeave: () -> Unit
) {
    var roomState by remember { mutableStateOf<RoomState?>(null) }
    var connected by remember { mutableStateOf(false) }
    var myVote by remember { mutableStateOf<String?>(null) }
    var customVote by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }

    // Inject keyframes once for the lifetime of this screen
    DisposableEffect(Unit) {
        val styleEl = document.createElement("style")
        styleEl.asDynamic().textContent = """
            @keyframes pp-scale-in {
                from { opacity: 0; transform: scale(0.96); }
                to   { opacity: 1; transform: scale(1);    }
            }
            @keyframes pp-fade-in-up {
                from { opacity: 0; transform: translateY(8px); }
                to   { opacity: 1; transform: translateY(0px); }
            }
        """.trimIndent()
        document.head?.appendChild(styleEl)
        onDispose { try { document.head?.removeChild(styleEl) } catch (_: Exception) {} }
    }

    // SSE connection — EventSource auto-reconnects on disconnect
    DisposableEffect(code, participantId) {
        val url = "/rooms/$code/events?participantId=$participantId"
        println("[SSE] connecting → $url")
        val es = EventSource(url)
        es.onopen = {
            println("[SSE] connected")
            connected = true
            null
        }
        es.onmessage = { event: MessageEvent ->
            val raw = event.data.toString()
            println("[SSE] message: ${raw.take(120)}")
            try {
                val elem = Json.parseToJsonElement(raw).jsonObject
                when (elem["type"]?.jsonPrimitive?.content) {
                    "state" -> {
                        val state = json.decodeFromString<RoomState>(raw)
                        roomState = state
                        if (state.votesRevealed) {
                            myVote = state.participants.find { it.participantId == participantId }?.vote ?: myVote
                        }
                    }
                    "voted" -> {
                        val pid = elem["participantId"]?.jsonPrimitive?.content
                        if (pid != null) {
                            roomState = roomState?.copy(
                                participants = roomState!!.participants.map {
                                    if (it.participantId == pid) it.copy(hasVoted = true) else it
                                }
                            )
                        }
                    }
                    else -> println("[SSE] unknown message type: ${elem["type"]}")
                }
            } catch (e: Exception) {
                println("[SSE] parse error: $e  raw=$raw")
            }
            null
        }
        es.onerror = {
            println("[SSE] error / disconnected (readyState=${es.readyState})")
            connected = false
            null
        }
        onDispose { es.close() }
    }

    fun send(type: String, value: String? = null) {
        scope.launch {
            try {
                val body = if (value != null)
                    """{"type":"$type","value":"$value"}"""
                else
                    """{"type":"$type"}"""
                window.fetch(
                    "/rooms/$code/action?participantId=$participantId",
                    RequestInit(
                        method = "POST",
                        body = body,
                        headers = js("({'Content-Type':'application/json'})")
                    )
                ).await()
            } catch (_: Exception) {}
        }
    }

    Div({
        style {
            minHeight(100.vh)
            backgroundColor(Color("#f0f2f5"))
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
        }
    }) {
        // Header
        Div({
            style {
                backgroundColor(Color(Colors.surface))
                property("border-bottom", "1px solid ${Colors.border}")
                padding(Spacing.md, Spacing.lg)
                display(DisplayStyle.Flex)
                justifyContent(JustifyContent.SpaceBetween)
                alignItems(AlignItems.Center)
            }
        }) {
            Span({
                style {
                    property("font-weight", "700")
                    fontSize(18.px)
                    color(Color(Colors.textPrimary))
                }
            }) {
                Text("Planning Poker")
            }
            Div({
                style {
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                    gap(Spacing.sm)
                }
            }) {
                if (!connected) {
                    Span({
                        style {
                            fontSize(12.px)
                            color(Color(Colors.warning))
                            property("font-weight", "600")
                        }
                    }) {
                        Text("⚠ Reconnecting…")
                    }
                }
                Span({
                    style {
                        backgroundColor(Color(Colors.surfaceAlt))
                        padding(4.px, 10.px)
                        borderRadius(6.px)
                        fontSize(13.px)
                        property("font-weight", "600")
                        color(Color(Colors.textSecondary))
                        property("letter-spacing", "0.05em")
                    }
                }) { Text("Room: $code") }
                // Copy room code button
                Button({
                    onClick {
                        scope.launch {
                            try {
                                window.navigator.asDynamic().clipboard.writeText(code)
                                copied = true
                                delay(2000)
                                copied = false
                            } catch (_: Exception) {}
                        }
                    }
                    attr("title", if (copied) "Copied!" else "Copy room code")
                    style {
                        background("transparent")
                        property("border", "none")
                        color(Color(if (copied) Colors.success else Colors.textSecondary))
                        cursor("pointer")
                        fontSize(15.px)
                        padding(4.px, 6.px)
                        borderRadius(4.px)
                        property("transition", "color 0.15s ease")
                        property("line-height", "1")
                    }
                }) { Text(if (copied) "✓" else "⎘") }
                Button({
                    onClick { onLeave() }
                    style {
                        background("transparent")
                        property("border", "none")
                        color(Color(Colors.textSecondary))
                        cursor("pointer")
                        fontSize(13.px)
                    }
                }) { Text("Leave") }
            }
        }

        // Content
        Div({
            style {
                flex(1)
                padding(Spacing.lg)
                maxWidth(900.px)
                width(100.percent)
                property("margin", "0 auto")
            }
        }) {
            val state = roomState
            if (state == null) {
                P({
                    style {
                        property("text-align", "center")
                        color(Color(Colors.textSecondary))
                        marginTop(Spacing.xxl)
                    }
                }) { Text("Connecting…") }
            } else {
                // Action bar at the top
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        gap(Spacing.md)
                        marginBottom(Spacing.lg)
                    }
                }) {
                    if (state.votesRevealed) {
                        ActionButton("Hide Votes", Colors.primary) { send("hide") }
                    } else {
                        ActionButton("Reveal Votes", Colors.primary) { send("reveal") }
                    }
                    ActionButton("Reset Round", Colors.danger) {
                        myVote = null
                        customVote = ""
                        send("reset")
                    }
                }

                // Participants / Results
                Section({
                    style {
                        backgroundColor(Color(Colors.surface))
                        borderRadius(12.px)
                        padding(Spacing.lg)
                        property("box-shadow", "0 1px 3px rgba(0,0,0,0.08)")
                        marginBottom(Spacing.lg)
                    }
                }) {
                    P({
                        style {
                            property("font-weight", "600")
                            color(Color(Colors.textSecondary))
                            fontSize(13.px)
                            property("text-transform", "uppercase")
                            property("letter-spacing", "0.05em")
                            marginBottom(Spacing.md)
                        }
                    }) {
                        Text(if (state.votesRevealed) "Results" else "Participants (${state.participants.size})")
                    }

                    // key() forces DOM recreation on state flip → CSS animation replays
                    key(state.votesRevealed) {
                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                flexWrap(FlexWrap.Wrap)
                                gap(Spacing.md)
                                property("animation", "pp-scale-in 0.2s ease-out")
                            }
                        }) {
                            state.participants.forEach { p ->
                                ParticipantCard(
                                    participant = p,
                                    isMe = p.participantId == participantId,
                                    votesRevealed = state.votesRevealed
                                )
                            }
                        }
                    }

                    if (state.votesRevealed) {
                        val numbers = state.participants.mapNotNull { it.vote?.toDoubleOrNull() }
                        if (numbers.isNotEmpty()) {
                            Div({
                                style {
                                    marginTop(Spacing.md)
                                    paddingTop(Spacing.md)
                                    property("border-top", "1px solid ${Colors.border}")
                                    display(DisplayStyle.Flex)
                                    gap(Spacing.lg)
                                    color(Color(Colors.textSecondary))
                                    fontSize(14.px)
                                    property("animation", "pp-fade-in-up 0.18s ease-out")
                                }
                            }) {
                                val avg = numbers.average().asDynamic().toFixed(1) as String
                                Span { B { Text("Avg: ") }; Text(avg) }
                                Span { B { Text("Min: ") }; Text(numbers.min().toInt().toString()) }
                                Span { B { Text("Max: ") }; Text(numbers.max().toInt().toString()) }
                            }
                        }
                    }
                }

                // Voting controls — conditionally rendered, so entrance animation plays on reset
                if (!state.votesRevealed) {
                    val quickVotes = state.votingScale
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }

                    Section({
                        style {
                            backgroundColor(Color(Colors.surface))
                            borderRadius(12.px)
                            padding(Spacing.lg)
                            property("box-shadow", "0 1px 3px rgba(0,0,0,0.08)")
                            property("animation", "pp-fade-in-up 0.2s ease-out")
                        }
                    }) {
                        P({
                            style {
                                property("font-weight", "600")
                                color(Color(Colors.textSecondary))
                                fontSize(13.px)
                                property("text-transform", "uppercase")
                                property("letter-spacing", "0.05em")
                                marginBottom(Spacing.md)
                            }
                        }) { Text("Your vote") }

                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                flexWrap(FlexWrap.Wrap)
                                gap(Spacing.sm)
                                marginBottom(Spacing.md)
                            }
                        }) {
                            quickVotes.forEach { v ->
                                VoteButton(
                                    label = v,
                                    selected = myVote == v,
                                    onClick = {
                                        myVote = v
                                        customVote = ""
                                        send("vote", v)
                                    }
                                )
                            }
                        }

                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                gap(Spacing.sm)
                                alignItems(AlignItems.Center)
                            }
                        }) {
                            Input(type = InputType.Text) {
                                value(customVote)
                                placeholder("Custom value")
                                onInput { customVote = it.value }
                                onKeyUp {
                                    if (it.key == "Enter" && customVote.isNotBlank()) {
                                        val v = customVote.trim()
                                        myVote = v
                                        send("vote", v)
                                    }
                                }
                                style {
                                    flex(1)
                                    padding(10.px, 14.px)
                                    border {
                                        width(1.5.px)
                                        style(LineStyle.Solid)
                                        color = Color(Colors.border)
                                    }
                                    borderRadius(8.px)
                                    fontSize(15.px)
                                    property("outline", "none")
                                }
                            }
                            Button({
                                onClick {
                                    val v = customVote.trim()
                                    if (v.isNotBlank()) { myVote = v; send("vote", v) }
                                }
                                style {
                                    backgroundColor(Color(Colors.primary))
                                    color(Color(Colors.surface))
                                    property("border", "none")
                                    borderRadius(8.px)
                                    padding(10.px, 16.px)
                                    property("font-weight", "600")
                                    cursor("pointer")
                                    property("white-space", "nowrap")
                                }
                            }) { Text("Submit") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(label: String, bgColor: String, onClick: () -> Unit) {
    Button({
        onClick { onClick() }
        style {
            backgroundColor(Color(bgColor))
            color(Color(Colors.surface))
            property("border", "none")
            borderRadius(8.px)
            padding(12.px, 24.px)
            fontSize(15.px)
            property("font-weight", "600")
            cursor("pointer")
            flex(1)
            property("transition", "opacity 0.1s ease")
        }
    }) { Text(label) }
}
