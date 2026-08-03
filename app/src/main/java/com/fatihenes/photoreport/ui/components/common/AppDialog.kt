package com.fatihenes.photoreport.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * FotoRapor Kurumsal Design System - Standart Modal Bildirim ve Onay Penceresi
 * Kullanıcı aksiyonlarını sade, şık ve okunaklı biçimde yönetir.
 */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    title: String,
    description: String,
    icon: ImageVector? = null,
    confirmButtonText: String = "Onayla",
    onConfirmClick: () -> Unit,
    dismissButtonText: String? = "İptal",
    onDismissClick: (() -> Unit)? = onDismissRequest,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    confirmButtonColor: Color = MaterialTheme.colorScheme.primary,
    dismissOnOutsideClick: Boolean = true
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnClickOutside = dismissOnOutsideClick)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(iconTint.copy(alpha = 0.15f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                AppButton(
                    text = confirmButtonText,
                    onClick = onConfirmClick,
                    fullWidth = true,
                    containerColor = confirmButtonColor
                )

                if (dismissButtonText != null && onDismissClick != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextButton(
                        text = dismissButtonText,
                        onClick = onDismissClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
