package com.example.poker.ui.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

@Composable
fun VoteButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Button({
        onClick { onClick() }
        style {
            width(52.px)
            height(52.px)
            borderRadius(10.px)
            fontSize(15.px)
            property("font-weight", "700")
            cursor("pointer")
            property("transition", "background-color 0.15s ease, color 0.15s ease, border-color 0.15s ease, transform 0.1s ease")
            border {
                width(2.px)
                style(LineStyle.Solid)
                color = Color(if (selected) Colors.primary else Colors.border)
            }
            if (selected) {
                backgroundColor(Color(Colors.primary))
                color(Color(Colors.surface))
                property("transform", "scale(1.05)")
            } else {
                backgroundColor(Color(Colors.surface))
                color(Color(Colors.textPrimary))
            }
        }
    }) {
        Text(label)
    }
}
