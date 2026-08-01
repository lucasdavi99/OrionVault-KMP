package com.cuboidestudio.orionvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.cuboidestudio.orionvault.domain.model.VaultFolder
import com.cuboidestudio.orionvault.domain.util.PasswordGenerator
import com.cuboidestudio.orionvault.ui.components.GlassPanel
import com.cuboidestudio.orionvault.ui.components.VaultTextField
import com.cuboidestudio.orionvault.ui.theme.CodeTextStyle
import com.cuboidestudio.orionvault.ui.theme.OrionColors
import com.cuboidestudio.orionvault.viewmodel.ItemEditorStep
import com.cuboidestudio.orionvault.viewmodel.ItemEditorViewModel

@Composable
fun ItemEditorScreen(viewModel: ItemEditorViewModel, onDone: () -> Unit) {
    val step by viewModel.step.collectAsState()
    when (step) {
        ItemEditorStep.FieldConfig -> FieldConfigStep(viewModel, onCancel = onDone)
        ItemEditorStep.Form -> ItemFormStep(viewModel, onDone = onDone)
    }
}

@Composable
private fun FieldConfigStep(viewModel: ItemEditorViewModel, onCancel: () -> Unit) {
    val includeUsername by viewModel.includeUsername.collectAsState()
    val includeEmail by viewModel.includeEmail.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(OrionColors.Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onCancel) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = OrionColors.OnSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Cancel", style = MaterialTheme.typography.labelSmall, color = OrionColors.OnSurfaceVariant)
            }
            Text(
                "Nova Conta",
                style = MaterialTheme.typography.headlineMedium,
                color = OrionColors.Primary
            )
            Spacer(Modifier.width(64.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .widthIn(max = 560.dp)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Quais campos você quer incluir?",
                style = MaterialTheme.typography.titleMedium,
                color = OrionColors.OnSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Você pode adicionar ou remover isso depois, a qualquer momento.",
                style = MaterialTheme.typography.labelSmall,
                color = OrionColors.OnSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = OrionColors.OnSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Text("Incluir Username", style = MaterialTheme.typography.bodyMedium, color = OrionColors.OnSurface)
                        }
                        Switch(checked = includeUsername, onCheckedChange = viewModel::setIncludeUsername)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Email, contentDescription = null, tint = OrionColors.OnSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Text("Incluir E-mail", style = MaterialTheme.typography.bodyMedium, color = OrionColors.OnSurface)
                        }
                        Switch(checked = includeEmail, onCheckedChange = viewModel::setIncludeEmail)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OrionColors.Primary, MaterialTheme.shapes.small)
                    .clickable { viewModel.advanceToForm() }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Avançar", style = MaterialTheme.typography.labelLarge, color = OrionColors.Background, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ItemFormStep(viewModel: ItemEditorViewModel, onDone: () -> Unit) {
    val existing by viewModel.item.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val includeUsername by viewModel.includeUsername.collectAsState()
    val includeEmail by viewModel.includeEmail.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()

    var title by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        val item = existing
        if (item != null && !initialized) {
            title = item.title
            username = item.username.orEmpty()
            email = item.email.orEmpty()
            password = item.password
            url = item.url.orEmpty()
            notes = item.notes.orEmpty()
            initialized = true
        }
    }

    LaunchedEffect(saved) {
        if (saved) onDone()
    }

    Column(modifier = Modifier.fillMaxSize().background(OrionColors.Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onDone) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = OrionColors.OnSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Cancel", style = MaterialTheme.typography.labelSmall, color = OrionColors.OnSurfaceVariant)
            }
            Text(
                if (existing != null) "Editar Conta" else "Nova Conta",
                style = MaterialTheme.typography.headlineMedium,
                color = OrionColors.Primary
            )
            TextButton(onClick = {
                viewModel.save(
                    title,
                    if (includeUsername) username.ifBlank { null } else null,
                    if (includeEmail) email.ifBlank { null } else null,
                    password,
                    url.ifBlank { null },
                    notes.ifBlank { null }
                )
            }) {
                Text("Save", style = MaterialTheme.typography.labelSmall, color = OrionColors.Primary, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .widthIn(max = 560.dp)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(64.dp).background(OrionColors.GlassPanel, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    title.take(1).ifBlank { "?" }.uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = OrionColors.Primary
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                title.ifBlank { "Nova Conta" },
                style = MaterialTheme.typography.headlineMedium,
                color = OrionColors.OnSurface
            )

            Spacer(Modifier.height(24.dp))

            FieldLabel("Título da Conta")
            VaultTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(16.dp))

            FieldLabel("Pasta")
            val selectedFolder = remember(folders, selectedFolderId) { folders.firstOrNull { it.id == selectedFolderId } }
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showFolderPicker = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            if (selectedFolder != null) Icons.Filled.Folder else Icons.Filled.FolderOff,
                            contentDescription = null,
                            tint = OrionColors.Primary
                        )
                        Text(
                            selectedFolder?.let { folderPath(it, folders) } ?: "Nenhuma pasta",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OrionColors.OnSurface
                        )
                    }
                }
            }

            if (showFolderPicker) {
                FolderSelectDialog(
                    folders = folders,
                    onSelect = { id ->
                        viewModel.selectFolder(id)
                        showFolderPicker = false
                    },
                    onDismiss = { showFolderPicker = false }
                )
            }

            Spacer(Modifier.height(16.dp))

            OptionalField(
                label = "Nome de usuário",
                addLabel = "Adicionar Username",
                visible = includeUsername,
                onAdd = { viewModel.setIncludeUsername(true) },
                onRemove = {
                    viewModel.setIncludeUsername(false)
                    username = ""
                }
            ) {
                VaultTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = OrionColors.OnSurfaceVariant) }
                )
            }

            OptionalField(
                label = "E-mail",
                addLabel = "Adicionar E-mail",
                visible = includeEmail,
                onAdd = { viewModel.setIncludeEmail(true) },
                onRemove = {
                    viewModel.setIncludeEmail(false)
                    email = ""
                }
            ) {
                VaultTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    trailingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = OrionColors.OnSurfaceVariant) }
                )
            }

            Spacer(Modifier.height(16.dp))

            FieldLabel("Senha")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VaultTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.weight(1f),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    textStyle = CodeTextStyle,
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Mostrar senha",
                                tint = OrionColors.OnSurfaceVariant
                            )
                        }
                    }
                )
                Row(
                    modifier = Modifier
                        .background(OrionColors.SecondaryContainer, MaterialTheme.shapes.small)
                        .clickable { password = PasswordGenerator.generate() }
                        .padding(horizontal = 12.dp)
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.EnhancedEncryption, contentDescription = null, tint = OrionColors.OnSecondaryContainer, modifier = Modifier.size(16.dp))
                    Text("Generate", style = MaterialTheme.typography.labelSmall, color = OrionColors.OnSecondaryContainer)
                }
            }

            if (password.isNotEmpty()) {
                val (score, label) = remember(password) { PasswordGenerator.strength(password) }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = OrionColors.Secondary)
                    Text("${(score * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = OrionColors.OnSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(OrionColors.SurfaceContainerHighest, MaterialTheme.shapes.small)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(score)
                            .height(4.dp)
                            .background(OrionColors.Secondary, MaterialTheme.shapes.small)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            FieldLabel("URL")
            VaultTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Icon(Icons.Filled.Link, contentDescription = null, tint = OrionColors.OnSurfaceVariant) }
            )

            Spacer(Modifier.height(16.dp))

            FieldLabel("Descrição")
            VaultTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Adicione sua descrição aqui.",
                singleLine = false,
                minLines = 4,
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = OrionColors.OnSurfaceVariant) }
            )

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.labelSmall, color = OrionColors.Error)
            }

            Spacer(Modifier.height(24.dp))

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = OrionColors.Secondary)
                    Column {
                        Text("Secure Environment", style = MaterialTheme.typography.labelLarge, color = OrionColors.OnSurface, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "This item is encrypted with AES-256 and stored only on your authorized devices.",
                            style = MaterialTheme.typography.labelSmall,
                            color = OrionColors.OnSurfaceVariant
                        )
                    }
                }
            }

            if (existing != null) {
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.delete() }
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = OrionColors.Error, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Item Permanently", style = MaterialTheme.typography.labelSmall, color = OrionColors.Error)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OptionalField(
    label: String,
    addLabel: String,
    visible: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    field: @Composable () -> Unit
) {
    if (visible) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FieldLabel(label, modifier = Modifier.weight(1f))
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Remover $label", tint = OrionColors.OnSurfaceVariant)
            }
        }
        field()
        Spacer(Modifier.height(16.dp))
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAdd)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = OrionColors.Primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(addLabel, style = MaterialTheme.typography.labelSmall, color = OrionColors.Primary)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = OrionColors.Primary,
        modifier = modifier.fillMaxWidth().padding(bottom = 6.dp)
    )
}

@Composable
private fun FolderSelectDialog(
    folders: List<VaultFolder>,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sorted = remember(folders) { folders.sortedBy { folderPath(it, folders) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Escolha uma pasta", color = OrionColors.OnSurface) },
        containerColor = OrionColors.SurfaceContainerHigh,
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(null) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.FolderOff, contentDescription = null, tint = OrionColors.Primary)
                        Text("Nenhuma pasta", color = OrionColors.OnSurface)
                    }
                }
                items(sorted, key = { it.id }) { folder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(folder.id) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.Folder, contentDescription = null, tint = OrionColors.Primary)
                        Text(folderPath(folder, folders), color = OrionColors.OnSurface)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = OrionColors.Primary)
            }
        }
    )
}

private fun folderPath(folder: VaultFolder, folders: List<VaultFolder>): String {
    val byId = folders.associateBy { it.id }
    val names = mutableListOf(folder.name)
    var parentId = folder.parentId
    while (parentId != null) {
        val parent = byId[parentId] ?: break
        names.add(0, parent.name)
        parentId = parent.parentId
    }
    return names.joinToString(" / ")
}
