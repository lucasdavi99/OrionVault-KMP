package com.cuboidestudio.orionvault.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Vocabulário de movimento do app.
 *
 * O visual anterior não tinha animação alguma — nem transição de rota, nem resposta a toque, nem
 * entrada de lista. É o que mais fazia o app parecer parado no tempo, mais até do que a paleta.
 */
internal object OrionMotion {
    const val fast = 150
    const val medium = 250
    const val slow = 400

    /** Ciclo dos halos do fundo. Bem longo de propósito: deve ser percebido, não notado. */
    const val ambient = 22_000

    /** Saída acelerada, entrada desacelerada — o padrão "emphasized" do Material 3. */
    val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val emphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    fun <T> tweenFast() = tween<T>(fast, easing = standard)
    fun <T> tweenMedium() = tween<T>(medium, easing = emphasized)
    fun <T> tweenSlow() = tween<T>(slow, easing = emphasized)

    /** Molinha discreta para reações a toque (escala de press). Sem overshoot perceptível. */
    fun <T> springSnappy(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}
