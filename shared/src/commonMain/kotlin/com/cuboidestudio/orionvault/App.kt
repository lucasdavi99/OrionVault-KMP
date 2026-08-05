package com.cuboidestudio.orionvault

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.cuboidestudio.orionvault.session.AccountState
import com.cuboidestudio.orionvault.ui.screens.AuthScreen
import com.cuboidestudio.orionvault.ui.screens.FolderEditorScreen
import com.cuboidestudio.orionvault.ui.screens.ItemEditorScreen
import com.cuboidestudio.orionvault.ui.screens.OnboardingScreen
import com.cuboidestudio.orionvault.ui.screens.SyncSettingsScreen
import com.cuboidestudio.orionvault.ui.screens.UnlockScreen
import com.cuboidestudio.orionvault.ui.screens.VaultScreen
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
        when (val currentRoute = route) {
            Route.Loading -> Unit

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
                    onOpenItem = { folderId, itemId -> rootViewModel.navigateTo(Route.EditItem(folderId, itemId)) },
                    onOpenFolder = { parentId, folderId -> rootViewModel.navigateTo(Route.EditFolder(parentId, folderId)) },
                    onOpenAccount = {
                        rootViewModel.navigateTo(
                            if (accountState is AccountState.LoggedIn) Route.SyncSettings else Route.AccountAuth
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
