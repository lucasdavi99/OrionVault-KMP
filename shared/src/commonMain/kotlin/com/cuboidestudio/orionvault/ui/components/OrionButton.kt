package com.cuboidestudio.orionvault.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cuboidestudio.orionvault.ui.theme.OrionMotion
import com.cuboidestudio.orionvault.ui.theme.OrionShapes
import com.cuboidestudio.orionvault.ui.theme.OrionSizes
import com.cuboidestudio.orionvault.ui.theme.OrionSpacing

internal enum class OrionButtonVariant {
    /** Ação principal da tela. Preenchimento violeta em gradiente. */
    Primary,

    /** Ação de apoio com peso visual. Preenchimento sólido discreto. */
    Secondary,

    /** Ação terciária. Só contorno — o "ghost" do design system. */
    Ghost,

    /** Ação destrutiva. Contorno vermelho; nunca é preenchido, para não convidar ao clique. */
    Destructive
}

internal enum class OrionButtonSize { Large, Medium }

/**
 * O botão do app.
 *
 * Antes disso, o CTA primário era `Box().background().clickable()` copiado em cinco telas — sem ripple,
 * sem estado desabilitado, sem alvo mínimo de toque e sem semântica de botão para leitores de tela.
 * Construído sobre [Surface], que entrega tudo isso de graça.
 */
@Composable
internal fun OrionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: OrionButtonVariant = OrionButtonVariant.Primary,
    size: OrionButtonSize = OrionButtonSize.Large,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val clickable = enabled && !loading

    val scale by animateFloatAsState(
        targetValue = if (pressed && clickable) 0.97f else 1f,
        animationSpec = OrionMotion.springSnappy(),
        label = "buttonPress"
    )

    val height: Dp = when (size) {
        OrionButtonSize.Large -> OrionSizes.buttonLg
        OrionButtonSize.Medium -> OrionSizes.buttonMd
    }

    val contentColor = when (variant) {
        OrionButtonVariant.Primary -> scheme.onPrimaryContainer
        OrionButtonVariant.Secondary -> scheme.onSurface
        OrionButtonVariant.Ghost -> scheme.primary
        OrionButtonVariant.Destructive -> scheme.error
    }

    val border: BorderStroke? = when (variant) {
        OrionButtonVariant.Primary -> null
        OrionButtonVariant.Secondary -> BorderStroke(1.dp, scheme.outlineVariant)
        OrionButtonVariant.Ghost -> BorderStroke(1.dp, scheme.outline)
        OrionButtonVariant.Destructive -> BorderStroke(1.dp, scheme.error.copy(alpha = 0.45f))
    }

    // O gradiente só existe no Primary; as demais variantes usam cor sólida (ou nenhuma).
    val fill: Brush? = when {
        !enabled -> null
        variant == OrionButtonVariant.Primary ->
            Brush.verticalGradient(listOf(scheme.primaryContainer, scheme.inversePrimary))
        variant == OrionButtonVariant.Secondary ->
            Brush.verticalGradient(listOf(scheme.surfaceContainerHigh, scheme.surfaceContainer))
        else -> null
    }

    val disabledFill = if (!enabled) scheme.surfaceContainerHigh.copy(alpha = 0.5f) else Color.Transparent

    Surface(
        onClick = onClick,
        enabled = clickable,
        shape = OrionShapes.Button,
        color = disabledFill,
        contentColor = if (enabled) contentColor else scheme.onSurfaceVariant.copy(alpha = 0.5f),
        border = border,
        interactionSource = interactionSource,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .heightIn(min = height)
    ) {
        Box(
            modifier = Modifier
                .then(if (fill != null) Modifier.background(fill) else Modifier)
                .heightIn(min = height)
                .padding(horizontal = OrionSpacing.lg),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = loading,
                transitionSpec = {
                    fadeIn(OrionMotion.tweenFast()) togetherWith fadeOut(OrionMotion.tweenFast())
                },
                label = "buttonContent"
            ) { isLoading ->
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(OrionSizes.iconSm),
                        strokeWidth = 2.dp,
                        color = contentColor
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OrionSpacing.xs),
                        modifier = Modifier.defaultMinSize(minHeight = height)
                    ) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(OrionSizes.iconSm)
                            )
                        }
                        Text(text = text, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
