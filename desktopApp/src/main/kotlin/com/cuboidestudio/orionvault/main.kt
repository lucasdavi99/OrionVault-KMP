package com.cuboidestudio.orionvault

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.cuboidestudio.orionvault.storage.secure.PlatformContext
import kotlinx.coroutines.flow.filter

fun main() = application {
    val container = remember { AppContainer(PlatformContext()) }
    // A janela não tinha tamanho definido, então abria no default do sistema. Este tamanho dá
    // espaço para a grade de pastas usar 3-4 colunas já na abertura.
    val windowState = rememberWindowState(size = DpSize(1120.dp, 780.dp))
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "OrionVault",
    ) {
        // Abaixo disso o layout de duas colunas do editor deixa de fazer sentido.
        window.minimumSize = java.awt.Dimension(480, 620)

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