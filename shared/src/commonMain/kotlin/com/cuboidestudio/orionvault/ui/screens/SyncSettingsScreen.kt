package com.cuboidestudio.orionvault.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cuboidestudio.orionvault.session.AccountState
import com.cuboidestudio.orionvault.sync.SyncConflict
import com.cuboidestudio.orionvault.sync.SyncStatus
import com.cuboidestudio.orionvault.ui.components.OrionButton
import com.cuboidestudio.orionvault.ui.components.OrionButtonSize
import com.cuboidestudio.orionvault.ui.components.OrionButtonVariant
import com.cuboidestudio.orionvault.ui.components.OrionScaffold
import com.cuboidestudio.orionvault.ui.components.OrionSectionHeader
import com.cuboidestudio.orionvault.ui.components.OrionSurface
import com.cuboidestudio.orionvault.ui.components.OrionTopBar
import com.cuboidestudio.orionvault.ui.components.SecurityBadge
import com.cuboidestudio.orionvault.ui.components.SecurityLevel
import com.cuboidestudio.orionvault.ui.theme.OrionSizes
import com.cuboidestudio.orionvault.ui.theme.OrionSpacing
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

    val isSyncing = status == SyncStatus.Syncing
    val isLoggedIn = account is AccountState.LoggedIn

    OrionScaffold(
        topBar = { OrionTopBar(title = "Sincronização", onBack = onBack, showDivider = false) }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = OrionSizes.contentForm)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = OrionSpacing.screenH, vertical = OrionSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(OrionSpacing.md)
            ) {
                OrionSurface(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OrionSpacing.sm)
                    ) {
                        SyncStatusIcon(status = status, syncing = isSyncing)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = (account as? AccountState.LoggedIn)?.email ?: "Não conectado",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(OrionSpacing.xxs))
                            Text(
                                text = when (val current = status) {
                                    SyncStatus.Idle -> lastSyncedAt?.let { "Última sincronização: $it" }
                                        ?: "Nenhuma sincronização ainda"

                                    SyncStatus.Syncing -> "Sincronizando..."
                                    is SyncStatus.Error -> current.message
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (status is SyncStatus.Error) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(OrionSpacing.lg))

                    OrionButton(
                        text = "Sincronizar agora",
                        onClick = viewModel::syncNow,
                        icon = Icons.Filled.CloudSync,
                        loading = isSyncing,
                        enabled = !isSyncing && isLoggedIn,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(OrionSpacing.xs))

                    OrionButton(
                        text = "Sair da conta",
                        onClick = viewModel::logout,
                        icon = Icons.AutoMirrored.Filled.Logout,
                        variant = OrionButtonVariant.Destructive,
                        enabled = isLoggedIn,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(OrionSpacing.sm))

                    Text(
                        text = "Sair encerra apenas a sincronização em nuvem. Seu cofre local " +
                            "continua intacto e destravado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OrionSurface(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OrionSpacing.sm)
                    ) {
                        SecurityBadge(text = "Conhecimento zero", level = SecurityLevel.Secure)
                    }
                    Spacer(Modifier.height(OrionSpacing.xs))
                    Text(
                        text = "O servidor guarda apenas texto cifrado. Nem sua Master Password nem " +
                            "sua Secret Key saem deste dispositivo, então ninguém além de você " +
                            "consegue abrir o conteúdo sincronizado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (conflicts.isNotEmpty()) {
                    OrionSectionHeader("Conflitos")
                    Text(
                        text = "Estes itens mudaram em mais de um dispositivo. Escolha qual versão manter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    conflicts.forEach { conflict ->
                        ConflictCard(
                            conflict = conflict,
                            onKeepLocal = { viewModel.keepLocal(conflict) },
                            onKeepRemote = { viewModel.keepRemote(conflict) }
                        )
                    }
                }
            }
        }
    }
}

/** Ícone de status: gira continuamente enquanto sincroniza, para o estado ser lido de relance. */
@Composable
private fun SyncStatusIcon(status: SyncStatus, syncing: Boolean) {
    val transition = rememberInfiniteTransition(label = "syncSpin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    val isError = status is SyncStatus.Error
    val icon = when {
        isError -> Icons.Filled.Warning
        syncing -> Icons.Filled.CloudSync
        else -> Icons.Filled.CloudDone
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = when {
            isError -> MaterialTheme.colorScheme.error
            syncing -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.secondary
        },
        modifier = Modifier
            .size(28.dp)
            .graphicsLayer { rotationZ = if (syncing) angle else 0f }
    )
}

@Composable
private fun ConflictCard(
    conflict: SyncConflict,
    onKeepLocal: () -> Unit,
    onKeepRemote: () -> Unit
) {
    OrionSurface(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = conflict.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(OrionSpacing.xxs))

        Text(
            text = "Este aparelho: ${conflict.localUpdatedAt}  ·  Outro: ${conflict.remoteUpdatedAt}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(OrionSpacing.sm))

        Row(horizontalArrangement = Arrangement.spacedBy(OrionSpacing.xs)) {
            OrionButton(
                text = "Manter deste",
                onClick = onKeepLocal,
                variant = OrionButtonVariant.Secondary,
                size = OrionButtonSize.Medium,
                modifier = Modifier.weight(1f)
            )
            OrionButton(
                text = "Manter do outro",
                onClick = onKeepRemote,
                variant = OrionButtonVariant.Ghost,
                size = OrionButtonSize.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
