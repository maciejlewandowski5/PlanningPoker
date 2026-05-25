package com.example.poker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.poker.api.RoomState
import com.example.poker.ui.components.*
import com.example.poker.ws.sendMessage
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.MessageEvent
import org.w3c.dom.WebSocket

private val json = Json { ignoreUnknownKeys = true }
private val QUICK_VOTES = listOf("1", "2", "3", "5", "8", "13", "21", "40", "100", "?")

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
    var ws by remember { mutableStateOf<WebSocket?>(null) }

    fun buildWsUrl(): String {
        val loc = window.location
        val proto = if (loc.protocol == "https:") "wss:" else "ws:"
        return "$proto//${loc.host}/rooms/$code/ws?participantId=$participantId"
    }

    fun connect(attempt: Int = 0) {
        val socket = WebSocket(buildWsUrl())
        ws = socket
        socket.onopen = { connected = true }
        socket.onmessage = { event: MessageEvent ->
            try {
                val state = json.decodeFromString<RoomState>(event.data.toString())
                roomState = state
                if (state.votesRevealed) {
                    myVote = state.participants.find { it.participantId == participantId }?.vote ?: myVote
                }
            } catch (_: Exception) {}
        }
        socket.onclose = {
            connected = false
            scope.launch {
                delay(minOf(1000L * (1 shl attempt.coerceAtMost(5)), 30_000L))
                connect(attempt + 1)
            }
        }
        socket.onerror = {}
    }

    fun send(type: String, value: String? = null) {
        ws?.takeIf { it.readyState == WebSocket.OPEN }?.sendMessage(type, value)
    }

    LaunchedEffect(code, participantId) { connect() }

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
                    gap(Spacing.md)
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
                Button({
                    onClick {
                        ws?.close()
                        onLeave()
                    }
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

                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            flexWrap(FlexWrap.Wrap)
                            gap(Spacing.md)
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

                // Voting controls (hidden when votes are revealed)
                if (!state.votesRevealed) {
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
                        }) { Text("Your vote") }

                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                flexWrap(FlexWrap.Wrap)
                                gap(Spacing.sm)
                                marginBottom(Spacing.md)
                            }
                        }) {
                            QUICK_VOTES.forEach { v ->
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

                // Action bar
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        gap(Spacing.md)
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
        }
    }) { Text(label) }
}
