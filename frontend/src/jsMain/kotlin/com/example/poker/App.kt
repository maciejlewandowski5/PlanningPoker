package com.example.poker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.poker.state.Screen
import com.example.poker.ui.HomeScreen
import com.example.poker.ui.JoinScreen
import com.example.poker.ui.RoomScreen
import kotlinx.coroutines.MainScope

val appScope = MainScope()

@Composable
fun App() {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    when (val s = screen) {
        is Screen.Home -> HomeScreen(scope = appScope) { screen = it }
        is Screen.Join -> JoinScreen(code = s.code, scope = appScope) { screen = it }
        is Screen.Room -> RoomScreen(
            code = s.code,
            participantId = s.participantId,
            scope = appScope,
            onLeave = { screen = Screen.Home }
        )
    }
}
