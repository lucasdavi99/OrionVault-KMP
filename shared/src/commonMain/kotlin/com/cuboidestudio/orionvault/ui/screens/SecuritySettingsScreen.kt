package com.cuboidestudio.orionvault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cuboidestudio.orionvault.security.BiometricAvailability
import com.cuboidestudio.orionvault.storage.secure.BiometricUnlockChoice
import com.cuboidestudio.orionvault.ui.components.OrionScaffold
import com.cuboidestudio.orionvault.ui.components.OrionSurface
import com.cuboidestudio.orionvault.ui.components.OrionTopBar
import com.cuboidestudio.orionvault.ui.theme.OrionSizes
import com.cuboidestudio.orionvault.ui.theme.OrionSpacing
import com.cuboidestudio.orionvault.viewmodel.SecuritySettingsViewModel

@Composable
fun SecuritySettingsScreen(viewModel: SecuritySettingsViewModel, onBack: () -> Unit) {
    val choice by viewModel.choice.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val available = viewModel.availability == BiometricAvailability.AVAILABLE

    OrionScaffold(
        topBar = { OrionTopBar(title = "Segurança", onBack = onBack, showDivider = false) }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = OrionSizes.contentForm)
                    .fillMaxWidth()
                    .padding(horizontal = OrionSpacing.screenH, vertical = OrionSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(OrionSpacing.md)
            ) {
                OrionSurface(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OrionSpacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = OrionSpacing.xxs)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Desbloqueio por biometria",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(OrionSpacing.xxs))
                            Text(
                                text = availabilityLabel(viewModel.availability),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = choice == BiometricUnlockChoice.ENABLED,
                            onCheckedChange = { checked ->
                                if (checked) viewModel.enable() else viewModel.disable()
                            },
                            enabled = available,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(Modifier.height(OrionSpacing.sm))

                    Text(
                        text = "Use a digital, o rosto, o PIN, o padrão ou a senha já configurados " +
                            "no aparelho para destravar o cofre sem digitar a Master Password. Ela " +
                            "continua sendo a única forma de recuperar o cofre em outro dispositivo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (error != null) {
                        Spacer(Modifier.height(OrionSpacing.xs))
                        Text(
                            text = error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private fun availabilityLabel(availability: BiometricAvailability): String = when (availability) {
    BiometricAvailability.AVAILABLE -> "Disponível neste dispositivo"
    BiometricAvailability.NO_HARDWARE -> "Este aparelho não tem sensor biométrico"
    BiometricAvailability.NOT_ENROLLED -> "Nenhuma biometria ou PIN cadastrado no aparelho"
    BiometricAvailability.UNAVAILABLE -> "Indisponível neste dispositivo"
}
