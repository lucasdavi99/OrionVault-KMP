package com.cuboidestudio.orionvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cuboidestudio.orionvault.domain.model.VaultFolder
import com.cuboidestudio.orionvault.domain.model.VaultItem
import com.cuboidestudio.orionvault.ui.components.GlassPanel
import com.cuboidestudio.orionvault.ui.components.VaultTextField
import com.cuboidestudio.orionvault.ui.theme.OrionColors
import com.cuboidestudio.orionvault.viewmodel.VaultViewModel
import kotlinx.coroutines.delay

@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onOpenItem: (folderId: String, itemId: String?) -> Unit,
    onOpenFolder: (parentId: String?, folderId: String?) -> Unit
) {
    val breadcrumb by viewModel.breadcrumb.collectAsState()
    val subfolders by viewModel.subfolders.collectAsState()
    val items by viewModel.items.collectAsState()
    val folderId = viewModel.currentFolderId

    var searchVisible by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val filteredFolders = remember(subfolders, query) {
        if (query.isBlank()) subfolders else subfolders.filter { it.name.contains(query, ignoreCase = true) }
    }
    val filteredItems = remember(items, query) {
        if (query.isBlank()) items else items.filter {
            it.title.contains(query, ignoreCase = true) || it.username?.contains(query, ignoreCase = true) == true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(OrionColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (breadcrumb.isEmpty()) {
                        Box(
                            modifier = Modifier.size(32.dp).background(OrionColors.SurfaceContainerHigh, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = OrionColors.Primary, modifier = Modifier.size(16.dp))
                        }
                        Text("OrionVault", style = MaterialTheme.typography.headlineMedium, color = OrionColors.Primary)
                    } else {
                        IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = OrionColors.Primary)
                        }
                        Text(
                            breadcrumb.last().name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = OrionColors.Primary
                        )
                    }
                }
                IconButton(onClick = {
                    searchVisible = !searchVisible
                    if (!searchVisible) query = ""
                }) {
                    Icon(
                        if (searchVisible) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = "Buscar",
                        tint = OrionColors.Primary
                    )
                }
            }

            if (searchVisible) {
                VaultTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = "Buscar no cofre...",
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = OrionColors.Outline) }
                )
            }

            val isEmpty = subfolders.isEmpty() && items.isEmpty()
            if (isEmpty) {
                VaultEmptyState(
                    isRoot = breadcrumb.isEmpty(),
                    onCreateFolder = { onOpenFolder(folderId, null) },
                    onAddItem = { folderId?.let { onOpenItem(it, null) } }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().widthIn(max = 900.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredFolders.isNotEmpty()) {
                        item {
                            SectionHeader(
                                label = "FOLDERS",
                                onAdd = { onOpenFolder(folderId, null) }
                            )
                        }
                        items(filteredFolders.chunked(2), key = { row -> row.first().id }) { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { folder ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        FolderCard(folder) { viewModel.navigateInto(folder) }
                                    }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    } else if (breadcrumb.isNotEmpty()) {
                        item {
                            SectionHeader(label = "FOLDERS", onAdd = { onOpenFolder(folderId, null) })
                        }
                    }

                    if (filteredItems.isNotEmpty()) {
                        item { SectionHeader(label = "ITEMS", onAdd = null) }
                        items(filteredItems, key = { it.id }) { vaultItem ->
                            ItemRow(
                                item = vaultItem,
                                onClick = { onOpenItem(vaultItem.folderId, vaultItem.id) }
                            )
                        }
                    }
                }
            }
        }

        if (folderId != null) {
            FloatingActionButton(
                onClick = { onOpenItem(folderId, null) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                containerColor = OrionColors.SecondaryContainer,
                contentColor = OrionColors.OnSecondaryContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Novo item")
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String, onAdd: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = OrionColors.OnSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        if (onAdd != null) {
            IconButton(onClick = onAdd, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.CreateNewFolder, contentDescription = "Nova pasta", tint = OrionColors.Primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun FolderCard(folder: VaultFolder, onClick: () -> Unit) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth().aspectRatio(1.6f),
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = OrionColors.Primary)
            Text(
                folder.name,
                style = MaterialTheme.typography.titleMedium,
                color = OrionColors.OnSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ItemRow(item: VaultItem, onClick: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var justCopied by remember { mutableStateOf(false) }

    LaunchedEffect(justCopied) {
        if (justCopied) {
            delay(1200)
            justCopied = false
        }
    }

    GlassPanel(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).background(OrionColors.SurfaceContainerHigh, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        item.title.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = OrionColors.Primary
                    )
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, color = OrionColors.OnSurface, fontWeight = FontWeight.Bold)
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = OrionColors.Secondary, modifier = Modifier.size(14.dp))
                    }
                    if (!item.username.isNullOrBlank()) {
                        Text(item.username, style = MaterialTheme.typography.bodySmall, color = OrionColors.OnSurfaceVariant)
                    }
                }
            }
            IconButton(onClick = {
                clipboard.setText(AnnotatedString(item.password))
                justCopied = true
            }) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "Copiar senha",
                    tint = if (justCopied) OrionColors.Secondary else OrionColors.OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VaultEmptyState(isRoot: Boolean, onCreateFolder: () -> Unit, onAddItem: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(140.dp).background(OrionColors.GlassPanel, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = OrionColors.Primary, modifier = Modifier.size(64.dp))
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "All Items",
            style = MaterialTheme.typography.headlineLarge,
            color = OrionColors.OnSurface
        )

        Spacer(Modifier.height(8.dp))

        Text(
            if (isRoot) {
                "Seu cofre está vazio. Crie uma pasta para começar a guardar suas credenciais com criptografia de ponta a ponta."
            } else {
                "Esta pasta está vazia. Adicione seu primeiro item para começar a protegê-lo."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = OrionColors.OnSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .background(OrionColors.Primary, MaterialTheme.shapes.medium)
                .clickable(onClick = if (isRoot) onCreateFolder else onAddItem)
                .padding(horizontal = 32.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.CreateNewFolder, contentDescription = null, tint = OrionColors.OnPrimary)
            Text(
                if (isRoot) "Criar primeira pasta" else "Adicionar primeiro item",
                style = MaterialTheme.typography.titleMedium,
                color = OrionColors.OnPrimary
            )
        }
    }
}
