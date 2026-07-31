package com.cuboidestudio.orionvault.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.cuboidestudio.orionvault.ui.components.VaultTextField
import com.cuboidestudio.orionvault.ui.theme.OrionColors
import com.cuboidestudio.orionvault.viewmodel.FolderEditorViewModel

@Composable
fun FolderEditorScreen(viewModel: FolderEditorViewModel, onDone: () -> Unit) {
    val error by viewModel.errorMessage.collectAsState()
    val saved by viewModel.saved.collectAsState()
    var name by remember { mutableStateOf("") }

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
                if (viewModel.isEditing) "Rename Folder" else "New Folder",
                style = MaterialTheme.typography.headlineMedium,
                color = OrionColors.Primary
            )
            TextButton(onClick = { viewModel.save(name) }) {
                Text("Save", style = MaterialTheme.typography.labelSmall, color = OrionColors.Primary, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 480.dp)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(64.dp).background(OrionColors.GlassPanel, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Folder, contentDescription = null, tint = OrionColors.Primary, modifier = Modifier.size(28.dp))
            }

            Spacer(Modifier.height(24.dp))

            VaultTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = "Nome da pasta"
            )

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.labelSmall, color = OrionColors.Error)
            }
        }
    }
}
