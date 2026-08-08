package com.cuboidestudio.orionvault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle

private val OrionColorScheme = darkColorScheme(
    primary = OrionColors.Primary,
    onPrimary = OrionColors.OnPrimary,
    primaryContainer = OrionColors.PrimaryContainer,
    onPrimaryContainer = OrionColors.OnPrimaryContainer,
    inversePrimary = OrionColors.InversePrimary,
    secondary = OrionColors.Secondary,
    onSecondary = OrionColors.OnSecondary,
    secondaryContainer = OrionColors.SecondaryContainer,
    onSecondaryContainer = OrionColors.OnSecondaryContainer,
    tertiary = OrionColors.Tertiary,
    onTertiary = OrionColors.OnTertiary,
    tertiaryContainer = OrionColors.TertiaryContainer,
    onTertiaryContainer = OrionColors.OnTertiaryContainer,
    background = OrionColors.Canvas,
    onBackground = OrionColors.OnSurface,
    surface = OrionColors.Canvas,
    onSurface = OrionColors.OnSurface,
    surfaceVariant = OrionColors.SurfaceContainerHighest,
    onSurfaceVariant = OrionColors.OnSurfaceVariant,
    surfaceTint = OrionColors.Primary,
    inverseSurface = OrionColors.InverseSurface,
    inverseOnSurface = OrionColors.InverseOnSurface,
    error = OrionColors.Error,
    onError = OrionColors.OnError,
    errorContainer = OrionColors.ErrorContainer,
    onErrorContainer = OrionColors.OnErrorContainer,
    outline = OrionColors.Outline,
    outlineVariant = OrionColors.OutlineVariant,
    scrim = OrionColors.Scrim,
    surfaceBright = OrionColors.SurfaceBright,
    surfaceDim = OrionColors.Canvas,
    surfaceContainerLowest = OrionColors.SurfaceContainerLowest,
    surfaceContainerLow = OrionColors.SurfaceContainerLow,
    surfaceContainer = OrionColors.SurfaceContainer,
    surfaceContainerHigh = OrionColors.SurfaceContainerHigh,
    surfaceContainerHighest = OrionColors.SurfaceContainerHighest
)

/**
 * Atalhos para os tokens que ficam fora do Material 3.
 *
 * `MaterialTheme.colorScheme` continua sendo a via principal de cor; [ext] cobre só o que o M3 não
 * modela (vidro, halos, rampa de força de senha) e [type] cobre os estilos monoespaçados.
 */
internal object OrionTheme {
    val ext: OrionExtendedColors
        @Composable @ReadOnlyComposable get() = LocalOrionColors.current

    val type: OrionTypeExtras
        @Composable @ReadOnlyComposable get() = LocalOrionTypeExtras.current

    /** Estilo monoespaçado para senhas e outros dados sensíveis. */
    val code: TextStyle
        @Composable @ReadOnlyComposable get() = LocalOrionTypeExtras.current.code
}

@Composable
fun OrionVaultTheme(content: @Composable () -> Unit) {
    val sans = orionSansFamily()
    val mono = orionMonoFamily()

    CompositionLocalProvider(
        LocalOrionColors provides OrionDarkExtendedColors,
        LocalOrionTypeExtras provides orionTypeExtras(mono)
    ) {
        MaterialTheme(
            colorScheme = OrionColorScheme,
            typography = orionTypography(sans),
            shapes = OrionMaterialShapes,
            content = content
        )
    }
}
