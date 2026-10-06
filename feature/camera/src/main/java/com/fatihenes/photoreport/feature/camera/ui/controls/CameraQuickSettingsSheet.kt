package com.fatihenes.photoreport.feature.camera.ui.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatihenes.photoreport.feature.camera.theme.CameraTokens

@Composable
@Suppress("LongMethod", "UnusedParameter")
fun CameraQuickSettingsSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    enableOptimization: Boolean,
    onToggleOptimization: (Boolean) -> Unit,
    enableHeic: Boolean,
    onToggleHeic: (Boolean) -> Unit,
    enableAvif: Boolean,
    onToggleAvif: (Boolean) -> Unit,
    isLevelVisible: Boolean,
    onToggleLevel: (Boolean) -> Unit,
    isGridVisible: Boolean,
    onToggleGrid: (Boolean) -> Unit,
    gpsWatermarkEnabled: Boolean,
    onToggleGpsWatermark: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFF1C1C1E))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume click inside sheet
                    )
                    .padding(24.dp)
            ) {
                // Header drag pill
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 20.dp)
                    )
                }

                Text(
                    text = "Kamera Tercihleri",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                SettingsToggleRow(
                    title = "Donanım HDR & Optimizasyon",
                    description = "Daha geniş dinamik aralık ve gürültü azaltma",
                    checked = enableOptimization,
                    onCheckedChange = onToggleOptimization
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 10.dp))

                SettingsToggleRow(
                    title = "AVIF / HEIC Yüksek Verimli Kayıt",
                    description = "Fotoğrafların dosya boyutunu %50 düşürür, görsel kaliteyi korur",
                    checked = enableAvif,
                    onCheckedChange = onToggleAvif
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 10.dp))

                SettingsToggleRow(
                    title = "Dahili Terazi (Su Terazisi)",
                    description = "Ufuk çizgisi ve tavan/zemin hizalama göstergesi",
                    checked = isLevelVisible,
                    onCheckedChange = onToggleLevel
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 10.dp))

                SettingsToggleRow(
                    title = "Kılavuz Çizgileri (3x3)",
                    description = "Fotoğraf kadrajını altın oranla hizalar",
                    checked = isGridVisible,
                    onCheckedChange = onToggleGrid
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 10.dp))

                SettingsToggleRow(
                    title = "Adli GPS & Tarih Filigranı",
                    description = "Fotoğraf üzerine koordinat ve saat damgası ekler",
                    checked = gpsWatermarkEnabled,
                    onCheckedChange = onToggleGpsWatermark
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = Color.LightGray,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = CameraTokens.Amber,
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = Color.DarkGray
            ),
            modifier = Modifier.scale(0.85f)
        )
    }
}
