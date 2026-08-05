package com.cuboidestudio.orionvault

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.cuboidestudio.orionvault.storage.secure.PlatformContext
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationDidBecomeActiveNotification

fun MainViewController() = ComposeUIViewController {
    val container = remember { AppContainer(PlatformContext()) }
    // Gatilho de sync ao (re)entrar em primeiro plano (plano, fase 5.1). O
    // UIApplicationDidBecomeActiveNotification cobre tanto o primeiro launch quanto os
    // retornos de background, sem exigir mudanças no app delegate Swift.
    DisposableEffect(container) {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = null
        ) { _ -> container.syncEngine.onAppForegrounded() }
        onDispose { NSNotificationCenter.defaultCenter.removeObserver(observer) }
    }
    App(container)
}