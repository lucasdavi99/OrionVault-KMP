package com.cuboidestudio.orionvault.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cuboidestudio.orionvault.ui.components.OrionBackground
import com.cuboidestudio.orionvault.ui.components.OrionButton
import com.cuboidestudio.orionvault.ui.components.OrionButtonVariant
import com.cuboidestudio.orionvault.ui.components.OrionSurface
import com.cuboidestudio.orionvault.ui.components.OrionTextField
import com.cuboidestudio.orionvault.ui.theme.OrionMotion
import com.cuboidestudio.orionvault.ui.theme.OrionShapes
import com.cuboidestudio.orionvault.ui.theme.OrionSizes
import com.cuboidestudio.orionvault.ui.theme.OrionSpacing
import com.cuboidestudio.orionvault.ui.theme.OrionTheme
import com.cuboidestudio.orionvault.viewmodel.OnboardingStep
import com.cuboidestudio.orionvault.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel, onCompleted: () -> Unit) {
    val step by viewModel.step.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val completed by viewModel.completed.collectAsState()

    LaunchedEffect(completed) {
        if (completed) onCompleted()
    }

    OrionBackground(intensity = 1.5f) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = OrionSizes.contentNarrow)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = OrionSpacing.screenH, vertical = OrionSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OnboardingHeader()

                Spacer(Modifier.height(OrionSpacing.lg))

                StepIndicator(current = step.orderIndex, total = TOTAL_STEPS)

                Spacer(Modifier.height(OrionSpacing.xl))

                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        val forward = targetState.orderIndex >= initialState.orderIndex
                        val dir = if (forward) 1 else -1
                        (
                            slideInHorizontally(OrionMotion.tweenMedium()) { it / 4 * dir } +
                                fadeIn(OrionMotion.tweenMedium())
                            ) togetherWith (
                            slideOutHorizontally(OrionMotion.tweenMedium()) { -it / 4 * dir } +
                                fadeOut(OrionMotion.tweenFast())
                            ) using SizeTransform(clip = false)
                    },
                    label = "onboardingStep"
                ) { currentStep ->
                    when (currentStep) {
                        is OnboardingStep.ChooseMode -> ChooseModeStep(
                            onCreate = viewModel::chooseCreate,
                            onRestore = viewModel::chooseRestore
                        )

                        is OnboardingStep.SetMasterPassword -> SetMasterPasswordStep(
                            error = error,
                            onCreate = { password, confirm -> viewModel.createVault(password, confirm) }
                        )

                        is OnboardingStep.ShowSecretKey -> ShowSecretKeyStep(
                            secretKey = currentStep.secretKeyDisplay,
                            onConfirm = viewModel::confirmSecretKeySaved
                        )

                        is OnboardingStep.RestoreLogin -> RestoreLoginStep(
                            error = error,
                            isLoading = isLoading,
                            onSubmit = { email, password -> viewModel.restoreLogin(email, password) },
                            onBack = viewModel::backToChooseMode
                        )

                        is OnboardingStep.RestoreVaultSecrets -> RestoreVaultSecretsStep(
                            error = error,
                            isLoading = isLoading,
                            onSubmit = { password, confirm, secretKey ->
                                viewModel.restoreVault(password, confirm, secretKey)
                            }
                        )
                    }
                }
            }
        }
    }
}

private const val TOTAL_STEPS = 3

/** Posição do passo na trilha, usada pelo indicador e pela direção da transição. */
private val OnboardingStep.orderIndex: Int
    get() = when (this) {
        is OnboardingStep.ChooseMode -> 0
        is OnboardingStep.SetMasterPassword -> 1
        is OnboardingStep.RestoreLogin -> 1
        is OnboardingStep.ShowSecretKey -> 2
        is OnboardingStep.RestoreVaultSecrets -> 2
    }

@Composable
private fun OnboardingHeader() {
    val primary = MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .background(
                    Brush.radialGradient(listOf(primary.copy(alpha = 0.22f), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(36.dp)
            )
        }
    }

    Spacer(Modifier.height(OrionSpacing.md))

    Text(
        text = "Proteja sua vida digital",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun StepIndicator(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(OrionSpacing.xs)) {
        repeat(total) { index ->
            val active = index <= current
            val color by animateColorAsState(
                targetValue = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                label = "stepDot"
            )
            val width by animateFloatAsState(
                targetValue = if (index == current) 28f else 8f,
                label = "stepWidth"
            )
            Box(
                modifier = Modifier
                    .width(width.dp)
                    .height(6.dp)
                    .background(color, OrionShapes.Pill)
            )
        }
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    OrionSurface(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(OrionSpacing.md)) {
            Box(
                modifier = Modifier
                    .size(OrionSizes.avatarSm)
                    .background(iconTint.copy(alpha = 0.14f), OrionShapes.Input),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(OrionSizes.iconSm)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(OrionSpacing.xxs))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StepColumn(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(OrionSpacing.md),
        content = content
    )
}

@Composable
private fun ChooseModeStep(onCreate: () -> Unit, onRestore: () -> Unit) {
    StepColumn {
        InfoCard(
            icon = Icons.Filled.Key,
            iconTint = MaterialTheme.colorScheme.primary,
            title = "Primeiro dispositivo",
            description = "Crie um cofre novo: sua Secret Key é gerada aqui mesmo e exibida uma única vez para você guardar."
        )

        InfoCard(
            icon = Icons.Filled.CloudSync,
            iconTint = MaterialTheme.colorScheme.secondary,
            title = "Já tenho um cofre",
            description = "Entre na sua conta e traga o cofre existente para este aparelho usando a Secret Key que você anotou."
        )

        OrionButton(
            text = "Criar cofre novo",
            onClick = onCreate,
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            modifier = Modifier.fillMaxWidth()
        )

        OrionButton(
            text = "Já tenho uma Secret Key",
            onClick = onRestore,
            variant = OrionButtonVariant.Ghost,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SetMasterPasswordStep(error: String?, onCreate: (CharArray, CharArray) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    StepColumn {
        InfoCard(
            icon = Icons.Filled.Key,
            iconTint = MaterialTheme.colorScheme.primary,
            title = "Master Password",
            description = "A única senha que você precisa lembrar. Ela destrava todo o cofre e criptografa seus dados localmente neste dispositivo."
        )

        InfoCard(
            icon = Icons.Filled.EnhancedEncryption,
            iconTint = MaterialTheme.colorScheme.secondary,
            title = "Secret Key",
            description = "Um identificador único gerado no seu dispositivo. Combinada com sua senha, protege matematicamente contra ataques de força bruta."
        )

        OrionSurface(modifier = Modifier.fillMaxWidth()) {
            OrionTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = "Master Password",
                mono = true,
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                )
            )

            Spacer(Modifier.height(OrionSpacing.md))

            OrionTextField(
                value = confirm,
                onValueChange = { confirm = it },
                modifier = Modifier.fillMaxWidth(),
                label = "Confirmar Master Password",
                mono = true,
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                ),
                error = error
            )
        }

        OrionButton(
            text = "Criar cofre",
            onClick = { onCreate(password.toCharArray(), confirm.toCharArray()) },
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            enabled = password.isNotEmpty() && confirm.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RestoreLoginStep(
    error: String?,
    isLoading: Boolean,
    onSubmit: (String, CharArray) -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    StepColumn {
        InfoCard(
            icon = Icons.Filled.CloudSync,
            iconTint = MaterialTheme.colorScheme.secondary,
            title = "Conta na nuvem",
            description = "Entre com as credenciais usadas no outro dispositivo. É de lá que vêm os parâmetros para recriar a chave do cofre — sua Master Password e sua Secret Key continuam sem sair daqui."
        )

        OrionSurface(modifier = Modifier.fillMaxWidth()) {
            OrionTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = "E-mail da conta",
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                )
            )

            Spacer(Modifier.height(OrionSpacing.md))

            OrionTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = "Senha da conta",
                mono = true,
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                ),
                error = error
            )
        }

        OrionButton(
            text = "Entrar",
            onClick = { onSubmit(email.trim(), password.toCharArray()) },
            loading = isLoading,
            enabled = email.isNotBlank() && password.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        )

        OrionButton(
            text = "Voltar",
            onClick = onBack,
            variant = OrionButtonVariant.Ghost,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RestoreVaultSecretsStep(
    error: String?,
    isLoading: Boolean,
    onSubmit: (CharArray, CharArray, String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }

    StepColumn {
        InfoCard(
            icon = Icons.Filled.EnhancedEncryption,
            iconTint = MaterialTheme.colorScheme.secondary,
            title = "Restaurar cofre",
            description = "Digite a mesma Master Password e a Secret Key do dispositivo original. Só a combinação exata das duas reproduz a chave que decifra seus itens."
        )

        OrionSurface(modifier = Modifier.fillMaxWidth()) {
            OrionTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = "Master Password",
                mono = true,
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                )
            )

            Spacer(Modifier.height(OrionSpacing.md))

            OrionTextField(
                value = confirm,
                onValueChange = { confirm = it },
                modifier = Modifier.fillMaxWidth(),
                label = "Confirmar Master Password",
                mono = true,
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Password
                )
            )

            Spacer(Modifier.height(OrionSpacing.md))

            OrionTextField(
                value = secretKey,
                onValueChange = { secretKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = "Secret Key",
                placeholder = "A3F9-7C1D-...",
                mono = true,
                leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
                error = error
            )
        }

        OrionButton(
            text = "Restaurar cofre",
            onClick = { onSubmit(password.toCharArray(), confirm.toCharArray(), secretKey) },
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            loading = isLoading,
            enabled = password.isNotEmpty() && secretKey.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ShowSecretKeyStep(secretKey: String, onConfirm: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var confirmed by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(1500)
            copied = false
        }
    }

    StepColumn {
        InfoCard(
            icon = Icons.Filled.EnhancedEncryption,
            iconTint = MaterialTheme.colorScheme.secondary,
            title = "Sua Secret Key",
            description = "Gerada agora, neste dispositivo, e exibida uma única vez. Combinada com sua Master Password, é ela que produz a chave do cofre."
        )

        // A chave é exibida em blocos, em JetBrains Mono: este é o texto que o usuário vai copiar à
        // mão para um papel, então distinguir l/I/1 e 0/O importa mais aqui do que em qualquer
        // outro lugar do app.
        OrionSurface(
            modifier = Modifier.fillMaxWidth(),
            padding = PaddingValues(OrionSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SECRET KEY GERADA",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = {
                    clipboard.setText(AnnotatedString(secretKey))
                    copied = true
                }) {
                    Icon(
                        imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                        contentDescription = "Copiar Secret Key",
                        tint = if (copied) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(OrionSizes.iconSm)
                    )
                }
            }

            Spacer(Modifier.height(OrionSpacing.xs))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, OrionShapes.Input)
                    .padding(OrionSpacing.md)
            ) {
                Text(
                    text = secretKey,
                    style = OrionTheme.type.codeLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Aviso destrutivo: fundo de erro contido, não só texto vermelho.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    OrionShapes.Card
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
                    OrionShapes.Card
                )
                .padding(OrionSpacing.md)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(OrionSpacing.xs)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(OrionSizes.iconSm)
                    )
                    Text(
                        text = "AVISO DE SEGURANÇA",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(OrionSpacing.xs))
                Text(
                    text = "Esta é sua única forma de recuperação. Se você perdê-la, nem o " +
                        "OrionVault consegue recuperar seus dados. Guarde-a offline e nunca a compartilhe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { confirmed = !confirmed },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = confirmed,
                onCheckedChange = { confirmed = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.secondary,
                    checkmarkColor = MaterialTheme.colorScheme.onSecondary
                )
            )
            Text(
                text = "Salvei minha Secret Key em um lugar seguro",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        OrionButton(
            text = "Continuar para o cofre",
            onClick = onConfirm,
            icon = Icons.Filled.Check,
            enabled = confirmed,
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedVisibility(visible = !confirmed) {
            Text(
                text = "Confirme que guardou a chave para continuar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
