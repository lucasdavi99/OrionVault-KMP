package com.cuboidestudio.orionvault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cuboidestudio.orionvault.ui.theme.OrionColors
import com.cuboidestudio.orionvault.ui.theme.OrionShapes

/**
 * Translucent card matching the design system's ".glass-panel" style
 * (backdrop-filter blur is not portable across all Compose Multiplatform
 * targets, so depth is approximated with a translucent fill + hairline border).
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = OrionShapes.CardLarge,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val base = modifier
        .background(OrionColors.GlassPanel, shape)
        .border(1.dp, OrionColors.GlassPanelBorder, shape)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(padding)
    Box(base) { content() }
}
