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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cuboidestudio.orionvault.ui.components.VaultTextField
import com.cuboidestudio.orionvault.ui.theme.CodeTextStyle
import com.cuboidestudio.orionvault.ui.theme.OrionColors
import com.cuboidestudio.orionvault.viewmodel.UnlockViewModel

@Composable
fun UnlockScreen(viewModel: UnlockViewModel, onUnlocked: () -> Unit) {
    val error by viewModel.errorMessage.collectAsState()
    val unlocked by viewModel.unlocked.collectAsState()
    var password by remember { mutableStateOf("") }

    LaunchedEffect(unlocked) {
        if (unlocked) onUnlocked()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OrionColors.Background)
            .background(
                Brush.radialGradient(
                    colors = listOf(OrionColors.Primary.copy(alpha = 0.10f), OrionColors.Primary.copy(alpha = 0f)),
                    center = Offset(0f, 0f),
                    radius = 900f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VaultLockMark()

            Spacer(Modifier.height(32.dp))

            Text(
                "Vault Locked",
                style = MaterialTheme.typography.headlineMedium,
                color = OrionColors.OnSurface
            )

            Spacer(Modifier.height(24.dp))

            VaultTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Master Password",
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = OrionColors.Outline) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                textStyle = CodeTextStyle,
                isError = error != null
            )

            error?.let { message ->
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                ) {
                    Icon(Icons.Filled.Error, contentDescription = null, tint = OrionColors.Error, modifier = Modifier.size(16.dp))
                    Text(message, style = MaterialTheme.typography.labelSmall, color = OrionColors.Error)
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { viewModel.attemptUnlock(password.toCharArray()) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrionColors.Primary,
                    contentColor = OrionColors.OnPrimary
                )
            ) {
                Text("Unlock Vault", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Sem a Secret Key e a Master Password corretas, não há como recuperar o cofre.",
                style = MaterialTheme.typography.labelSmall,
                color = OrionColors.Outline,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun VaultLockMark() {
    Box(
        modifier = Modifier
            .size(96.dp)
            .background(OrionColors.GlassPanel, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            tint = OrionColors.Primary,
            modifier = Modifier.size(44.dp)
        )
    }
}
