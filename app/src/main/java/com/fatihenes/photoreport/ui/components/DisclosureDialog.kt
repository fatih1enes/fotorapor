import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatihenes.photoreport.R
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
import com.fatihenes.photoreport.core.ui.components.AppButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisclosureDialog(
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(500)) + expandVertically(tween(500)),
        exit = fadeOut(tween(300)) + shrinkVertically(tween(300))
    ) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismissal */ },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            ),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(FotoRaporTokens.RadiusXL),
                tonalElevation = FotoRaporTokens.ElevationL,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(FotoRaporTokens.SpacingXXL)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PrivacyTip,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(FotoRaporTokens.IconSizeL)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXL))

                    Text(
                        text = stringResource(R.string.disclosure_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
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

                    Spacer(modifier = Modifier.height(FotoRaporTokens.Spacing4XL))

                    AppButton(
                        text = stringResource(R.string.disclosure_button),
                        onClick = onDismiss,
                        fullWidth = true
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
