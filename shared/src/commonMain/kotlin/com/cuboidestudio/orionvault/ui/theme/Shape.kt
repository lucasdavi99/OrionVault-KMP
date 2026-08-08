package com.cuboidestudio.orionvault.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Raios do design system: 8 (botões/inputs), 12 (cards), 16 (cards grandes), 24 (diálogos), pílula (badges). */
internal object OrionShapes {
    val Input = RoundedCornerShape(10.dp)
    val Button = RoundedCornerShape(12.dp)
    val Card = RoundedCornerShape(14.dp)
    val CardLarge = RoundedCornerShape(18.dp)
    val Dialog = RoundedCornerShape(24.dp)
    val Pill = RoundedCornerShape(percent = 50)

    /**
     * Segmento do medidor de senha: cantos externos arredondados, internos retos.
     * É o detalhe "machined hardware" que o design doc pede para barras de progresso.
     */
    val MeterStart = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 1.dp, bottomEnd = 1.dp)
    val MeterMiddle = RoundedCornerShape(1.dp)
    val MeterEnd = RoundedCornerShape(topStart = 1.dp, bottomStart = 1.dp, topEnd = 3.dp, bottomEnd = 3.dp)
}

internal val OrionMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)
