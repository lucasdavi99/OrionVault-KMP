package com.cuboidestudio.orionvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cuboidestudio.orionvault.ui.components.GlassPanel
import com.cuboidestudio.orionvault.ui.components.VaultTextField
import com.cuboidestudio.orionvault.ui.components.VaultTopBar
import com.cuboidestudio.orionvault.ui.theme.CodeTextStyle
import com.cuboidestudio.orionvault.ui.theme.OrionColors
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

    Column(modifier = Modifier.fillMaxSize().background(OrionColors.Background)) {
        VaultTopBar(
            leading = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = OrionColors.Primary
                    )
                }
            },
            title = if (isSignUp) "Criar conta" else "Entrar",
            trailing = { Spacer(Modifier.size(48.dp)) }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .widthIn(max = 480.dp)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlassPanel(modifier = Modifier.fillMaxWidth(), padding = 20.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(
                        Icons.Filled.CloudSync,
                        contentDescription = null,
                        tint = OrionColors.Secondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        "Sincronize seu cofre entre dispositivos. Só o conteúdo já criptografado sai " +
                            "deste aparelho — sua Master Password e sua Secret Key nunca são enviadas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OrionColors.OnSurfaceVariant
                    )

                    VaultTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = "E-mail",
                        leadingIcon = {
                            Icon(Icons.Filled.Email, contentDescription = null, tint = OrionColors.Outline)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = error != null
                    )

                    VaultTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = "Senha da conta",
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = OrionColors.Outline)
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        textStyle = CodeTextStyle,
                        isError = error != null
                    )

                    if (isSignUp) {
                        VaultTextField(
                            value = confirm,
                            onValueChange = { confirm = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = "Confirmar senha",
                            leadingIcon = {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = OrionColors.Outline)
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            textStyle = CodeTextStyle,
                            isError = error != null
                        )
                    }

                    error?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = OrionColors.Error)
                    }
                    info?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = OrionColors.Secondary)
                    }

                    Button(
                        onClick = {
                            viewModel.submit(
                                email.trim(),
                                password.toCharArray(),
                                if (isSignUp) confirm.toCharArray() else null
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrionColors.SecondaryContainer,
                            contentColor = OrionColors.OnSecondaryContainer
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = OrionColors.OnSecondaryContainer
                            )
                        } else {
                            Text(
                                if (isSignUp) "Criar conta" else "Entrar",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth().clickable(enabled = !isLoading) { viewModel.toggleMode() }) {
                Text(
                    if (isSignUp) "Já tem conta? Entrar" else "Não tem conta? Criar uma",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OrionColors.Primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                )
            }
        }
    }
}
