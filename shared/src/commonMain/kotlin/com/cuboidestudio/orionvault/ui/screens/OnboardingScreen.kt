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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cuboidestudio.orionvault.ui.components.GlassPanel
import com.cuboidestudio.orionvault.ui.components.VaultTextField
import com.cuboidestudio.orionvault.ui.theme.CodeTextStyle
import com.cuboidestudio.orionvault.ui.theme.OrionColors
import com.cuboidestudio.orionvault.viewmodel.OnboardingStep
import com.cuboidestudio.orionvault.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel, onCompleted: () -> Unit) {
    val step by viewModel.step.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val completed by viewModel.completed.collectAsState()

    LaunchedEffect(completed) {
        if (completed) onCompleted()
    }

    Box(modifier = Modifier.fillMaxSize().background(OrionColors.Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .widthIn(max = 480.dp)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OnboardingHeader()

            Spacer(Modifier.height(32.dp))

            when (val currentStep = step) {
                is OnboardingStep.SetMasterPassword -> SetMasterPasswordStep(
                    error = error,
                    onCreate = { password, confirm -> viewModel.createVault(password, confirm) }
                )

                is OnboardingStep.ShowSecretKey -> ShowSecretKeyStep(
                    secretKey = currentStep.secretKeyDisplay,
                    onConfirm = { viewModel.confirmSecretKeySaved() }
                )
            }
        }
    }
}

@Composable
private fun OnboardingHeader() {
    Box(
        modifier = Modifier.size(96.dp).background(OrionColors.GlassPanel, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = OrionColors.Primary, modifier = Modifier.size(44.dp))
    }
    Spacer(Modifier.height(16.dp))
    Text(
        "Secure Your Digital Life",
        style = MaterialTheme.typography.headlineMedium,
        color = OrionColors.Primary,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun InfoCard(icon: ImageVector, iconTint: Color, title: String, titleColor: Color, description: String) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), padding = 20.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier.size(36.dp).background(iconTint.copy(alpha = 0.1f), MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor)
                Spacer(Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = OrionColors.OnSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SetMasterPasswordStep(error: String?, onCreate: (CharArray, CharArray) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        InfoCard(
            icon = Icons.Filled.Key,
            iconTint = OrionColors.Primary,
            title = "Master Password",
            titleColor = OrionColors.OnSurface,
            description = "A única senha que você precisa lembrar. Ela desbloqueia todo o seu cofre e criptografa seus dados localmente neste dispositivo."
        )

        InfoCard(
            icon = Icons.Filled.EnhancedEncryption,
            iconTint = OrionColors.Secondary,
            title = "Secret Key",
            titleColor = OrionColors.Secondary,
            description = "Um identificador único gerado no seu dispositivo. Combinado com sua senha, protege matematicamente contra ataques de força bruta."
        )

        VaultTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = "Master Password",
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = OrionColors.Outline) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            textStyle = CodeTextStyle
        )

        VaultTextField(
            value = confirm,
            onValueChange = { confirm = it },
            modifier = Modifier.fillMaxWidth(),
            label = "Confirmar Master Password",
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = OrionColors.Outline) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            textStyle = CodeTextStyle
        )

        error?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = OrionColors.Error)
        }

        Button(
            onClick = { onCreate(password.toCharArray(), confirm.toCharArray()) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(
                containerColor = OrionColors.SecondaryContainer,
                contentColor = OrionColors.OnSecondaryContainer
            )
        ) {
            Text("Criar Cofre", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun ShowSecretKeyStep(secretKey: String, onConfirm: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var confirmed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        InfoCard(
            icon = Icons.Filled.EnhancedEncryption,
            iconTint = OrionColors.Secondary,
            title = "Secret Key",
            titleColor = OrionColors.Secondary,
            description = "Um identificador único gerado no seu dispositivo. Combinado com sua senha, protege matematicamente contra ataques de força bruta."
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(OrionColors.SurfaceContainerLowest, MaterialTheme.shapes.small)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    "GENERATED SECRET KEY",
                    style = MaterialTheme.typography.labelSmall,
                    color = OrionColors.Outline
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        secretKey,
                        style = CodeTextStyle,
                        color = OrionColors.SecondaryFixedDim,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "Copiar Secret Key",
                        tint = OrionColors.OnSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { clipboard.setText(AnnotatedString(secretKey)) }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(OrionColors.ErrorContainer.copy(alpha = 0.2f), MaterialTheme.shapes.medium)
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = OrionColors.Error, modifier = Modifier.size(18.dp))
                    Text(
                        "SECURITY WARNING",
                        style = MaterialTheme.typography.labelSmall,
                        color = OrionColors.Error,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Sua Secret Key é seu único método de recuperação. Se você perdê-la, o OrionVault não poderá recuperar seus dados. Guarde-a offline e nunca a compartilhe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OrionColors.OnErrorContainer
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = confirmed,
                onCheckedChange = { confirmed = it },
                colors = CheckboxDefaults.colors(checkedColor = OrionColors.Secondary, checkmarkColor = OrionColors.OnSecondary)
            )
            Text(
                "Eu salvei minha Secret Key em um local seguro",
                style = MaterialTheme.typography.bodyMedium,
                color = OrionColors.OnSurface
            )
        }

        Button(
            onClick = onConfirm,
            enabled = confirmed,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(
                containerColor = OrionColors.SecondaryContainer,
                contentColor = OrionColors.OnSecondaryContainer
            )
        ) {
            Icon(Icons.Filled.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Já salvei minha Secret Key", style = MaterialTheme.typography.titleMedium)
        }

        Text(
            "Ao continuar, você concorda com nosso Protocolo de Segurança e com os padrões de criptografia ponta a ponta.",
            style = MaterialTheme.typography.labelSmall,
            color = OrionColors.Outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}
