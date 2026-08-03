package com.fatihenes.photoreport.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * FotoRapor Kurumsal Design System - Standart Geri Bildirim ve Durum Bildirim Bileşeni
 * Başarılı işlem, hata bildirimleri veya bilgi iletilerini ikon destekli olarak sunar.
 */
enum class SnackbarType {
    INFO, SUCCESS, ERROR
}

@Composable
fun AppSnackbarHost(
    hostState: SnackbarHostState,
    type: SnackbarType = SnackbarType.INFO,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { data ->
        val (containerColor, icon, tint) = when (type) {
            SnackbarType.SUCCESS -> Triple(Color(0xFF065F46), Icons.Default.CheckCircle, Color.White)
            SnackbarType.ERROR -> Triple(MaterialTheme.colorScheme.error, Icons.Default.ErrorOutline, Color.White)
            SnackbarType.INFO -> Triple(MaterialTheme.colorScheme.primary, Icons.Default.Info, Color.White)
        }

        Snackbar(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            containerColor = containerColor,
            contentColor = Color.White
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}
