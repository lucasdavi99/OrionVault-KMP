package com.cuboidestudio.orionvault.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cuboidestudio.orionvault.ui.theme.OrionShapes
import com.cuboidestudio.orionvault.ui.theme.OrionSpacing
import com.cuboidestudio.orionvault.ui.theme.OrionTheme

/**
 * Campo de texto do app.
 *
 * Diferenças em relação ao `VaultTextField` anterior:
 * - o rótulo fica acima do campo, não flutuando dentro dele — lê melhor e não desloca o texto no foco;
 * - o foco acende um anel externo violeta animado, em vez de só trocar a cor da borda;
 * - há slot de erro com entrada animada, então a mensagem não empurra o layout de repente;
 * - [mono] troca para JetBrains Mono, para senhas e chaves onde distinguir `l`/`I`/`1` importa.
 */
@Composable
internal fun OrionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    error: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    mono: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val isError = error != null

    val glow by animateFloatAsState(
        targetValue = if (focused && !isError) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "focusGlow"
    )

    val textStyle: TextStyle =
        if (mono) OrionTheme.code else MaterialTheme.typography.bodyLarge

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isError) scheme.error else scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = OrionSpacing.xxs + 2.dp)
            )
        }

        // O anel externo é desenhado num wrapper com folga de 3dp: com alfa 0 ele simplesmente
        // desaparece, sem reservar nem devolver espaço quando o foco muda.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = scheme.primary.copy(alpha = 0.30f * glow),
                    shape = RoundedCornerShape(13.dp)
                )
                .padding(3.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                placeholder = placeholder?.let { { Text(it, style = MaterialTheme.typography.bodyLarge) } },
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                isError = isError,
                singleLine = singleLine,
                minLines = minLines,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                textStyle = textStyle,
                shape = OrionShapes.Input,
                interactionSource = interactionSource,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = scheme.surfaceContainerLowest,
                    unfocusedContainerColor = scheme.surfaceContainerLowest,
                    disabledContainerColor = scheme.surfaceContainerLowest.copy(alpha = 0.5f),
                    errorContainerColor = scheme.surfaceContainerLowest,
                    focusedBorderColor = scheme.primary,
                    unfocusedBorderColor = scheme.outlineVariant,
                    disabledBorderColor = scheme.outlineVariant.copy(alpha = 0.5f),
                    errorBorderColor = scheme.error,
                    focusedTextColor = scheme.onSurface,
                    unfocusedTextColor = scheme.onSurface,
                    disabledTextColor = scheme.onSurfaceVariant,
                    cursorColor = scheme.primary,
                    focusedPlaceholderColor = OrionTheme.ext.textMuted,
                    unfocusedPlaceholderColor = OrionTheme.ext.textMuted,
                    focusedLeadingIconColor = scheme.primary,
                    unfocusedLeadingIconColor = scheme.onSurfaceVariant,
                    focusedTrailingIconColor = scheme.primary,
                    unfocusedTrailingIconColor = scheme.onSurfaceVariant
                )
            )
        }

        AnimatedVisibility(
            visible = isError,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = error.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.error,
                modifier = Modifier.padding(top = OrionSpacing.xxs, start = OrionSpacing.xxs)
            )
        }
    }
}
