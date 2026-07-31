package com.cuboidestudio.orionvault.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import com.cuboidestudio.orionvault.ui.theme.OrionColors
import com.cuboidestudio.orionvault.ui.theme.OrionShapes

/**
 * Text field styled after the ".input-vault" pattern: near-black container,
 * hairline outline-variant border, primary glow on focus.
 */
@Composable
fun VaultTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        singleLine = singleLine,
        minLines = minLines,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        textStyle = textStyle,
        shape = OrionShapes.Input,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = OrionColors.SurfaceContainerLowest,
            unfocusedContainerColor = OrionColors.SurfaceContainerLowest,
            disabledContainerColor = OrionColors.SurfaceContainerLowest,
            focusedBorderColor = OrionColors.Primary,
            unfocusedBorderColor = OrionColors.OutlineVariant,
            focusedTextColor = OrionColors.OnSurface,
            unfocusedTextColor = OrionColors.OnSurface,
            cursorColor = OrionColors.Primary,
            focusedLabelColor = OrionColors.Primary,
            unfocusedLabelColor = OrionColors.OnSurfaceVariant,
            focusedPlaceholderColor = OrionColors.Outline,
            unfocusedPlaceholderColor = OrionColors.Outline
        )
    )
}
