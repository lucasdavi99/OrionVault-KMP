package com.cuboidestudio.orionvault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

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
    background = OrionColors.Background,
    onBackground = OrionColors.OnBackground,
    surface = OrionColors.Surface,
    onSurface = OrionColors.OnSurface,
    surfaceVariant = OrionColors.SurfaceVariant,
    onSurfaceVariant = OrionColors.OnSurfaceVariant,
    surfaceTint = OrionColors.SurfaceTint,
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
    surfaceDim = OrionColors.SurfaceDim,
    surfaceContainer = OrionColors.SurfaceContainer,
    surfaceContainerHigh = OrionColors.SurfaceContainerHigh,
    surfaceContainerHighest = OrionColors.SurfaceContainerHighest,
    surfaceContainerLow = OrionColors.SurfaceContainerLow,
    surfaceContainerLowest = OrionColors.SurfaceContainerLowest,
    primaryFixed = OrionColors.PrimaryFixed,
    primaryFixedDim = OrionColors.PrimaryFixedDim,
    onPrimaryFixed = OrionColors.OnPrimaryFixed,
    onPrimaryFixedVariant = OrionColors.OnPrimaryFixedVariant,
    secondaryFixed = OrionColors.SecondaryFixed,
    secondaryFixedDim = OrionColors.SecondaryFixedDim,
    onSecondaryFixed = OrionColors.OnSecondaryFixed,
    onSecondaryFixedVariant = OrionColors.OnSecondaryFixedVariant,
    tertiaryFixed = OrionColors.TertiaryFixed,
    tertiaryFixedDim = OrionColors.TertiaryFixedDim,
    onTertiaryFixed = OrionColors.OnTertiaryFixed,
    onTertiaryFixedVariant = OrionColors.OnTertiaryFixedVariant
)

@Composable
fun OrionVaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OrionColorScheme,
        typography = OrionTypography,
        shapes = OrionMaterialShapes,
        content = content
    )
}
