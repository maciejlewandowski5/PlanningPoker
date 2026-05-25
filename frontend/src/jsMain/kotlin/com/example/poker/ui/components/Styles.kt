package com.example.poker.ui.components

import org.jetbrains.compose.web.css.*

object Colors {
    const val primary = "#4f46e5"
    const val primaryHover = "#4338ca"
    const val surface = "#ffffff"
    const val surfaceAlt = "#f8f9ff"
    const val border = "#e2e8f0"
    const val textPrimary = "#1a1a2e"
    const val textSecondary = "#64748b"
    const val success = "#22c55e"
    const val danger = "#ef4444"
    const val warning = "#f59e0b"
}

object Spacing {
    val xs = 4.px
    val sm = 8.px
    val md = 16.px
    val lg = 24.px
    val xl = 32.px
    val xxl = 48.px
}

fun StyleScope.card() {
    backgroundColor(Color(Colors.surface))
    borderRadius(12.px)
    padding(Spacing.lg)
    property("box-shadow", "0 1px 3px rgba(0,0,0,0.1), 0 1px 2px rgba(0,0,0,0.06)")
}

fun StyleScope.primaryButton() {
    backgroundColor(Color(Colors.primary))
    color(Color(Colors.surface))
    property("border", "none")
    borderRadius(8.px)
    padding(10.px, 20.px)
    fontSize(15.px)
    property("font-weight", "600")
    cursor("pointer")
    width(100.percent)
}

fun StyleScope.secondaryButton() {
    background("transparent")
    color(Color(Colors.primary))
    border {
        width(2.px)
        style(LineStyle.Solid)
        color = Color(Colors.primary)
    }
    borderRadius(8.px)
    padding(8.px, 16.px)
    fontSize(14.px)
    property("font-weight", "600")
    cursor("pointer")
}

fun StyleScope.inputStyle() {
    width(100.percent)
    padding(10.px, 14.px)
    border {
        width(1.5.px)
        style(LineStyle.Solid)
        color = Color(Colors.border)
    }
    borderRadius(8.px)
    fontSize(15.px)
    property("outline", "none")
    color(Color(Colors.textPrimary))
}
