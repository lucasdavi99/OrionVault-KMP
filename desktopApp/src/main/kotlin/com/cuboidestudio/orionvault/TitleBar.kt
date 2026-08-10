package com.cuboidestudio.orionvault

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState

private val outlineStroke = Stroke(width = 1.2f)

/**
 * Barra de título própria: a janela é aberta com undecorated = true (ver main.kt), então o SO não
 * desenha nada aqui — sem isso a janela fica sem título, sem botões e sem forma de arrastar.
 */
@Composable
fun FrameWindowScope.CustomTitleBar(windowState: WindowState, onCloseRequest: () -> Unit) {
    WindowDraggableArea(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "OrionVault",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(start = 14.dp)
            )

            val defaultIconColor = MaterialTheme.colorScheme.onSurface
            val hoverBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

            WindowControlButton(hoverBackground = hoverBg, onClick = { windowState.isMinimized = true }) { color ->
                drawLine(color, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f))
            }

            val isMaximized = windowState.placement == WindowPlacement.Maximized
            WindowControlButton(
                hoverBackground = hoverBg,
                onClick = {
                    windowState.placement =
                        if (isMaximized) WindowPlacement.Floating else WindowPlacement.Maximized
                }
            ) { color ->
                if (isMaximized) {
                    val inset = 2.dp.toPx()
                    drawRect(color, topLeft = Offset(inset, 0f), size = size.copy(width = size.width - inset, height = size.height - inset), style = outlineStroke)
                    drawRect(color, topLeft = Offset(0f, inset), size = size.copy(width = size.width - inset, height = size.height - inset), style = outlineStroke)
                } else {
                    drawRect(color, style = outlineStroke)
                }
            }

            WindowControlButton(
                hoverBackground = Color(0xFFE81123),
                hoverIconColor = Color.White,
                onClick = onCloseRequest
            ) { color ->
                drawLine(color, Offset(0f, 0f), Offset(size.width, size.height))
                drawLine(color, Offset(size.width, 0f), Offset(0f, size.height))
            }
        }
    }
}

@Composable
private fun WindowControlButton(
    hoverBackground: Color,
    hoverIconColor: Color? = null,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.drawscope.DrawScope.(color: Color) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val defaultIconColor = MaterialTheme.colorScheme.onSurface
    val iconColor = if (isHovered && hoverIconColor != null) hoverIconColor else defaultIconColor

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(46.dp)
            .background(if (isHovered) hoverBackground else Color.Transparent)
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(10.dp)) { icon(iconColor) }
    }
}
