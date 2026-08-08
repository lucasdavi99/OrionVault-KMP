package com.cuboidestudio.orionvault.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.cuboidestudio.orionvault.ui.theme.OrionMotion
import com.cuboidestudio.orionvault.ui.theme.OrionTheme
import kotlin.math.max

/**
 * Canvas do app: preto neutro com dois halos de cor que derivam lentamente.
 *
 * É o que substitui o fundo chapado anterior. O movimento é longo (22s, ida e volta) de propósito —
 * deve ser percebido perifericamente, nunca notado como animação.
 *
 * Os raios são derivados do tamanho real em [drawBehind], não de pixels crus: a tela de unlock antes
 * usava `radius = 900f`, um valor em pixels que virava um halo minúsculo em telas densas e um borrão
 * em telas de baixa densidade.
 *
 * @param intensity multiplica o alfa dos halos. Telas-herói (unlock, onboarding) usam valores > 1.
 */
@Composable
internal fun OrionBackground(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    animated: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val canvas = MaterialTheme.colorScheme.background
    val violet = OrionTheme.ext.ambientViolet
    val emerald = OrionTheme.ext.ambientEmerald

    // A transição é sempre criada (chamar composables condicionalmente quebraria a composição se
    // `animated` mudasse); o parâmetro só decide se o valor animado é usado ou congelado no meio.
    val transition = rememberInfiniteTransition(label = "ambient")
    val animatedDrift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(OrionMotion.ambient, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )
    val drift = if (animated) animatedDrift else 0.5f

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(canvas)

                val span = max(size.width, size.height)

                // Halo violeta: canto superior esquerdo, deriva na diagonal.
                val violetRadius = span * 0.95f
                val violetCenter = Offset(
                    x = size.width * (0.08f + 0.18f * drift),
                    y = size.height * (0.02f + 0.12f * drift)
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(violet.scaleAlpha(intensity), Color.Transparent),
                        center = violetCenter,
                        radius = violetRadius
                    )
                )

                // Halo esmeralda: canto inferior direito, deriva na direção oposta.
                val emeraldRadius = span * 0.8f
                val emeraldCenter = Offset(
                    x = size.width * (0.98f - 0.16f * drift),
                    y = size.height * (0.92f - 0.1f * drift)
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(emerald.scaleAlpha(intensity), Color.Transparent),
                        center = emeraldCenter,
                        radius = emeraldRadius
                    )
                )
            },
        content = content
    )
}

/** Multiplica o alfa mantendo-o em 0..1 — [intensity] pode passar de 1 nas telas-herói. */
private fun Color.scaleAlpha(intensity: Float): Color =
    copy(alpha = (alpha * intensity).coerceIn(0f, 1f))
