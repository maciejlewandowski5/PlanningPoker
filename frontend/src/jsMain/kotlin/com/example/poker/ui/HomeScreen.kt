package com.example.poker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.poker.api.createRoom
import com.example.poker.state.Screen
import com.example.poker.ui.components.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

@Composable
fun HomeScreen(scope: kotlinx.coroutines.CoroutineScope, onNavigate: (Screen) -> Unit) {
    var joinCode by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    PageWrapper {
        H1({
            style {
                fontSize(28.px)
                property("font-weight", "700")
                color(Color(Colors.textPrimary))
                property("text-align", "center")
                marginBottom(Spacing.xl)
            }
        }) { Text("Planning Poker") }

        Card {
            P({
                style {
                    property("font-weight", "600")
                    marginBottom(Spacing.md)
                    color(Color(Colors.textSecondary))
                    fontSize(13.px)
                    property("text-transform", "uppercase")
                    property("letter-spacing", "0.05em")
                }
            }) { Text("Start a new session") }

            Button({
                onClick {
                    loading = true
                    error = null
                    scope.launch {
                        try {
                            val room = createRoom()
                            onNavigate(Screen.Join(room.code))
                        } catch (e: Exception) {
                            error = "Could not create room. Is the server running?"
                        } finally {
                            loading = false
                        }
                    }
                }
                style { primaryButton() }
                if (loading) attr("disabled", "true")
            }) {
                Text(if (loading) "Creating…" else "Create Room")
            }
        }

        Div({
            style {
                margin(Spacing.md, 0.px)
                property("text-align", "center")
                color(Color(Colors.textSecondary))
            }
        }) {
            Text("— or —")
        }

        Card {
            P({
                style {
                    property("font-weight", "600")
                    marginBottom(Spacing.md)
                    color(Color(Colors.textSecondary))
                    fontSize(13.px)
                    property("text-transform", "uppercase")
                    property("letter-spacing", "0.05em")
                }
            }) { Text("Join existing room") }

            Input(type = InputType.Text) {
                value(joinCode.uppercase())
                placeholder("Room code (6 characters)")
                onInput { joinCode = it.value.uppercase().take(6) }
                style {
                    inputStyle()
                    marginBottom(Spacing.md)
                }
            }

            Button({
                onClick {
                    if (joinCode.length != 6) { error = "Room code must be 6 characters"; return@onClick }
                    onNavigate(Screen.Join(joinCode))
                }
                style { primaryButton() }
            }) { Text("Join Room") }
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
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Div({
        style {
            backgroundColor(Color(Colors.surface))
            borderRadius(12.px)
            padding(Spacing.lg)
            property("box-shadow", "0 1px 3px rgba(0,0,0,0.1)")
        }
    }) { content() }
}

@Composable
private fun PageWrapper(content: @Composable () -> Unit) {
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
        Div({
            style {
                width(100.percent)
                maxWidth(400.px)
            }
        }) { content() }
    }
}
