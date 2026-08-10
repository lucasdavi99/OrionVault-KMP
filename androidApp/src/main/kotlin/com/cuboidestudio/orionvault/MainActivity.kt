package com.cuboidestudio.orionvault

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.cuboidestudio.orionvault.security.PlatformBiometricContext
import com.cuboidestudio.orionvault.storage.secure.PlatformContext

class MainActivity : FragmentActivity() {
    private lateinit var container: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        // Sem argumentos, enableEdgeToEdge decide o contraste dos ícones das barras pelo tema DO
        // SISTEMA. Como o app é dark-only, num aparelho em modo claro os ícones ficavam escuros
        // sobre o fundo escuro do cofre. Forçar o estilo escuro mantém os ícones claros sempre.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        container = AppContainer(PlatformContext(applicationContext), PlatformBiometricContext(this))
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