package com.cuboidestudio.orionvault.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cuboidestudio.orionvault.domain.model.VaultFolder
import com.cuboidestudio.orionvault.domain.util.BreachStatus
import com.cuboidestudio.orionvault.domain.util.PasswordGenerator
import com.cuboidestudio.orionvault.ui.components.ItemAvatar
import com.cuboidestudio.orionvault.ui.components.OrionButton
import com.cuboidestudio.orionvault.ui.components.OrionButtonSize
import com.cuboidestudio.orionvault.ui.components.OrionButtonVariant
import com.cuboidestudio.orionvault.ui.components.OrionDialog
import com.cuboidestudio.orionvault.ui.components.OrionScaffold
import com.cuboidestudio.orionvault.ui.components.OrionSectionHeader
import com.cuboidestudio.orionvault.ui.components.OrionSurface
import com.cuboidestudio.orionvault.ui.components.OrionTextField
import com.cuboidestudio.orionvault.ui.components.OrionTopBar
import com.cuboidestudio.orionvault.ui.components.PasswordStrengthMeter
import com.cuboidestudio.orionvault.ui.components.SecurityBadge
import com.cuboidestudio.orionvault.ui.components.SecurityLevel
import com.cuboidestudio.orionvault.ui.theme.OrionMotion
import com.cuboidestudio.orionvault.ui.theme.OrionShapes
import com.cuboidestudio.orionvault.ui.theme.OrionSizes
import com.cuboidestudio.orionvault.ui.theme.OrionSpacing
import com.cuboidestudio.orionvault.viewmodel.ItemEditorStep
import com.cuboidestudio.orionvault.viewmodel.ItemEditorViewModel

@Composable
fun ItemEditorScreen(viewModel: ItemEditorViewModel, onDone: () -> Unit) {
    val step by viewModel.step.collectAsState()

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            val forward = targetState == ItemEditorStep.Form
            val offset = if (forward) 1 else -1
            (
                slideInHorizontally(OrionMotion.tweenMedium()) { it / 4 * offset } +
                    fadeIn(OrionMotion.tweenMedium())
                ) togetherWith (
                slideOutHorizontally(OrionMotion.tweenMedium()) { -it / 4 * offset } +
                    fadeOut(OrionMotion.tweenFast())
                ) using SizeTransform(clip = false)
        },
        label = "itemEditorStep"
    ) { currentStep ->
        when (currentStep) {
            ItemEditorStep.FieldConfig -> FieldConfigStep(viewModel, onCancel = onDone)
            ItemEditorStep.Form -> ItemFormStep(viewModel, onDone = onDone)
        }
    }
}

@Composable
private fun FieldConfigStep(viewModel: ItemEditorViewModel, onCancel: () -> Unit) {
    val includeUsername by viewModel.includeUsername.collectAsState()
    val includeEmail by viewModel.includeEmail.collectAsState()

    OrionScaffold(
        topBar = { OrionTopBar(title = "Nova conta", onBack = onCancel, showDivider = false) }
    ) { padding ->
        EditorContent(padding = padding) {
            Text(
                text = "Quais campos você quer incluir?",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(OrionSpacing.xxs))
            Text(
                text = "Você pode adicionar ou remover esses campos depois, a qualquer momento.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(OrionSpacing.lg))

            OrionSurface(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(OrionSpacing.xxs)) {
                FieldToggle(
                    icon = Icons.Filled.Person,
                    label = "Incluir nome de usuário",
                    checked = includeUsername,
                    onCheckedChange = viewModel::setIncludeUsername
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FieldToggle(
                    icon = Icons.Filled.Email,
                    label = "Incluir e-mail",
                    checked = includeEmail,
                    onCheckedChange = viewModel::setIncludeEmail
                )
            }

            Spacer(Modifier.height(OrionSpacing.lg))

            OrionButton(
                text = "Avançar",
                onClick = viewModel::advanceToForm,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FieldToggle(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = OrionSpacing.sm, vertical = OrionSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OrionSpacing.sm)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(OrionSizes.icon)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimaryContainer,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
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
    val reuseCount by viewModel.reuseCount.collectAsState()
    val breachStatus by viewModel.breachStatus.collectAsState()

    var title by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

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

    LaunchedEffect(password) {
        viewModel.onPasswordChanged(password)
    }

    // O caminho de cada pasta é calculado uma vez por lista, não a cada chamada: antes o
    // `associateBy` era reconstruído dentro de um `sortedBy`, ou seja, a cada comparação.
    val folderPaths = remember(folders) { buildFolderPaths(folders) }
    val isEditing = existing != null

    OrionScaffold(
        topBar = {
            OrionTopBar(
                title = if (isEditing) "Editar conta" else "Nova conta",
                onBack = onDone,
                showDivider = false
            )
        },
        bottomBar = {
            EditorBottomBar(
                primaryLabel = "Salvar",
                onPrimary = {
                    viewModel.save(
                        title,
                        if (includeUsername) username.ifBlank { null } else null,
                        if (includeEmail) email.ifBlank { null } else null,
                        password,
                        url.ifBlank { null },
                        notes.ifBlank { null }
                    )
                },
                onCancel = onDone
            )
        }
    ) { padding ->
        EditorContent(padding = padding) {
            // --- Identidade -----------------------------------------------------------------
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ItemAvatar(
                    title = title.ifBlank { "?" },
                    size = 68.dp,
                    shape = OrionShapes.CardLarge
                )
                Spacer(Modifier.height(OrionSpacing.sm))
                Text(
                    text = title.ifBlank { "Nova conta" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(OrionSpacing.xs))
                SecurityBadge(text = "Criptografado", level = SecurityLevel.Secure)
            }

            Spacer(Modifier.height(OrionSpacing.xl))

            // --- Identificação --------------------------------------------------------------
            OrionSectionHeader("Identificação")
            OrionSurface(modifier = Modifier.fillMaxWidth()) {
                OrionTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "Título da conta",
                    placeholder = "Ex.: GitHub"
                )

                Spacer(Modifier.height(OrionSpacing.md))

                Text(
                    text = "Pasta",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = OrionSpacing.xxs + 2.dp)
                )
                FolderPickerRow(
                    label = selectedFolderId?.let { folderPaths[it] } ?: "Nenhuma pasta",
                    hasFolder = selectedFolderId != null,
                    onClick = { showFolderPicker = true }
                )
            }

            Spacer(Modifier.height(OrionSpacing.lg))

            // --- Credenciais ----------------------------------------------------------------
            OrionSectionHeader("Credenciais")
            OrionSurface(modifier = Modifier.fillMaxWidth()) {
                OptionalField(
                    label = "Nome de usuário",
                    addLabel = "Adicionar nome de usuário",
                    visible = includeUsername,
                    onAdd = { viewModel.setIncludeUsername(true) },
                    onRemove = {
                        viewModel.setIncludeUsername(false)
                        username = ""
                    }
                ) {
                    OrionTextField(
                        value = username,
                        onValueChange = { username = it },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { Icon(Icons.Filled.Person, contentDescription = null) }
                    )
                }

                OptionalField(
                    label = "E-mail",
                    addLabel = "Adicionar e-mail",
                    visible = includeEmail,
                    onAdd = { viewModel.setIncludeEmail(true) },
                    onRemove = {
                        viewModel.setIncludeEmail(false)
                        email = ""
                    }
                ) {
                    OrionTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        trailingIcon = { Icon(Icons.Filled.Email, contentDescription = null) }
                    )
                }

                Text(
                    text = "Senha",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = OrionSpacing.xxs + 2.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(OrionSpacing.xs),
                    verticalAlignment = Alignment.Top
                ) {
                    OrionTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.weight(1f),
                        mono = true,
                        visualTransformation = if (showPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) {
                                        Icons.Filled.VisibilityOff
                                    } else {
                                        Icons.Filled.Visibility
                                    },
                                    contentDescription = if (showPassword) "Ocultar senha" else "Mostrar senha"
                                )
                            }
                        }
                    )
                    OrionButton(
                        text = "Gerar",
                        onClick = { password = PasswordGenerator.generate() },
                        variant = OrionButtonVariant.Secondary,
                        icon = Icons.Filled.EnhancedEncryption,
                        modifier = Modifier.heightIn(min = OrionSizes.field).padding(top = 3.dp)
                    )
                }

                AnimatedVisibility(visible = password.isNotEmpty()) {
                    val (score, label) = remember(password) { PasswordGenerator.strength(password) }
                    Column {
                        Spacer(Modifier.height(OrionSpacing.sm))
                        PasswordStrengthMeter(score = score, label = label)

                        if (reuseCount > 0) {
                            Spacer(Modifier.height(OrionSpacing.xs))
                            SecurityBadge(
                                text = if (reuseCount == 1) {
                                    "Senha reutilizada em 1 outra conta"
                                } else {
                                    "Senha reutilizada em $reuseCount outras contas"
                                },
                                level = SecurityLevel.Warning
                            )
                        }

                        val breach = breachStatus
                        if (breach is BreachStatus.Breached) {
                            Spacer(Modifier.height(OrionSpacing.xs))
                            SecurityBadge(
                                text = "Encontrada em vazamentos conhecidos (${breach.count}x)",
                                level = SecurityLevel.Danger
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(OrionSpacing.lg))

            // --- Detalhes -------------------------------------------------------------------
            OrionSectionHeader("Detalhes")
            OrionSurface(modifier = Modifier.fillMaxWidth()) {
                OrionTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "URL",
                    placeholder = "https://",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    trailingIcon = { Icon(Icons.Filled.Link, contentDescription = null) }
                )

                Spacer(Modifier.height(OrionSpacing.md))

                OrionTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "Descrição",
                    placeholder = "Anotações sobre esta conta.",
                    singleLine = false,
                    minLines = 4,
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null)
                    }
                )
            }

            AnimatedVisibility(visible = error != null) {
                Column {
                    Spacer(Modifier.height(OrionSpacing.sm))
                    Text(
                        text = error.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(OrionSpacing.lg))

            SecurityNote()

            if (isEditing) {
                Spacer(Modifier.height(OrionSpacing.lg))
                OrionSectionHeader("Zona de perigo")
                OrionButton(
                    text = "Excluir item permanentemente",
                    onClick = { showDeleteConfirm = true },
                    variant = OrionButtonVariant.Destructive,
                    icon = Icons.Filled.Delete,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(OrionSpacing.lg))
        }
    }

    if (showFolderPicker) {
        FolderSelectDialog(
            folders = folders,
            folderPaths = folderPaths,
            onSelect = { id ->
                viewModel.selectFolder(id)
                showFolderPicker = false
            },
            onDismiss = { showFolderPicker = false }
        )
    }

    // Exclusão de item passou a exigir confirmação. Antes ela era imediata, enquanto o editor de
    // pastas — uma ação menos destrutiva — já perguntava.
    if (showDeleteConfirm) {
        OrionDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = "Excluir este item?",
            text = "A conta \"${title.ifBlank { "sem título" }}\" será removida em definitivo deste " +
                "cofre e de todos os dispositivos sincronizados. Não há como desfazer.",
            confirmLabel = "Excluir",
            destructive = true,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.delete()
            }
        )
    }
}

/**
 * Coluna de conteúdo dos editores.
 *
 * O `widthIn(max = …)` vem antes do preenchimento de largura e a coluna externa centraliza — nas telas
 * antigas a ordem estava invertida, então o limite de largura não centralizava nada em janelas largas.
 */
@Composable
private fun EditorContent(
    padding: PaddingValues,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = OrionSizes.contentForm)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OrionSpacing.screenH, vertical = OrionSpacing.md),
            content = content
        )
    }
}

@Composable
private fun EditorBottomBar(
    primaryLabel: String,
    onPrimary: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Box(
            modifier = Modifier.fillMaxWidth().padding(
                horizontal = OrionSpacing.screenH,
                vertical = OrionSpacing.sm
            ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.widthIn(max = OrionSizes.contentForm).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(OrionSpacing.sm)
            ) {
                OrionButton(
                    text = "Cancelar",
                    onClick = onCancel,
                    variant = OrionButtonVariant.Ghost,
                    modifier = Modifier.weight(1f)
                )
                OrionButton(
                    text = primaryLabel,
                    onClick = onPrimary,
                    variant = OrionButtonVariant.Primary,
                    modifier = Modifier.weight(1.6f)
                )
            }
        }
    }
}

@Composable
private fun FolderPickerRow(label: String, hasFolder: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest, OrionShapes.Input)
            .clickable(onClick = onClick)
            .padding(horizontal = OrionSpacing.md, vertical = OrionSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OrionSpacing.sm)
    ) {
        Icon(
            imageVector = if (hasFolder) Icons.Filled.Folder else Icons.Filled.FolderOff,
            contentDescription = null,
            tint = if (hasFolder) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(OrionSizes.icon)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SecurityNote() {
    OrionSurface(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(OrionSpacing.sm)) {
            Icon(
                imageVector = Icons.Filled.EnhancedEncryption,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(OrionSizes.icon)
            )
            Column {
                Text(
                    text = "Ambiente seguro",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(OrionSpacing.xxs))
                // O texto anterior dizia AES-256; a implementação real em `crypto/AeadCipher` é
                // XChaCha20-Poly1305.
                Text(
                    text = "Este item é criptografado com XChaCha20-Poly1305 e fica apenas nos " +
                        "dispositivos que você autorizou.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remover $label",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(OrionSizes.iconSm)
                )
            }
        }
        Spacer(Modifier.height(OrionSpacing.xxs))
        field()
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAdd)
                .padding(vertical = OrionSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OrionSpacing.xxs + 2.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(OrionSizes.iconSm)
            )
            Text(
                text = addLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    Spacer(Modifier.height(OrionSpacing.md))
}

@Composable
private fun FolderSelectDialog(
    folders: List<VaultFolder>,
    folderPaths: Map<String, String>,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sorted = remember(folders, folderPaths) {
        folders.sortedBy { folderPaths[it.id].orEmpty() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = OrionShapes.Dialog,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = "Escolha uma pasta",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(OrionSpacing.xxs)) {
                item {
                    FolderOption(
                        label = "Nenhuma pasta",
                        icon = Icons.Filled.FolderOff,
                        onClick = { onSelect(null) }
                    )
                }
                items(sorted, key = { it.id }) { folder ->
                    FolderOption(
                        label = folderPaths[folder.id] ?: folder.name,
                        icon = Icons.Filled.Folder,
                        onClick = { onSelect(folder.id) }
                    )
                }
            }
        },
        confirmButton = {
            OrionButton(
                text = "Cancelar",
                onClick = onDismiss,
                variant = OrionButtonVariant.Ghost,
                size = OrionButtonSize.Medium
            )
        }
    )
}

@Composable
private fun FolderOption(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = OrionSpacing.sm, horizontal = OrionSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OrionSpacing.sm)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(OrionSizes.icon)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Monta uma vez o caminho completo ("Pai / Filho") de cada pasta, indexado por id. */
private fun buildFolderPaths(folders: List<VaultFolder>): Map<String, String> {
    val byId = folders.associateBy { it.id }
    return folders.associate { folder ->
        val names = mutableListOf(folder.name)
        var parentId = folder.parentId
        // O limite de profundidade é 5 (VaultConstants.MAX_FOLDER_DEPTH); o `seen` protege contra
        // um ciclo em dados corrompidos, que aqui viraria laço infinito.
        val seen = mutableSetOf(folder.id)
        while (parentId != null && seen.add(parentId)) {
            val parent = byId[parentId] ?: break
            names.add(0, parent.name)
            parentId = parent.parentId
        }
        folder.id to names.joinToString(" / ")
    }
}
