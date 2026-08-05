package com.cuboidestudio.orionvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cuboidestudio.orionvault.storage.secure.PlatformContext

class MainActivity : ComponentActivity() {
    private lateinit var container: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        container = AppContainer(PlatformContext(applicationContext))
        setContent {
            App(container)
        }
    }

    override fun onResume() {
        super.onResume()
        // Gatilho de sync ao trazer o app para o primeiro plano (plano, fase 5.1).
        container.syncEngine.onAppForegrounded()
    }
}