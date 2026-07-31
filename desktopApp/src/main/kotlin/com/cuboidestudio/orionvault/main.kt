package com.cuboidestudio.orionvault

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.cuboidestudio.orionvault.storage.secure.PlatformContext

fun main() = application {
    val container = remember { AppContainer(PlatformContext()) }
    Window(
        onCloseRequest = ::exitApplication,
        title = "OrionVault",
    ) {
        App(container)
    }
}