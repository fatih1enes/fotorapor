package com.fatihenes.photoreport.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.fatihenes.photoreport.R
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisclosureDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Prevent dismissal by clicking outside */ },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(FotoRaporTokens.RadiusXL),
            tonalElevation = FotoRaporTokens.ElevationL,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(FotoRaporTokens.SpacingXXL)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.PrivacyTip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(FotoRaporTokens.IconSizeXL)
                )
                Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingL))
                Text(
                    text = stringResource(R.string.disclosure_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXL))

                DisclosureItem(
                    icon = Icons.Default.CameraAlt,
                    title = stringResource(R.string.camera_btn),
                    description = stringResource(R.string.disclosure_camera_desc)
                )

                Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXL))

                DisclosureItem(
                    icon = Icons.Default.LocationOn,
                    title = stringResource(R.string.settings_gps_watermark_title),
                    description = stringResource(R.string.disclosure_location_desc)
                )

                Spacer(modifier = Modifier.height(FotoRaporTokens.Spacing3XL))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(FotoRaporTokens.ButtonHeightL),
                    shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = FotoRaporTokens.ElevationNone,
                        pressedElevation = FotoRaporTokens.ElevationNone
                    )
                ) {
                    Text(
                        stringResource(R.string.disclosure_button),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun DisclosureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(FotoRaporTokens.IconSizeM)
        )
        Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingL))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXS))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}
