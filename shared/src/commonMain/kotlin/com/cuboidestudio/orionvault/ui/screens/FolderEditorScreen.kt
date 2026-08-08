package com.cuboidestudio.orionvault.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cuboidestudio.orionvault.ui.components.OrionButton
import com.cuboidestudio.orionvault.ui.components.OrionButtonVariant
import com.cuboidestudio.orionvault.ui.components.OrionDialog
import com.cuboidestudio.orionvault.ui.components.OrionScaffold
import com.cuboidestudio.orionvault.ui.components.OrionSectionHeader
import com.cuboidestudio.orionvault.ui.components.OrionSurface
import com.cuboidestudio.orionvault.ui.components.OrionTextField
import com.cuboidestudio.orionvault.ui.components.OrionTopBar
import com.cuboidestudio.orionvault.ui.theme.OrionSizes
import com.cuboidestudio.orionvault.ui.theme.OrionSpacing
import com.cuboidestudio.orionvault.viewmodel.FolderEditorViewModel

@Composable
fun FolderEditorScreen(viewModel: FolderEditorViewModel, onDone: () -> Unit) {
    val error by viewModel.errorMessage.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val folderName by viewModel.folderName.collectAsState()

    var name by remember { mutableStateOf("") }
    var prefilled by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(saved) {
        if (saved) onDone()
    }

    // Só preenche uma vez. Antes, qualquer reemissão de `folderName` sobrescrevia o que o usuário
    // já tinha digitado no campo.
    LaunchedEffect(folderName) {
        val current = folderName
        if (current != null && !prefilled) {
            name = current
            prefilled = true
        }
    }

    OrionScaffold(
        topBar = {
            OrionTopBar(
                title = if (viewModel.isEditing) "Renomear pasta" else "Nova pasta",
                onBack = onDone,
                showDivider = false
            )
        },
        bottomBar = {
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
                        modifier = Modifier.widthIn(max = OrionSizes.contentNarrow).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(OrionSpacing.sm)
                    ) {
                        OrionButton(
                            text = "Cancelar",
                            onClick = onDone,
                            variant = OrionButtonVariant.Ghost,
                            modifier = Modifier.weight(1f)
                        )
                        OrionButton(
                            text = "Salvar",
                            onClick = { viewModel.save(name) },
                            enabled = name.isNotBlank(),
                            modifier = Modifier.weight(1.6f)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = OrionSizes.contentNarrow)
                    .fillMaxWidth()
                    // Faltava rolagem aqui: em telas baixas, com o teclado aberto, o conteúdo cortava.
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = OrionSpacing.screenH, vertical = OrionSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FolderMark()

                Spacer(Modifier.height(OrionSpacing.lg))

                OrionSurface(modifier = Modifier.fillMaxWidth()) {
                    OrionTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = "Nome da pasta",
                        placeholder = "Ex.: Trabalho",
                        error = error
                    )
                }

                if (viewModel.isEditing) {
                    Spacer(Modifier.height(OrionSpacing.xl))
                    OrionSectionHeader(
                        label = "Zona de perigo",
                        modifier = Modifier.fillMaxWidth()
                    )
                    OrionButton(
                        text = "Excluir pasta permanentemente",
                        onClick = { showDeleteConfirm = true },
                        variant = OrionButtonVariant.Destructive,
                        icon = Icons.Filled.Delete,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(OrionSpacing.lg))
            }
        }
    }

    AnimatedVisibility(visible = showDeleteConfirm) {
        OrionDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = "Excluir esta pasta?",
            text = "A pasta \"$name\" será removida em definitivo, junto com todas as contas e " +
                "subpastas dentro dela. Não há como desfazer.",
            confirmLabel = "Excluir",
            destructive = true,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.delete()
            }
        )
    }
}

@Composable
private fun FolderMark() {
    Box(
        modifier = Modifier
            .size(88.dp)
            .background(
                Brush.radialGradient(
                    listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), Color.Transparent)
                ),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        )
    }
}
