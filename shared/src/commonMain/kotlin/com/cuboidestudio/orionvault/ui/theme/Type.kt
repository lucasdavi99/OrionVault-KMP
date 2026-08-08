package com.cuboidestudio.orionvault.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import orionvault.shared.generated.resources.Res
import orionvault.shared.generated.resources.ibm_plex_sans_bold
import orionvault.shared.generated.resources.ibm_plex_sans_medium
import orionvault.shared.generated.resources.ibm_plex_sans_regular
import orionvault.shared.generated.resources.ibm_plex_sans_semibold
import orionvault.shared.generated.resources.jetbrains_mono_medium
import orionvault.shared.generated.resources.jetbrains_mono_regular
import org.jetbrains.compose.resources.Font

/**
 * IBM Plex Sans — a fonte de interface.
 *
 * Escolhida por desambiguação de caracteres: o 'l' minúsculo tem cauda curva, o 'I' maiúsculo é uma
 * barra reta e o '1' tem base. Num cofre de senhas isso não é preciosismo tipográfico — é a diferença
 * entre transcrever uma credencial certa ou errada.
 */
@Composable
internal fun orionSansFamily(): FontFamily {
    val regular = Font(Res.font.ibm_plex_sans_regular, FontWeight.Normal)
    val medium = Font(Res.font.ibm_plex_sans_medium, FontWeight.Medium)
    val semiBold = Font(Res.font.ibm_plex_sans_semibold, FontWeight.SemiBold)
    val bold = Font(Res.font.ibm_plex_sans_bold, FontWeight.Bold)
    return remember(regular, medium, semiBold, bold) {
        FontFamily(regular, medium, semiBold, bold)
    }
}

/**
 * JetBrains Mono — reservada a dados sensíveis (senhas, chave secreta).
 *
 * Zero cortado e caracteres largos: desenhada justamente para leitura caractere a caractere.
 */
@Composable
internal fun orionMonoFamily(): FontFamily {
    val regular = Font(Res.font.jetbrains_mono_regular, FontWeight.Normal)
    val medium = Font(Res.font.jetbrains_mono_medium, FontWeight.Medium)
    return remember(regular, medium) { FontFamily(regular, medium) }
}

/**
 * Estilos monoespaçados, que não têm slot correspondente no Material 3.
 * Acesso via [OrionTheme.type].
 */
@Immutable
data class OrionTypeExtras(
    /** Senha em campo/linha de lista. */
    val code: TextStyle,
    /** Chave secreta e outros blocos que o usuário vai transcrever à mão. Tracking generoso. */
    val codeLarge: TextStyle
)

/**
 * Preenche os 15 slots do Material 3 — a tipografia anterior deixava `display*` e `titleSmall`
 * nos defaults, então qualquer componente que os usasse fugia da escala.
 *
 * Headlines levam tracking negativo para ficarem compactas e "travadas", conforme o design system.
 */
@Composable
internal fun orionTypography(sans: FontFamily): Typography = remember(sans) {
    Typography(
        displayLarge = TextStyle(sans, FontWeight.Bold, 40.sp, 48.sp, (-0.03).em),
        displayMedium = TextStyle(sans, FontWeight.Bold, 36.sp, 44.sp, (-0.025).em),
        displaySmall = TextStyle(sans, FontWeight.Bold, 32.sp, 40.sp, (-0.02).em),
        headlineLarge = TextStyle(sans, FontWeight.Bold, 30.sp, 38.sp, (-0.02).em),
        headlineMedium = TextStyle(sans, FontWeight.SemiBold, 24.sp, 32.sp, (-0.015).em),
        headlineSmall = TextStyle(sans, FontWeight.SemiBold, 20.sp, 28.sp, (-0.01).em),
        titleLarge = TextStyle(sans, FontWeight.SemiBold, 18.sp, 26.sp, (-0.01).em),
        titleMedium = TextStyle(sans, FontWeight.SemiBold, 16.sp, 24.sp),
        titleSmall = TextStyle(sans, FontWeight.SemiBold, 14.sp, 20.sp),
        bodyLarge = TextStyle(sans, FontWeight.Normal, 16.sp, 24.sp),
        bodyMedium = TextStyle(sans, FontWeight.Normal, 14.sp, 21.sp),
        bodySmall = TextStyle(sans, FontWeight.Normal, 12.sp, 18.sp),
        labelLarge = TextStyle(sans, FontWeight.SemiBold, 14.sp, 20.sp, 0.01.em),
        labelMedium = TextStyle(sans, FontWeight.SemiBold, 13.sp, 18.sp, 0.02.em),
        // Tracking largo: este slot carrega os cabeçalhos de seção em caixa alta.
        labelSmall = TextStyle(sans, FontWeight.SemiBold, 12.sp, 16.sp, 0.06.em)
    )
}

@Composable
internal fun orionTypeExtras(mono: FontFamily): OrionTypeExtras = remember(mono) {
    OrionTypeExtras(
        code = TextStyle(mono, FontWeight.Medium, 14.sp, 20.sp, 0.02.em),
        codeLarge = TextStyle(mono, FontWeight.Medium, 18.sp, 30.sp, 0.08.em)
    )
}

private fun TextStyle(
    family: FontFamily,
    weight: FontWeight,
    size: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    tracking: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = tracking
)

internal val LocalOrionTypeExtras = staticCompositionLocalOf {
    OrionTypeExtras(code = TextStyle.Default, codeLarge = TextStyle.Default)
}
