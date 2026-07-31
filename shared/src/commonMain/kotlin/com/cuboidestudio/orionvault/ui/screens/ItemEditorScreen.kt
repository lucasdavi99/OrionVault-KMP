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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cuboidestudio.orionvault.ui.components.GlassPanel
import com.cuboidestudio.orionvault.ui.components.VaultTextField
import com.cuboidestudio.orionvault.ui.theme.CodeTextStyle
import com.cuboidestudio.orionvault.ui.theme.OrionColors
import com.cuboidestudio.orionvault.viewmodel.ItemEditorViewModel
import kotlin.random.Random

private const val PASSWORD_CHARSET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#\$%^&*-_=+"

private fun generatePassword(length: Int = 20): String =
    (1..length).map { PASSWORD_CHARSET[Random.nextInt(PASSWORD_CHARSET.length)] }.joinToString("")

private fun passwordStrength(password: String): Pair<Float, String> {
    if (password.isEmpty()) return 0f to "—"
    var variety = 0
    if (password.any { it.isLowerCase() }) variety++
    if (password.any { it.isUpperCase() }) variety++
    if (password.any { it.isDigit() }) variety++
    if (password.any { !it.isLetterOrDigit() }) variety++
    val lengthScore = (password.length.coerceAtMost(24) / 24f)
    val score = (lengthScore * 0.6f + (variety / 4f) * 0.4f).coerceIn(0f, 1f)
    val label = when {
        score < 0.35f -> "Weak"
        score < 0.6f -> "Fair"
        score < 0.85f -> "Strong Security"
        else -> "Excellent Security"
    }
    return score to label
}

@Composable
fun ItemEditorScreen(viewModel: ItemEditorViewModel, onDone: () -> Unit) {
    val existing by viewModel.item.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val saved by viewModel.saved.collectAsState()

    var title by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        val item = existing
        if (item != null && !initialized) {
            title = item.title
            username = item.username.orEmpty()
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
                if (existing != null) "Edit Item" else "New Item",
                style = MaterialTheme.typography.headlineMedium,
                color = OrionColors.Primary
            )
            TextButton(onClick = {
                viewModel.save(title, username.ifBlank { null }, password, url.ifBlank { null }, notes.ifBlank { null })
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
                title.ifBlank { "Novo item" },
                style = MaterialTheme.typography.headlineMedium,
                color = OrionColors.OnSurface
            )

            Spacer(Modifier.height(24.dp))

            FieldLabel("NAME")
            VaultTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(16.dp))

            FieldLabel("USERNAME")
            VaultTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = OrionColors.OnSurfaceVariant) }
            )

            Spacer(Modifier.height(16.dp))

            FieldLabel("PASSWORD")
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
                        .clickable { password = generatePassword() }
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
                val (score, label) = remember(password) { passwordStrength(password) }
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

            FieldLabel("NOTES")
            VaultTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Add secure notes here...",
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
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = OrionColors.Primary,
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
    )
}
