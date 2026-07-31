package com.cuboidestudio.orionvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuboidestudio.orionvault.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Decide a rota inicial (Onboarding vs. Unlock) e força a volta para Unlock sempre que
 * a sessão for bloqueada (manualmente ou por auto-lock — design doc seção 5).
 */
class RootViewModel(private val container: AppContainer) : ViewModel() {
    private val _route = MutableStateFlow<Route>(Route.Loading)
    val route: StateFlow<Route> = _route.asStateFlow()

    init {
        viewModelScope.launch {
            _route.value = if (container.vaultRepository.isVaultInitialized()) Route.Unlock else Route.Onboarding
        }
        viewModelScope.launch {
            container.sessionManager.isUnlocked.collect { unlocked ->
                if (!unlocked && _route.value != Route.Onboarding && _route.value != Route.Loading) {
                    _route.value = Route.Unlock
                }
            }
        }
    }

    fun navigateTo(route: Route) {
        _route.value = route
    }
}
