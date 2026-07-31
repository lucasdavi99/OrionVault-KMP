package com.cuboidestudio.orionvault.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Radii from the design system: 8px (buttons/inputs), 12px (cards), 16px (bento cards), pill (badges). */
object OrionShapes {
    val Input = RoundedCornerShape(8.dp)
    val Card = RoundedCornerShape(12.dp)
    val CardLarge = RoundedCornerShape(16.dp)
    val Pill = RoundedCornerShape(50)
}

val OrionMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)
