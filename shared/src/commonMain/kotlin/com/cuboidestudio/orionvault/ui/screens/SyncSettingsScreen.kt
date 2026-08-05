package com.cuboidestudio.orionvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cuboidestudio.orionvault.session.AccountState
import com.cuboidestudio.orionvault.sync.SyncConflict
import com.cuboidestudio.orionvault.sync.SyncStatus
import com.cuboidestudio.orionvault.ui.components.GlassPanel
import com.cuboidestudio.orionvault.ui.components.VaultTopBar
import com.cuboidestudio.orionvault.ui.theme.OrionColors
import com.cuboidestudio.orionvault.viewmodel.SyncSettingsViewModel

@Composable
fun SyncSettingsScreen(viewModel: SyncSettingsViewModel, onBack: () -> Unit, onLoggedOut: () -> Unit) {
    val account by viewModel.accountState.collectAsState()
    val status by viewModel.status.collectAsState()
    val lastSyncedAt by viewModel.lastSyncedAt.collectAsState()
    val conflicts by viewModel.conflicts.collectAsState()
    val loggedOut by viewModel.loggedOut.collectAsState()

    LaunchedEffect(loggedOut) {
        if (loggedOut) onLoggedOut()
    }

    Column(modifier = Modifier.fillMaxSize().background(OrionColors.Background)) {
        VaultTopBar(
            leading = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = OrionColors.Primary
                    )
                }
            },
            title = "Sincronização",
            trailing = { Spacer(Modifier.size(48.dp)) }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .widthIn(max = 640.dp)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassPanel(modifier = Modifier.fillMaxWidth(), padding = 20.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            if (status is SyncStatus.Error) Icons.Filled.Warning else Icons.Filled.CloudDone,
                            contentDescription = null,
                            tint = if (status is SyncStatus.Error) OrionColors.Error else OrionColors.Secondary
                        )
                        Text(
                            (account as? AccountState.LoggedIn)?.email ?: "Não conectado",
                            style = MaterialTheme.typography.titleMedium,
                            color = OrionColors.OnSurface
                        )
                    }

                    Text(
                        when (val current = status) {
                            SyncStatus.Idle -> lastSyncedAt?.let { "Última sincronização: $it" }
                                ?: "Nenhuma sincronização ainda"

                            SyncStatus.Syncing -> "Sincronizando..."
                            is SyncStatus.Error -> current.message
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (status is SyncStatus.Error) OrionColors.Error else OrionColors.OnSurfaceVariant
                    )

                    Button(
                        onClick = viewModel::syncNow,
                        enabled = status != SyncStatus.Syncing && account is AccountState.LoggedIn,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrionColors.SecondaryContainer,
                            contentColor = OrionColors.OnSecondaryContainer
                        )
                    ) {
                        if (status == SyncStatus.Syncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = OrionColors.OnSecondaryContainer
                            )
                        } else {
                            Icon(Icons.Filled.CloudSync, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Sincronizar agora", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    TextButton(onClick = viewModel::logout, modifier = Modifier.fillMaxWidth()) {
                        Text("Sair da conta", style = MaterialTheme.typography.labelSmall, color = OrionColors.Error)
                    }

                    Text(
                        "Sair da conta encerra apenas a sincronização em nuvem. Seu cofre local " +
                            "continua intacto e destravado.",
                        style = MaterialTheme.typography.labelSmall,
                        color = OrionColors.Outline
                    )
                }
            }

            if (conflicts.isNotEmpty()) {
                Text(
                    "CONFLITOS",
                    style = MaterialTheme.typography.labelSmall,
                    color = OrionColors.OnSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                conflicts.forEach { conflict ->
                    ConflictRow(
                        conflict = conflict,
                        onKeepLocal = { viewModel.keepLocal(conflict) },
                        onKeepRemote = { viewModel.keepRemote(conflict) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConflictRow(conflict: SyncConflict, onKeepLocal: () -> Unit, onKeepRemote: () -> Unit) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), padding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(conflict.title, style = MaterialTheme.typography.titleMedium, color = OrionColors.OnSurface)
            Text(
                "Este dispositivo: ${conflict.localUpdatedAt} · Outro dispositivo: ${conflict.remoteUpdatedAt}",
                style = MaterialTheme.typography.labelSmall,
                color = OrionColors.OnSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onKeepLocal) {
                    Text("Manter deste aparelho", style = MaterialTheme.typography.labelSmall, color = OrionColors.Primary)
                }
                TextButton(onClick = onKeepRemote) {
                    Text("Manter do outro", style = MaterialTheme.typography.labelSmall, color = OrionColors.Secondary)
                }
            }
        }
    }
}
