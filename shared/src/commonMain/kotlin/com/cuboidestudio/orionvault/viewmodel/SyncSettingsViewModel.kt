package com.cuboidestudio.orionvault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuboidestudio.orionvault.AppContainer
import com.cuboidestudio.orionvault.session.AccountState
import com.cuboidestudio.orionvault.sync.SyncConflict
import com.cuboidestudio.orionvault.sync.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SyncSettingsViewModel(private val container: AppContainer) : ViewModel() {
    val accountState: StateFlow<AccountState> = container.accountSessionManager.accountState
    val status: StateFlow<SyncStatus> = container.syncEngine.status
    val lastSyncedAt: StateFlow<Long?> = container.syncEngine.lastSyncedAt
    val conflicts: StateFlow<List<SyncConflict>> = container.syncEngine.conflicts

    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    fun syncNow() {
        container.syncEngine.syncNow()
    }

    /** Encerra apenas a sessão de nuvem — o cofre local continua exatamente como estava. */
    fun logout() {
        viewModelScope.launch {
            container.accountSessionManager.logout()
            _loggedOut.value = true
        }
    }

    fun keepLocal(conflict: SyncConflict) = container.syncEngine.resolveKeepLocal(conflict)

    fun keepRemote(conflict: SyncConflict) = container.syncEngine.resolveKeepRemote(conflict)
}
