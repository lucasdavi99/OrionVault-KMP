package com.cuboidestudio.orionvault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cuboidestudio.orionvault.ui.theme.OrionShapes
import com.cuboidestudio.orionvault.ui.theme.OrionSizes
import com.cuboidestudio.orionvault.ui.theme.OrionSpacing

internal enum class SecurityLevel { Secure, Warning, Danger }

/** Pílula de status. Verde só significa "seguro/verificado" — nunca é usada decorativamente. */
@Composable
internal fun SecurityBadge(
    text: String,
    level: SecurityLevel,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val (tint, icon) = when (level) {
        SecurityLevel.Secure -> scheme.secondary to Icons.Filled.VerifiedUser
        SecurityLevel.Warning -> scheme.tertiary to Icons.Filled.WarningAmber
        SecurityLevel.Danger -> scheme.error to Icons.Filled.ErrorOutline
    }

    Row(
        modifier = modifier
            .background(tint.copy(alpha = 0.12f), OrionShapes.Pill)
            .border(1.dp, tint.copy(alpha = 0.30f), OrionShapes.Pill)
            .padding(horizontal = OrionSpacing.sm, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OrionSpacing.xxs + 2.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

/** Cabeçalho de seção em caixa alta. O `labelSmall` do tema já carrega o tracking largo. */
@Composable
internal fun OrionSectionHeader(
    label: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = OrionSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        trailing?.invoke()
    }
}

/**
 * Estado vazio único, parametrizado.
 *
 * Substitui três blocos praticamente idênticos espalhados pelas telas — um deles com o título
 * "Pastas" fixo em código, que aparecia errado dentro de subpastas.
 */
@Composable
internal fun OrionEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = OrionSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    Brush.radialGradient(
                        listOf(scheme.primary.copy(alpha = 0.18f), Color.Transparent)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(Modifier.height(OrionSpacing.lg))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = scheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(OrionSpacing.xs))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(OrionSpacing.xl))
            OrionButton(
                text = actionLabel,
                onClick = onAction,
                icon = actionIcon,
                variant = OrionButtonVariant.Primary
            )
        }
    }
}

/**
 * Diálogo temático.
 *
 * Padroniza confirmações destrutivas, que antes existiam só no editor de pastas — o editor de itens
 * apagava sem perguntar nada.
 */
@Composable
internal fun OrionDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = "Cancelar",
    destructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = OrionShapes.Dialog,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            OrionButton(
                text = confirmLabel,
                onClick = onConfirm,
                size = OrionButtonSize.Medium,
                variant = if (destructive) OrionButtonVariant.Destructive else OrionButtonVariant.Primary
            )
        },
        dismissButton = {
            OrionButton(
                text = dismissLabel,
                onClick = onDismissRequest,
                size = OrionButtonSize.Medium,
                variant = OrionButtonVariant.Ghost
            )
        }
    )
}

/** Espaçador do fim das listas, para o FAB não cobrir o último item. */
@Composable
internal fun FabSpacer() {
    Spacer(Modifier.height(OrionSizes.listBottom))
}
