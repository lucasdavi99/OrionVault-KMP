package com.cuboidestudio.orionvault.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tokens crus da paleta "Obsidiana + Violeta Elétrico".
 *
 * Esta é a única fonte de verdade das cores, e existe só para alimentar o [androidx.compose.material3.ColorScheme]
 * montado em [OrionVaultTheme]. **Telas e componentes não devem importar este objeto** — devem ler de
 * `MaterialTheme.colorScheme` ou de [OrionTheme.ext]. Foi exatamente o acesso direto a este objeto que
 * tornava o tema anterior decorativo: trocar a paleta não propagava para lugar nenhum.
 *
 * Os índices de contraste anotados são medidos contra [Canvas] (WCAG 2.1, texto normal exige 4.5:1).
 */
internal object OrionColors {
    // --- Canvas neutro -----------------------------------------------------------------------
    /** Fundo mais profundo. Neutro de verdade — é o que faz os acentos "acenderem". */
    val Canvas = Color(0xFF0A0A0F)
    val SurfaceContainerLowest = Color(0xFF070709) // fundo de inputs: mais escuro que o pai
    val SurfaceContainerLow = Color(0xFF101017)
    val SurfaceContainer = Color(0xFF12121A) // card base
    val SurfaceContainerHigh = Color(0xFF1B1B26) // card elevado, diálogos
    val SurfaceContainerHighest = Color(0xFF242432)
    val SurfaceBright = Color(0xFF2C2C3B)

    // --- Texto e traços ----------------------------------------------------------------------
    val OnSurface = Color(0xFFF4F4F7) // 18:1
    val OnSurfaceVariant = Color(0xFFA1A1B5) // 8.6:1
    val Outline = Color(0xFF3A3A4A)
    val OutlineVariant = Color(0xFF26262F)

    // --- Primária: violeta (ação, foco, marca) -----------------------------------------------
    val Primary = Color(0xFFA78BFA) // 7.2:1 — ícones, links, foco sobre o canvas
    val OnPrimary = Color(0xFF21103F)

    /**
     * Preenchimento do botão primário. Escurecido em relação ao #7C5CFA original: aquele media
     * apenas 3.7:1 com o texto claro por cima, abaixo do mínimo AA. Este mede 5.5:1 com branco.
     */
    val PrimaryContainer = Color(0xFF6D46F2)
    val OnPrimaryContainer = Color(0xFFFFFFFF)
    val InversePrimary = Color(0xFF5B34D6) // fim do gradiente do botão

    // --- Secundária: esmeralda (APENAS seguro / verificado / copiado / força alta) ------------
    val Secondary = Color(0xFF34D399) // 10.2:1
    val OnSecondary = Color(0xFF04291C)
    val SecondaryContainer = Color(0xFF0E4535)
    val OnSecondaryContainer = Color(0xFF6EE7B7)

    // --- Terciária: âmbar (aviso, força média) -----------------------------------------------
    val Tertiary = Color(0xFFFBBF24)
    val OnTertiary = Color(0xFF2E1F00)
    val TertiaryContainer = Color(0xFF4A3306)
    val OnTertiaryContainer = Color(0xFFFDE68A)

    // --- Erro: vermelho real (o #FFB4AB anterior era lavado demais para significar perigo) ----
    val Error = Color(0xFFF87171) // 7.1:1
    val OnError = Color(0xFF2E0A0A)
    val ErrorContainer = Color(0xFF4C1414)
    val OnErrorContainer = Color(0xFFFCA5A5)

    // --- Inversos e scrim --------------------------------------------------------------------
    val InverseSurface = Color(0xFFF4F4F7)
    val InverseOnSurface = Color(0xFF12121A)
    val Scrim = Color(0xFF000000)
}

/**
 * Tokens que não têm slot no Material 3 e por isso viajam num CompositionLocal próprio,
 * em vez de ficarem soltos num `object` global (era o caso de `GlassPanel`/`GlassPanelBorder`).
 *
 * Acesso via [OrionTheme.ext].
 */
@Immutable
data class OrionExtendedColors(
    /** Texto de apoio, apenas para rótulos grandes ou não essenciais — mede 4.2:1. */
    val textMuted: Color,
    /** Topo do gradiente de preenchimento dos cards. */
    val cardFillTop: Color,
    val cardFillBottom: Color,
    /** Borda em gradiente: mais clara no topo, simulando o brilho interno do vidro. */
    val cardBorderTop: Color,
    val cardBorderBottom: Color,
    /** Halos do fundo ambiente. Já vêm com alfa aplicado. */
    val ambientViolet: Color,
    val ambientEmerald: Color,
    /** Rampa do medidor de força de senha, do mais fraco ao mais forte. */
    val strengthWeak: Color,
    val strengthFair: Color,
    val strengthGood: Color,
    /** Reservada exclusivamente para senha com score 100% (força máxima). */
    val strengthPerfect: Color,
    /** Gradientes determinísticos (início, fim) para avatares de item, escolhidos por hash do título. */
    val avatarGradients: List<Pair<Color, Color>>
)

internal val OrionDarkExtendedColors = OrionExtendedColors(
    textMuted = Color(0xFF6E6E85),
    cardFillTop = Color(0xFF1A1A26),
    cardFillBottom = Color(0xFF121219),
    cardBorderTop = Color(0x1FFFFFFF), // branco 12%
    cardBorderBottom = Color(0x0AFFFFFF), // branco 4%
    ambientViolet = Color(0x1A7C5CFA), // violeta 10%
    ambientEmerald = Color(0x0F10B981), // esmeralda 6%
    strengthWeak = Color(0xFFF87171),
    strengthFair = Color(0xFFFBBF24),
    strengthGood = Color(0xFF34D399),
    strengthPerfect = Color(0xFF8B5CF6),
    avatarGradients = listOf(
        Color(0xFF8B5CF6) to Color(0xFF6366F1), // violeta → indigo
        Color(0xFF06B6D4) to Color(0xFF3B82F6), // ciano → azul
        Color(0xFF10B981) to Color(0xFF059669), // esmeralda
        Color(0xFFF59E0B) to Color(0xFFEA580C), // âmbar → laranja
        Color(0xFFEC4899) to Color(0xFF8B5CF6), // rosa → violeta
        Color(0xFF14B8A6) to Color(0xFF0E7490), // teal
        Color(0xFF6366F1) to Color(0xFF4338CA), // indigo profundo
        Color(0xFFF43F5E) to Color(0xFFBE123C) // rosa → carmim
    )
)

internal val LocalOrionColors = staticCompositionLocalOf { OrionDarkExtendedColors }
