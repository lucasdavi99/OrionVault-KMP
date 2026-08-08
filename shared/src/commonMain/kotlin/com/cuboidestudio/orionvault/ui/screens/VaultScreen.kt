package com.cuboidestudio.orionvault.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cuboidestudio.orionvault.domain.model.VaultFolder
import com.cuboidestudio.orionvault.domain.model.VaultItem
import com.cuboidestudio.orionvault.ui.components.ItemAvatar
import com.cuboidestudio.orionvault.ui.components.OrionEmptyState
import com.cuboidestudio.orionvault.ui.components.OrionLargeTopBar
import com.cuboidestudio.orionvault.ui.components.OrionScaffold
import com.cuboidestudio.orionvault.ui.components.OrionSectionHeader
import com.cuboidestudio.orionvault.ui.components.OrionSurface
import com.cuboidestudio.orionvault.ui.components.OrionTextField
import com.cuboidestudio.orionvault.ui.theme.OrionShapes
import com.cuboidestudio.orionvault.ui.theme.OrionSizes
import com.cuboidestudio.orionvault.ui.theme.OrionSpacing
import com.cuboidestudio.orionvault.viewmodel.VaultViewModel
import kotlinx.coroutines.launch

private const val TAB_ACCOUNTS = 0
private const val TAB_FOLDERS = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onOpenItem: (folderId: String?, itemId: String?) -> Unit,
    onOpenFolder: (parentId: String?, folderId: String?) -> Unit,
    onOpenAccount: () -> Unit
) {
    val unfiledItems by viewModel.unfiledItems.collectAsState()
    val breadcrumb by viewModel.breadcrumb.collectAsState()
    val subfolders by viewModel.subfolders.collectAsState()
    val items by viewModel.items.collectAsState()
    val folderId = viewModel.currentFolderId

    var searchVisible by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val pagerState = rememberPagerState(initialPage = TAB_ACCOUNTS) { 2 }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val filteredUnfiledItems = remember(unfiledItems, query) { unfiledItems.matching(query) }
    val filteredItems = remember(items, query) { items.matching(query) }
    val filteredFolders = remember(subfolders, query) {
        if (query.isBlank()) subfolders else subfolders.filter { it.name.contains(query, ignoreCase = true) }
    }

    OrionScaffold(
        snackbarHostState = snackbarHostState,
        topBar = {
            Column(modifier = Modifier.background(Color.Transparent)) {
                OrionLargeTopBar(
                    title = "OrionVault",
                    scrollBehavior = scrollBehavior,
                    navigationIcon = { VaultBrandMark() },
                    actions = {
                        IconButton(onClick = {
                            searchVisible = !searchVisible
                            if (!searchVisible) query = ""
                        }) {
                            Icon(
                                imageVector = if (searchVisible) Icons.Filled.Close else Icons.Filled.Search,
                                contentDescription = if (searchVisible) "Fechar busca" else "Buscar"
                            )
                        }
                        IconButton(onClick = onOpenAccount) {
                            Icon(Icons.Filled.CloudSync, contentDescription = "Conta e sincronização")
                        }
                    }
                )

                AnimatedVisibility(
                    visible = searchVisible,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    OrionTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = OrionSpacing.screenH, vertical = OrionSpacing.xs),
                        placeholder = "Buscar no cofre...",
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
                    )
                }

                VaultTabs(
                    selectedPage = pagerState.currentPage,
                    onSelect = { page -> scope.launch { pagerState.animateScrollToPage(page) } }
                )
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) { page ->
            when (page) {
                TAB_ACCOUNTS -> AccountsTab(
                    items = filteredUnfiledItems,
                    hasAnyItem = unfiledItems.isNotEmpty(),
                    query = query,
                    snackbarHostState = snackbarHostState,
                    onOpenItem = onOpenItem
                )

                else -> FoldersTab(
                    breadcrumb = breadcrumb,
                    folders = filteredFolders,
                    items = filteredItems,
                    isEmpty = subfolders.isEmpty() && items.isEmpty(),
                    query = query,
                    folderId = folderId,
                    snackbarHostState = snackbarHostState,
                    onNavigateInto = viewModel::navigateInto,
                    onNavigateUp = viewModel::navigateUp,
                    onOpenFolder = onOpenFolder,
                    onOpenItem = onOpenItem
                )
            }
        }
    }
}

private fun List<VaultItem>.matching(query: String): List<VaultItem> =
    if (query.isBlank()) {
        this
    } else {
        filter {
            it.title.contains(query, ignoreCase = true) ||
                it.username?.contains(query, ignoreCase = true) == true
        }
    }

/** Selo do cadeado que acompanha o wordmark na top bar. */
@Composable
private fun VaultBrandMark() {
    Box(
        modifier = Modifier
            .padding(start = OrionSpacing.sm)
            .size(34.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(OrionSizes.iconSm)
        )
    }
}

@Composable
private fun VaultTabs(selectedPage: Int, onSelect: (Int) -> Unit) {
    // O indicador padrão do PrimaryTabRow já desliza animado entre as abas e herda o contentColor;
    // só a divisória é removida, para a barra flutuar sobre o fundo ambiente.
    PrimaryTabRow(
        selectedTabIndex = selectedPage,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {}
    ) {
        VaultTab("Contas", selectedPage == TAB_ACCOUNTS) { onSelect(TAB_ACCOUNTS) }
        VaultTab("Pastas", selectedPage == TAB_FOLDERS) { onSelect(TAB_FOLDERS) }
    }
}

@Composable
private fun VaultTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val color by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "tabColor"
    )
    Tab(
        selected = selected,
        onClick = onClick,
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = color
            )
        }
    )
}

@Composable
private fun AccountsTab(
    items: List<VaultItem>,
    hasAnyItem: Boolean,
    query: String,
    snackbarHostState: SnackbarHostState,
    onOpenItem: (folderId: String?, itemId: String?) -> Unit
) {
    val listState = rememberLazyGridState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            items.isEmpty() && query.isNotBlank() -> NoSearchResults(query)

            items.isEmpty() -> OrionEmptyState(
                icon = Icons.Filled.VerifiedUser,
                title = "Nenhuma conta ainda",
                description = "Contas que não estão em nenhuma pasta aparecem aqui.",
                actionLabel = "Adicionar conta",
                actionIcon = Icons.Filled.Add,
                onAction = { onOpenItem(null, null) }
            )

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                state = listState,
                modifier = Modifier.fillMaxSize().widthIn(max = OrionSizes.contentWide),
                contentPadding = vaultListPadding(),
                verticalArrangement = Arrangement.spacedBy(OrionSpacing.xs)
            ) {
                items(items, key = { it.id }) { vaultItem ->
                    ItemRow(
                        item = vaultItem,
                        snackbarHostState = snackbarHostState,
                        modifier = Modifier.animateItem(),
                        onClick = { onOpenItem(vaultItem.folderId, vaultItem.id) }
                    )
                }
            }
        }

        if (hasAnyItem || query.isNotBlank()) {
            VaultFab(
                label = "Nova conta",
                icon = Icons.Filled.Add,
                expanded = fabExpanded,
                onClick = { onOpenItem(null, null) },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun FoldersTab(
    breadcrumb: List<VaultFolder>,
    folders: List<VaultFolder>,
    items: List<VaultItem>,
    isEmpty: Boolean,
    query: String,
    folderId: String?,
    snackbarHostState: SnackbarHostState,
    onNavigateInto: (VaultFolder) -> Unit,
    onNavigateUp: () -> Unit,
    onOpenFolder: (parentId: String?, folderId: String?) -> Unit,
    onOpenItem: (folderId: String?, itemId: String?) -> Unit
) {
    val gridState = rememberLazyGridState()
    val fabExpanded by remember { derivedStateOf { gridState.firstVisibleItemIndex == 0 } }
    val atRoot = breadcrumb.isEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!atRoot) {
                FolderBreadcrumb(current = breadcrumb.last().name, onNavigateUp = onNavigateUp)
            }

            when {
                folders.isEmpty() && items.isEmpty() && query.isNotBlank() -> NoSearchResults(query)

                isEmpty -> OrionEmptyState(
                    icon = if (atRoot) Icons.Filled.Lock else Icons.Filled.Folder,
                    title = if (atRoot) "Seu cofre está vazio" else breadcrumb.last().name,
                    description = if (atRoot) {
                        "Crie uma pasta para começar a organizar suas credenciais com criptografia de ponta a ponta."
                    } else {
                        "Esta pasta está vazia. Adicione o primeiro item para começar a protegê-lo."
                    },
                    actionLabel = if (atRoot) "Criar primeira pasta" else "Adicionar primeiro item",
                    actionIcon = if (atRoot) Icons.Filled.CreateNewFolder else Icons.Filled.Add,
                    onAction = {
                        if (atRoot) onOpenFolder(folderId, null) else onOpenItem(folderId, null)
                    }
                )

                // Uma única grade adaptativa cuida de tudo: as pastas ocupam uma célula cada e os
                // cabeçalhos e itens ocupam a linha inteira. Substitui o `chunked()` manual dentro de
                // uma LazyColumn, que recalculava as colunas à mão a partir de um BoxWithConstraints.
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = OrionSizes.folderCellMin),
                    state = gridState,
                    modifier = Modifier.fillMaxSize().widthIn(max = OrionSizes.contentWide),
                    contentPadding = vaultListPadding(),
                    verticalArrangement = Arrangement.spacedBy(OrionSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(OrionSpacing.sm)
                ) {
                    if (folders.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "header-folders") {
                            OrionSectionHeader("Pastas")
                        }
                        items(folders, key = { it.id }, span = { GridItemSpan(1) }) { folder ->
                            FolderCard(
                                folder = folder,
                                modifier = Modifier.animateItem(),
                                onClick = { onNavigateInto(folder) },
                                onEdit = { onOpenFolder(folder.parentId, folder.id) }
                            )
                        }
                    }

                    if (items.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "header-items") {
                            OrionSectionHeader(
                                label = "Itens",
                                modifier = Modifier.padding(top = OrionSpacing.xs)
                            )
                        }
                        items(items, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) { vaultItem ->
                            ItemRow(
                                item = vaultItem,
                                snackbarHostState = snackbarHostState,
                                modifier = Modifier.animateItem(),
                                onClick = { onOpenItem(vaultItem.folderId, vaultItem.id) }
                            )
                        }
                    }
                }
            }
        }

        if (!isEmpty || query.isNotBlank()) {
            VaultFab(
                label = if (atRoot) "Nova pasta" else "Nova conta",
                icon = if (atRoot) Icons.Filled.CreateNewFolder else Icons.Filled.Add,
                expanded = fabExpanded,
                onClick = {
                    if (atRoot) onOpenFolder(folderId, null) else onOpenItem(folderId, null)
                },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun vaultListPadding() = PaddingValues(
    start = OrionSpacing.screenH,
    end = OrionSpacing.screenH,
    top = OrionSpacing.xs,
    bottom = OrionSizes.listBottom
)

@Composable
private fun VaultFab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        expanded = expanded,
        icon = { Icon(icon, contentDescription = null) },
        text = { Text(label, style = MaterialTheme.typography.labelLarge) },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.padding(OrionSizes.fabMargin)
    )
}

@Composable
private fun FolderBreadcrumb(current: String, onNavigateUp: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = OrionSpacing.sm, vertical = OrionSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OrionSpacing.xxs)
    ) {
        IconButton(onClick = onNavigateUp) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = current,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NoSearchResults(query: String) {
    OrionEmptyState(
        icon = Icons.Filled.Search,
        title = "Nada encontrado",
        description = "Nenhum resultado para \"$query\" nesta aba."
    )
}

@Composable
private fun FolderCard(
    folder: VaultFolder,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    OrionSurface(
        modifier = modifier.fillMaxWidth(),
        shape = OrionShapes.Card,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(OrionSizes.avatarSm)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        OrionShapes.Input
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(OrionSizes.iconSm)
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Editar pasta",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(OrionSizes.iconSm)
                )
            }
        }

        Spacer(Modifier.height(OrionSpacing.md))

        Text(
            text = folder.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ItemRow(
    item: VaultItem,
    snackbarHostState: SnackbarHostState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var justCopied by remember { mutableStateOf(false) }

    val copyTint by animateColorAsState(
        targetValue = if (justCopied) {
            MaterialTheme.colorScheme.secondary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "copyTint"
    )

    LaunchedEffect(justCopied) {
        if (justCopied) {
            kotlinx.coroutines.delay(1500)
            justCopied = false
        }
    }

    OrionSurface(
        modifier = modifier.fillMaxWidth(),
        shape = OrionShapes.Card,
        padding = PaddingValues(horizontal = OrionSpacing.sm, vertical = OrionSpacing.sm),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OrionSpacing.sm)
            ) {
                ItemAvatar(title = item.title)

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OrionSpacing.xxs + 2.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = "Criptografado",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    val subtitle = item.username?.takeIf { it.isNotBlank() }
                        ?: item.email?.takeIf { it.isNotBlank() }
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(onClick = {
                clipboard.setText(AnnotatedString(item.password))
                justCopied = true
                scope.launch {
                    snackbarHostState.showSnackbar("Senha de \"${item.title}\" copiada")
                }
            }) {
                Icon(
                    imageVector = if (justCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                    contentDescription = "Copiar senha",
                    tint = copyTint,
                    modifier = Modifier.size(OrionSizes.icon)
                )
            }
        }
    }
}
