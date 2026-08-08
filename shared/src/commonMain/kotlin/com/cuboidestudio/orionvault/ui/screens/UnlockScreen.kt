package com.cuboidestudio.orionvault.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cuboidestudio.orionvault.ui.components.OrionBackground
import com.cuboidestudio.orionvault.ui.components.OrionButton
import com.cuboidestudio.orionvault.ui.components.OrionTextField
import com.cuboidestudio.orionvault.ui.theme.OrionSizes
import com.cuboidestudio.orionvault.ui.theme.OrionSpacing
import com.cuboidestudio.orionvault.viewmodel.UnlockViewModel

/**
 * Tela de destravamento — a primeira coisa que o usuário vê a cada abertura do app.
 *
 * Recebe o tratamento mais forte do fundo ambiente e uma marca do cadeado com respiração lenta,
 * porque é o momento em que a promessa do produto ("cofre") precisa ser sentida.
 */
@Composable
fun UnlockScreen(viewModel: UnlockViewModel, onUnlocked: () -> Unit) {
    val error by viewModel.errorMessage.collectAsState()
    val unlocked by viewModel.unlocked.collectAsState()
    var password by remember { mutableStateOf("") }

    LaunchedEffect(unlocked) {
        if (unlocked) onUnlocked()
    }

    val submit = { viewModel.attemptUnlock(password.toCharArray()) }

    OrionBackground(intensity = 1.8f) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = OrionSizes.contentNarrow)
                    .fillMaxWidth()
                    .padding(horizontal = OrionSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BreathingLockMark()

                Spacer(Modifier.height(OrionSpacing.xl))

                Text(
                    text = "Cofre trancado",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(OrionSpacing.xs))

                Text(
                    text = "Digite sua Master Password para destravar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(OrionSpacing.xl))

                OrionTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Master Password",
                    mono = true,
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    error = error
                )

                Spacer(Modifier.height(OrionSpacing.lg))

                OrionButton(
                    text = "Destravar cofre",
                    onClick = submit,
                    enabled = password.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(OrionSpacing.lg))

                Text(
                    text = "Sem a Master Password e a Secret Key corretas, não há como recuperar o cofre.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Cadeado com halo que pulsa devagar — sinaliza "protegendo" sem virar spinner. */
@Composable
private fun BreathingLockMark() {
    val transition = rememberInfiniteTransition(label = "lockBreath")
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val primary = MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                    alpha = 0.55f
                }
                .background(
                    Brush.radialGradient(listOf(primary.copy(alpha = 0.28f), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}
