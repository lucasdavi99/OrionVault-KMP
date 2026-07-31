package com.cuboidestudio.orionvault

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.cuboidestudio.orionvault.storage.secure.PlatformContext

fun MainViewController() = ComposeUIViewController {
    val container = remember { AppContainer(PlatformContext()) }
    App(container)
}