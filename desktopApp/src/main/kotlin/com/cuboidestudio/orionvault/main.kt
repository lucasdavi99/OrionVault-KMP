package com.cuboidestudio.orionvault

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.cuboidestudio.orionvault.storage.secure.PlatformContext
import kotlinx.coroutines.flow.filter

fun main() = application {
    val container = remember { AppContainer(PlatformContext()) }
    val windowState = rememberWindowState()
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "OrionVault",
    ) {
        // Equivalente desktop do gatilho de "app em primeiro plano" (plano, fase 5.1):
        // dispara ao ganhar foco da janela.
        LaunchedEffect(window) {
            snapshotFlow { window.isFocused }
                .filter { it }
                .collect { container.syncEngine.onAppForegrounded() }
        }
        App(container)
    }
}