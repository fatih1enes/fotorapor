package com.fatihenes.photoreport.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens

/**
 * FotoRapor Design System — Empty State
 *
 * 3 adımlı yönlendirme kuralı:
 * 1. Mevcut Durum / Sorun Ne? (title)
 * 2. Neden Böyle? (description)
 * 3. Şimdi Ne Yapmalı? (action buton veya hint)
 *
 * İkon hafifçe yukarı-aşağı hareket ederek canlılık hissi verir.
 */
@Composable
fun AppEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    actionHintText: String? = null
) {
    // Gentle floating animation for the icon
    val infiniteTransition = rememberInfiniteTransition(label = "empty_state_float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FotoRaporMotion.EasingStandard),
            repeatMode = RepeatMode.Reverse
        ),
        label = "empty_state_float_offset"
    )

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(FotoRaporMotion.enterTween()) + scaleIn(initialScale = 0.95f)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    horizontal = FotoRaporTokens.Spacing3XL,
                    vertical = FotoRaporTokens.Spacing5XL
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon container with floating effect
            Surface(
                shape = RoundedCornerShape(FotoRaporTokens.RadiusL),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer { translationY = floatOffset }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(FotoRaporTokens.IconSizeL),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXL))

            // 1. What happened?
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingS))

            // 2. Why?
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.85f)
            )

            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXL))

            // 3. What to do?
            if (actionLabel != null && onActionClick != null) {
                AppButton(
                    text = actionLabel,
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth(0.7f)
                )
            } else if (!actionHintText.isNullOrBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Text(
                        text = actionHintText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(
                            vertical = FotoRaporTokens.SpacingM,
                            horizontal = FotoRaporTokens.SpacingL
                        )
                    )
                }
            }
        }
    }
}
