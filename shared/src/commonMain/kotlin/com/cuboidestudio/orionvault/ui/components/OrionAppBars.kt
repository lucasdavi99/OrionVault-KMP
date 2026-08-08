package com.cuboidestudio.orionvault.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Top bar padrão: container transparente, para o fundo ambiente aparecer através dela.
 *
 * Substitui o `VaultTopBar` (um `Row` de 64dp escrito à mão) e mais três cabeçalhos idênticos
 * duplicados dentro das telas. Sendo uma `TopAppBar` de verdade, ganha comportamento de rolagem,
 * insets de sistema e semântica corretos.
 *
 * A divisória inferior só aparece quando há conteúdo passando por baixo — quando [scrollBehavior]
 * é informado, seu alfa acompanha a fração de sobreposição.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrionTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    showDivider: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val dividerAlpha = when {
        !showDivider -> 0f
        scrollBehavior != null -> scrollBehavior.state.overlappedFraction.coerceIn(0f, 1f)
        else -> 1f
    }

    Box(modifier = modifier) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            },
            actions = actions,
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                actionIconContentColor = MaterialTheme.colorScheme.primary
            )
        )
        HorizontalDivider(
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomStart),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = dividerAlpha)
        )
    }
}

/** Variante grande, para a tela principal: o título colapsa conforme a lista rola. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrionLargeTopBar(
    title: String,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    LargeTopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            actionIconContentColor = MaterialTheme.colorScheme.primary
        )
    )
}

/**
 * Scaffold do app: [Scaffold] do Material 3 desenhado por cima do [OrionBackground].
 *
 * O projeto não tinha nenhum `Scaffold` — cada tela montava sua própria `Column` e ignorava insets de
 * sistema, o que na prática significava conteúdo por baixo da barra de status no Android.
 */
@Composable
internal fun OrionScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    backgroundIntensity: Float = 1f,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit
) {
    OrionBackground(modifier = modifier, intensity = backgroundIntensity) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            topBar = topBar,
            bottomBar = bottomBar,
            floatingActionButton = floatingActionButton,
            snackbarHost = {
                if (snackbarHostState != null) SnackbarHost(hostState = snackbarHostState)
            },
            contentWindowInsets = contentWindowInsets,
            content = content
        )
    }
}
