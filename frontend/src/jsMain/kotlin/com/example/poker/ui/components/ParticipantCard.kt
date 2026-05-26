package com.example.poker.ui.components

import androidx.compose.runtime.Composable
import com.example.poker.api.ParticipantState
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

@Composable
fun ParticipantCard(participant: ParticipantState, isMe: Boolean, votesRevealed: Boolean) {
    Div({
        style {
            backgroundColor(Color(if (isMe) Colors.surfaceAlt else Colors.surface))
            border {
                width(if (isMe) 2.px else 1.px)
                style(LineStyle.Solid)
                color = Color(if (isMe) Colors.primary else Colors.border)
            }
            borderRadius(10.px)
            padding(Spacing.md)
            minWidth(120.px)
            property("text-align", "center")
            property("transition", "background-color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease")
        }
    }) {
        P({
            style {
                property("font-weight", "600")
                fontSize(14.px)
                color(Color(if (isMe) Colors.primary else Colors.textPrimary))
                marginBottom(6.px)
                property("overflow", "hidden")
                property("text-overflow", "ellipsis")
                property("white-space", "nowrap")
                maxWidth(140.px)
            }
        }) {
            Text(if (isMe) "${participant.displayName} (you)" else participant.displayName)
        }

        when {
            votesRevealed && participant.vote != null -> VoteBadge(participant.vote, revealed = true)
            votesRevealed && !participant.hasVoted -> VoteBadge("–", revealed = false)
            participant.hasVoted -> VoteBadge("✓", revealed = false)
            else -> P({
                style {
                    fontSize(12.px)
                    color(Color(Colors.textSecondary))
                }
            }) { Text("waiting…") }
        }
    }
}

@Composable
private fun VoteBadge(label: String, revealed: Boolean) {
    Span({
        style {
            display(DisplayStyle.InlineBlock)
            padding(4.px, 10.px)
            borderRadius(20.px)
            fontSize(14.px)
            property("font-weight", "700")
            property("transition", "background-color 0.25s ease, transform 0.2s ease")
            if (revealed) {
                backgroundColor(Color(Colors.primary))
                color(Color(Colors.surface))
            } else {
                backgroundColor(Color(Colors.success))
                color(Color(Colors.surface))
            }
        }
    }) { Text(label) }
}
