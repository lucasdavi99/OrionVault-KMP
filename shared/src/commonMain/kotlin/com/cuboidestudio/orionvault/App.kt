package com.cuboidestudio.orionvault

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cuboidestudio.orionvault.session.AccountState
import com.cuboidestudio.orionvault.ui.components.OrionBackground
import com.cuboidestudio.orionvault.ui.screens.AuthScreen
import com.cuboidestudio.orionvault.ui.screens.FolderEditorScreen
import com.cuboidestudio.orionvault.ui.screens.ItemEditorScreen
import com.cuboidestudio.orionvault.ui.screens.OnboardingScreen
import com.cuboidestudio.orionvault.ui.screens.SyncSettingsScreen
import com.cuboidestudio.orionvault.ui.screens.UnlockScreen
import com.cuboidestudio.orionvault.ui.screens.VaultScreen
import com.cuboidestudio.orionvault.ui.theme.OrionMotion
import com.cuboidestudio.orionvault.ui.theme.OrionVaultTheme
import com.cuboidestudio.orionvault.viewmodel.AuthViewModel
import com.cuboidestudio.orionvault.viewmodel.FolderEditorViewModel
import com.cuboidestudio.orionvault.viewmodel.ItemEditorViewModel
import com.cuboidestudio.orionvault.viewmodel.OnboardingViewModel
import com.cuboidestudio.orionvault.viewmodel.RootViewModel
import com.cuboidestudio.orionvault.viewmodel.Route
import com.cuboidestudio.orionvault.viewmodel.SyncSettingsViewModel
import com.cuboidestudio.orionvault.viewmodel.UnlockViewModel
import com.cuboidestudio.orionvault.viewmodel.VaultViewModel

@Composable
fun App(container: AppContainer) {
    val rootViewModel = remember { RootViewModel(container) }
    val vaultViewModel = remember { VaultViewModel(container) }
    val route by rootViewModel.route.collectAsState()

    OrionVaultTheme {
        AnimatedContent(
            targetState = route,
            transitionSpec = {
                // A direção sai da profundidade das rotas: ir para um editor entra pela direita,
                // voltar para o cofre entra pela esquerda.
                val forward = targetState.depth >= initialState.depth
                val dir = if (forward) 1 else -1
                (
                    slideInHorizontally(OrionMotion.tweenMedium()) { it / 5 * dir } +
                        fadeIn(OrionMotion.tweenMedium())
                    ) togetherWith (
                    slideOutHorizontally(OrionMotion.tweenMedium()) { -it / 5 * dir } +
                        fadeOut(OrionMotion.tweenFast())
                    )
            },
            label = "route"
        ) { currentRoute ->
            when (currentRoute) {
                Route.Loading -> SplashScreen()

                Route.Onboarding -> {
                    val vm = remember { OnboardingViewModel(container) }
                    OnboardingScreen(vm, onCompleted = {
                        vaultViewModel.refresh()
                        rootViewModel.navigateTo(Route.Vault)
                    })
                }

                Route.Unlock -> {
                    val vm = remember { UnlockViewModel(container) }
                    UnlockScreen(vm, onUnlocked = {
                        vaultViewModel.refresh()
                        rootViewModel.navigateTo(Route.Vault)
                    })
                }

                Route.Vault -> {
                    val accountState by container.accountSessionManager.accountState.collectAsState()
                    VaultScreen(
                        viewModel = vaultViewModel,
                        onOpenItem = { folderId, itemId ->
                            rootViewModel.navigateTo(Route.EditItem(folderId, itemId))
                        },
                        onOpenFolder = { parentId, folderId ->
                            rootViewModel.navigateTo(Route.EditFolder(parentId, folderId))
                        },
                        onOpenAccount = {
                            rootViewModel.navigateTo(
                                if (accountState is AccountState.LoggedIn) {
                                    Route.SyncSettings
                                } else {
                                    Route.AccountAuth
                                }
                            )
                        }
                    )
                }

                Route.AccountAuth -> {
                    val vm = remember { AuthViewModel(container) }
                    AuthScreen(
                        viewModel = vm,
                        onBack = { rootViewModel.navigateTo(Route.Vault) },
                        onAuthenticated = { rootViewModel.navigateTo(Route.SyncSettings) }
                    )
                }

                Route.SyncSettings -> {
                    val vm = remember { SyncSettingsViewModel(container) }
                    SyncSettingsScreen(
                        viewModel = vm,
                        onBack = { rootViewModel.navigateTo(Route.Vault) },
                        onLoggedOut = { rootViewModel.navigateTo(Route.Vault) }
                    )
                }

                is Route.EditItem -> {
                    val vm = remember(currentRoute) {
                        ItemEditorViewModel(container, currentRoute.folderId, currentRoute.itemId)
                    }
                    ItemEditorScreen(vm, onDone = {
                        vaultViewModel.refresh()
                        rootViewModel.navigateTo(Route.Vault)
                    })
                }

                is Route.EditFolder -> {
                    val vm = remember(currentRoute) {
                        FolderEditorViewModel(container, currentRoute.parentId, currentRoute.folderId)
                    }
                    FolderEditorScreen(vm, onDone = {
                        vaultViewModel.refresh()
                        rootViewModel.navigateTo(Route.Vault)
                    })
                }
            }
        }
    }
}

/**
 * Profundidade da rota na hierarquia, só para escolher a direção da transição.
 *
 * Não é um back stack — a navegação continua sendo uma máquina de estados; isto apenas evita que
 * voltar do editor pareça um avanço.
 */
private val Route.depth: Int
    get() = when (this) {
        Route.Loading -> 0
        Route.Onboarding -> 0
        Route.Unlock -> 0
        Route.Vault -> 1
        Route.AccountAuth -> 2
        Route.SyncSettings -> 2
        is Route.EditItem -> 2
        is Route.EditFolder -> 2
    }

/**
 * Tela de abertura enquanto [RootViewModel] decide entre onboarding e unlock.
 *
 * Antes essa rota renderizava `Unit`, ou seja, um retângulo preto sem nenhuma indicação de que o app
 * estava vivo.
 */
@Composable
private fun SplashScreen() {
    OrionBackground(intensity = 1.4f) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(42.dp)
                )
            }
        }
    }
}
