package com.cuboidestudio.orionvault.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Escala de espaçamento em incrementos de 4dp, conforme o design system.
 *
 * Existe para acabar com os literais duplicados por arquivo: antes, `64.dp`, `52.dp` e cinco variações
 * de `widthIn(max = …)` apareciam repetidos em cada tela, sem nenhuma fonte comum.
 */
internal object OrionSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp

    /** Margem lateral do conteúdo. */
    val screenH = 20.dp
}

/** Dimensões recorrentes de componentes. */
internal object OrionSizes {
    val topBar = 64.dp
    val buttonLg = 52.dp
    val buttonMd = 44.dp
    val field = 56.dp
    val avatar = 44.dp
    val avatarSm = 36.dp
    val iconSm = 18.dp
    val icon = 22.dp
    val fabMargin = 20.dp

    /** Folga no fim das listas para o FAB não cobrir o último item. */
    val listBottom = 104.dp

    /** Larguras máximas de coluna — o conteúdo centraliza acima disso. */
    val contentNarrow = 420.dp // formulários de uma coluna (unlock, onboarding)
    val contentForm = 560.dp // editores
    val contentWide = 960.dp // listas do cofre

    /** Largura mínima de um card de pasta na grade adaptativa. */
    val folderCellMin = 168.dp
}
