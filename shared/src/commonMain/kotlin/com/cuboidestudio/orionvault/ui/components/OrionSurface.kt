package com.cuboidestudio.orionvault.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.cuboidestudio.orionvault.ui.theme.OrionMotion
import com.cuboidestudio.orionvault.ui.theme.OrionShapes
import com.cuboidestudio.orionvault.ui.theme.OrionSpacing
import com.cuboidestudio.orionvault.ui.theme.OrionTheme

/**
 * Painel de vidro do app — o card base de toda a interface.
 *
 * Sobre o `GlassPanel` anterior (preenchimento chapado + borda de cor única) muda o que dá a sensação
 * de profundidade: o preenchimento é um gradiente vertical e a borda também, mais clara no topo. Isso
 * simula a luz batendo na quina superior do vidro. Desfoque de fundo real continua fora de questão —
 * `backdrop-filter` não é portável entre os alvos do Compose Multiplatform.
 *
 * Quando clicável, ganha ripple de verdade e uma redução de escala discreta no toque.
 */
@Composable
internal fun OrionSurface(
    modifier: Modifier = Modifier,
    shape: Shape = OrionShapes.CardLarge,
    padding: PaddingValues = PaddingValues(OrionSpacing.md),
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val ext = OrionTheme.ext
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.985f else 1f,
        animationSpec = OrionMotion.springSnappy(),
        label = "pressScale"
    )

    val fill = remember(ext) { Brush.verticalGradient(listOf(ext.cardFillTop, ext.cardFillBottom)) }
    val stroke = remember(ext) {
        Brush.verticalGradient(listOf(ext.cardBorderTop, ext.cardBorderBottom))
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(fill, shape)
            .border(1.dp, stroke, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(color = MaterialTheme.colorScheme.primary),
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(padding),
        content = content
    )
}
