package com.cuboidestudio.orionvault.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cuboidestudio.orionvault.ui.theme.OrionMotion
import com.cuboidestudio.orionvault.ui.theme.OrionShapes
import com.cuboidestudio.orionvault.ui.theme.OrionSizes
import com.cuboidestudio.orionvault.ui.theme.OrionTheme

/**
 * Avatar de um item do cofre: inicial do título sobre um gradiente escolhido pelo próprio título.
 *
 * O gradiente é determinístico, então o mesmo item tem sempre a mesma cor — vira uma pista visual
 * para reencontrar credenciais numa lista longa. Antes era um quadrado cinza uniforme para todos.
 */
@Composable
internal fun ItemAvatar(
    title: String,
    modifier: Modifier = Modifier,
    size: Dp = OrionSizes.avatar,
    shape: Shape = OrionShapes.Card
) {
    val gradients = OrionTheme.ext.avatarGradients
    val brush = remember(title, gradients) {
        val (start, end) = gradients[stableIndex(title, gradients.size)]
        Brush.linearGradient(listOf(start, end))
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title.trim().take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color.White
        )
    }
}

/**
 * Hash próprio em vez de [String.hashCode]: garante o mesmo índice em JVM, Native e qualquer alvo
 * futuro, sem depender de detalhes de implementação da stdlib por plataforma.
 */
private fun stableIndex(input: String, buckets: Int): Int {
    var hash = 7
    for (char in input) {
        hash = (hash * 31 + char.code) and 0x7FFFFFFF
    }
    return if (buckets <= 0) 0 else hash % buckets
}

/**
 * Medidor de força de senha em quatro segmentos.
 *
 * Substitui a barra contínua de 4dp. Segmentos discretos comunicam melhor "quão longe estou do topo",
 * e os cantos (externos arredondados, internos retos) seguem o detalhe de hardware usinado que o
 * design system pede para barras de progresso.
 *
 * @param score de 0 a 1, vindo de `PasswordGenerator.strength`.
 */
@Composable
internal fun PasswordStrengthMeter(
    score: Float,
    label: String,
    modifier: Modifier = Modifier
) {
    val ext = OrionTheme.ext
    val clamped = score.coerceIn(0f, 1f)

    val targetColor = when {
        clamped < 0.35f -> ext.strengthWeak
        clamped < 0.6f -> ext.strengthFair
        clamped < 1f -> ext.strengthGood
        else -> ext.strengthPerfect
    }
    val color by animateColorAsState(targetColor, OrionMotion.tweenMedium(), label = "strengthColor")
    val animatedScore by animateFloatAsState(clamped, OrionMotion.tweenMedium(), label = "strengthScore")

    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().height(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(SEGMENTS) { index ->
                val shape = when (index) {
                    0 -> OrionShapes.MeterStart
                    SEGMENTS - 1 -> OrionShapes.MeterEnd
                    else -> OrionShapes.MeterMiddle
                }
                // Cada segmento preenche a fatia que lhe cabe; o último preenche parcialmente.
                val fill = (animatedScore * SEGMENTS - index).coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(shape)
                        .background(trackColor)
                ) {
                    if (fill > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fill)
                                .background(color)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = color)
            Text(
                text = "${(clamped * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private const val SEGMENTS = 4
