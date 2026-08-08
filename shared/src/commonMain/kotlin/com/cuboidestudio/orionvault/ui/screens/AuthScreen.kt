package com.cuboidestudio.orionvault.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.cuboidestudio.orionvault.ui.components.OrionButton
import com.cuboidestudio.orionvault.ui.components.OrionButtonVariant
import com.cuboidestudio.orionvault.ui.components.OrionScaffold
import com.cuboidestudio.orionvault.ui.components.OrionSurface
import com.cuboidestudio.orionvault.ui.components.OrionTextField
import com.cuboidestudio.orionvault.ui.components.OrionTopBar
import com.cuboidestudio.orionvault.ui.theme.OrionSizes
import com.cuboidestudio.orionvault.ui.theme.OrionSpacing
import com.cuboidestudio.orionvault.viewmodel.AuthMode
import com.cuboidestudio.orionvault.viewmodel.AuthViewModel

@Composable
fun AuthScreen(viewModel: AuthViewModel, onBack: () -> Unit, onAuthenticated: () -> Unit) {
    val mode by viewModel.mode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val info by viewModel.infoMessage.collectAsState()
    val completed by viewModel.completed.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    val isSignUp = mode == AuthMode.SignUp

    LaunchedEffect(completed) {
        if (completed) onAuthenticated()
    }

    OrionScaffold(
        topBar = {
            OrionTopBar(
                title = if (isSignUp) "Criar conta" else "Entrar",
                onBack = onBack,
                showDivider = false
            )
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = OrionSpacing.screenH, vertical = OrionSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OrionSurface(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OrionSpacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(OrionSizes.icon)
                        )
                        Text(
                            text = "Sincronização na nuvem",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(Modifier.height(OrionSpacing.xs))

                    Text(
                        text = "Só o conteúdo já criptografado sai deste aparelho. Sua Master " +
                            "Password e sua Secret Key nunca são enviadas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(OrionSpacing.lg))

                    OrionTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = "E-mail",
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(Modifier.height(OrionSpacing.md))

                    OrionTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = "Senha da conta",
                        mono = true,
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    AnimatedVisibility(visible = isSignUp) {
                        Column {
                            Spacer(Modifier.height(OrionSpacing.md))
                            OrionTextField(
                                value = confirm,
                                onValueChange = { confirm = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = "Confirmar senha",
                                mono = true,
                                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                            )
                        }
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

                    AnimatedVisibility(visible = info != null) {
                        Column {
                            Spacer(Modifier.height(OrionSpacing.sm))
                            Text(
                                text = info.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Spacer(Modifier.height(OrionSpacing.lg))

                    OrionButton(
                        text = if (isSignUp) "Criar conta" else "Entrar",
                        onClick = {
                            viewModel.submit(
                                email.trim(),
                                password.toCharArray(),
                                if (isSignUp) confirm.toCharArray() else null
                            )
                        },
                        loading = isLoading,
                        enabled = email.isNotBlank() && password.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(OrionSpacing.md))

                OrionButton(
                    text = if (isSignUp) "Já tenho conta? Entrar" else "Não tenho conta? Criar uma",
                    onClick = viewModel::toggleMode,
                    enabled = !isLoading,
                    variant = OrionButtonVariant.Ghost,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(OrionSpacing.md))

                Text(
                    text = "Esta conta serve apenas para transportar o cofre entre dispositivos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
