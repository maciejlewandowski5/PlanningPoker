package com.example.poker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.poker.api.joinRoom
import com.example.poker.state.Screen
import com.example.poker.ui.components.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

@Composable
fun JoinScreen(
    code: String,
    scope: kotlinx.coroutines.CoroutineScope,
    onNavigate: (Screen) -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Div({
        style {
            minHeight(100.vh)
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            justifyContent(JustifyContent.Center)
            alignItems(AlignItems.Center)
            padding(Spacing.lg)
        }
    }) {
        Div({ style { width(100.percent); maxWidth(400.px) } }) {
            H1({
                style {
                    fontSize(28.px)
                    property("font-weight", "700")
                    color(Color(Colors.textPrimary))
                    property("text-align", "center")
                    marginBottom(4.px)
                }
            }) { Text("Planning Poker") }

            P({
                style {
                    property("text-align", "center")
                    color(Color(Colors.textSecondary))
                    marginBottom(Spacing.xl)
                    fontSize(14.px)
                }
            }) { Text("Room code: $code") }

            Div({
                style {
                    backgroundColor(Color(Colors.surface))
                    borderRadius(12.px)
                    padding(Spacing.lg)
                    property("box-shadow", "0 1px 3px rgba(0,0,0,0.1)")
                }
            }) {
                P({
                    style {
                        property("font-weight", "600")
                        marginBottom(Spacing.md)
                        color(Color(Colors.textSecondary))
                        fontSize(13.px)
                        property("text-transform", "uppercase")
                        property("letter-spacing", "0.05em")
                    }
                }) { Text("What's your name?") }

                Input(type = InputType.Text) {
                    value(displayName)
                    placeholder("Display name")
                    onInput { displayName = it.value }
                    onKeyUp {
                        if (it.key == "Enter") {
                            submitJoin(code, displayName, scope, onNavigate) { err ->
                                error = err
                                loading = false
                            }
                        }
                    }
                    style {
                        inputStyle()
                        marginBottom(Spacing.md)
                    }
                }

                Button({
                    onClick {
                        loading = true
                        error = null
                        submitJoin(code, displayName, scope, onNavigate) { err ->
                            error = err
                            loading = false
                        }
                    }
                    style { primaryButton() }
                    if (loading || displayName.isBlank()) attr("disabled", "true")
                }) {
                    Text(if (loading) "Joining…" else "Enter Room")
                }
            }

            error?.let { msg ->
                P({
                    style {
                        color(Color(Colors.danger))
                        property("text-align", "center")
                        marginTop(Spacing.md)
                        fontSize(14.px)
                    }
                }) { Text(msg) }
            }

            Button({
                onClick { onNavigate(Screen.Home) }
                style {
                    secondaryButton()
                    width(100.percent)
                    marginTop(Spacing.md)
                }
            }) { Text("← Back") }
        }
    }
}

private fun submitJoin(
    code: String,
    displayName: String,
    scope: kotlinx.coroutines.CoroutineScope,
    onNavigate: (Screen) -> Unit,
    onError: (String) -> Unit
) {
    if (displayName.isBlank()) return
    scope.launch {
        try {
            val result = joinRoom(code, displayName.trim())
            onNavigate(Screen.Room(result.code, result.participantId, displayName.trim()))
        } catch (e: Exception) {
            val msg = e.message ?: ""
            onError(if ("404" in msg) "Room not found. Check the code and try again." else "Could not join room.")
        }
    }
}
