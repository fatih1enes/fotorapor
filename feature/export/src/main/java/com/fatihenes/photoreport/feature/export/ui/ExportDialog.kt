@file:Suppress("LocalContextGetResourceValueCall")
package com.fatihenes.photoreport.feature.export.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
import com.fatihenes.photoreport.core.designsystem.theme.PetrolPrimary
import com.fatihenes.photoreport.core.designsystem.theme.WarningColor
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.core.model.DailyLogWithPhotos
import com.fatihenes.photoreport.core.model.Project
import com.fatihenes.photoreport.core.ui.navigation.LocalSnackbarHostState
import com.fatihenes.photoreport.feature.export.viewmodel.ExportViewModel
import com.fatihenes.photoreport.core.common.model.FileSizeInfo
import kotlinx.coroutines.launch

enum class ExportFormat { PDF, ZIP }

private const val MB_FORMAT = "%.1f MB"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(
    project: Project,
    logs: List<DailyLogWithPhotos>,
    onDismiss: () -> Unit,
    onExportPdf: (Int) -> Unit,
    onExportZip: (Int) -> Unit,
    viewModel: ExportViewModel = hiltViewModel(),
) {
    val allPhotos = remember(logs) { logs.flatMap { it.photos } }
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf<ExportFormat?>(null) }
    var selectedQuality by remember { mutableIntStateOf(100) }
    var isExporting by remember { mutableStateOf(false) }
    val snackbarHost = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()

    val fileSizes by viewModel.fileSizeInfo.collectAsStateWithLifecycle()
    val isCalculatingSizes = fileSizes == null

    LaunchedEffect(allPhotos) { viewModel.calculateFileSizes(allPhotos) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = FotoRaporTokens.RadiusXL, topEnd = FotoRaporTokens.RadiusXL)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = FotoRaporTokens.ScreenPaddingHorizontal).padding(bottom = FotoRaporTokens.Spacing3XL)) {
            ExportHeader(project.name)
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXL))

            FormatSelectionRow(selectedFormat) { selectedFormat = it }
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingL))

            ExportOptionsAndSummary(
                selectedFormat = selectedFormat,
                selectedQuality = selectedQuality,
                onQualitySelect = { selectedQuality = it },
                fileSizes = fileSizes,
                isCalculatingSizes = isCalculatingSizes,
                logsCount = logs.size
            )

            ExportActionButtons(
                onDismiss = onDismiss,
                onShare = {
                    isExporting = true
                    coroutineScope.launch { snackbarHost.showSnackbar(context.getString(R.string.export_started_toast)) }
                    if (selectedFormat == ExportFormat.PDF) onExportPdf(selectedQuality) else if (selectedFormat == ExportFormat.ZIP) onExportZip(selectedQuality)
                    onDismiss()
                },
                isEnabled = selectedFormat != null && !isCalculatingSizes && !isExporting,
                isExporting = isExporting
            )
        }
    }
}

@Composable
private fun ExportHeader(projectName: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingS))
        Icon(Icons.Default.Share, stringResource(R.string.acc_share), modifier = Modifier.size(FotoRaporTokens.IconSizeL + 4.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingM))
        Text(stringResource(R.string.export_share_project), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXS))
        Text(projectName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun FormatSelectionRow(selectedFormat: ExportFormat?, onFormatSelect: (ExportFormat) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingM)) {
        FormatCard(modifier = Modifier.weight(1f), icon = Icons.Default.PictureAsPdf, title = "PDF", description = stringResource(R.string.export_pdf_desc), isSelected = selectedFormat == ExportFormat.PDF, accentColor = Color(0xFFB23327), onClick = { onFormatSelect(ExportFormat.PDF) })
        FormatCard(modifier = Modifier.weight(1f), icon = Icons.Default.FolderZip, title = "ZIP", description = stringResource(R.string.export_zip_desc), isSelected = selectedFormat == ExportFormat.ZIP, accentColor = PetrolPrimary, onClick = { onFormatSelect(ExportFormat.ZIP) })
    }
}

@Composable
private fun ExportOptionsAndSummary(
    selectedFormat: ExportFormat?,
    selectedQuality: Int,
    onQualitySelect: (Int) -> Unit,
    fileSizes: FileSizeInfo?,
    isCalculatingSizes: Boolean,
    logsCount: Int
) {
    AnimatedVisibility(visible = selectedFormat != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Column {
            Text(stringResource(R.string.export_compression_quality).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 1.5f, modifier = Modifier.padding(start = FotoRaporTokens.SpacingXS, bottom = FotoRaporTokens.SpacingS))
            QualitySelectionRow(selectedQuality, onQualitySelect)
            ExportSummaryBox(fileSizes, isCalculatingSizes, selectedFormat, selectedQuality, logsCount)
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingL))
        }
    }
}

@Composable
private fun QualitySelectionRow(selectedQuality: Int, onQualitySelect: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = FotoRaporTokens.SpacingL), horizontalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingS)) {
        QualityOption(Modifier.weight(1f), stringResource(R.string.export_quality_original), stringResource(R.string.export_quality_original_sub), selectedQuality == 100) { onQualitySelect(100) }
        QualityOption(Modifier.weight(1f), stringResource(R.string.export_quality_high), stringResource(R.string.export_quality_high_sub), selectedQuality == 85) { onQualitySelect(85) }
        QualityOption(Modifier.weight(1f), stringResource(R.string.export_quality_standard), stringResource(R.string.export_quality_standard_sub), selectedQuality == 75) { onQualitySelect(75) }
    }
}

@Composable
private fun ExportSummaryBox(fileSizes: FileSizeInfo?, isCalculatingSizes: Boolean, selectedFormat: ExportFormat?, selectedQuality: Int, logsCount: Int) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(FotoRaporTokens.RadiusM), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(modifier = Modifier.padding(FotoRaporTokens.SpacingL)) {
            Text(stringResource(R.string.export_content_summary).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 1.5f)
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingS))
            if (isCalculatingSizes) {
                CalculatingIndicator()
            } else if (fileSizes != null) {
                SummaryContent(fileSizes, selectedFormat, selectedQuality, logsCount)
            }
        }
    }
}

@Composable
private fun CalculatingIndicator() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = FotoRaporTokens.SpacingL), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(FotoRaporTokens.IconSizeM), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingM))
            Text(stringResource(R.string.export_calculating), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SummaryContent(fileSizes: FileSizeInfo, selectedFormat: ExportFormat?, selectedQuality: Int, logsCount: Int) {
    val totalPhotoSizeMB = fileSizes.totalPhotoBytes / (1024.0 * 1024.0)
    val totalVideoSizeMB = fileSizes.totalVideoBytes / (1024.0 * 1024.0)

    InfoRow(stringResource(R.string.export_photo_label), "${fileSizes.photoCount} adet", MB_FORMAT.format(totalPhotoSizeMB))
    if (fileSizes.videoCount > 0) {
        if (selectedFormat == ExportFormat.PDF) {
            InfoRow(stringResource(R.string.export_video_label), "${fileSizes.videoCount} adet (Oynatılamaz)", "Dahil edilmez (0.0 MB)")
        } else {
            InfoRow(stringResource(R.string.export_video_label), "${fileSizes.videoCount} adet (Oynatılabilir)", MB_FORMAT.format(totalVideoSizeMB))
        }
    }
    InfoRow(stringResource(R.string.export_day_label), "$logsCount gün", "")
    HorizontalDivider(modifier = Modifier.padding(vertical = FotoRaporTokens.SpacingS), thickness = FotoRaporTokens.DividerThickness, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

    if (selectedFormat == ExportFormat.PDF) {
        PdfEstimateRow(fileSizes, selectedQuality)
    } else {
        ZipEstimateRow(fileSizes, selectedQuality, totalVideoSizeMB)
    }
}

@Composable
private fun PdfEstimateRow(fileSizes: FileSizeInfo, selectedQuality: Int) {
    val estimatedPdfBytes = when (selectedQuality) {
        100 -> fileSizes.estimatedQ100Bytes + (fileSizes.photoCount * 50 * 1024L)
        85 -> fileSizes.estimatedQ85Bytes + (fileSizes.photoCount * 30 * 1024L)
        else -> fileSizes.estimatedQ75Bytes + (fileSizes.photoCount * 20 * 1024L)
    }
    val estimatedPdfMB = (estimatedPdfBytes / (1024.0 * 1024.0)).coerceAtLeast(0.1)
    Text(stringResource(R.string.export_pdf_estimate, MB_FORMAT.format(estimatedPdfMB)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    if (fileSizes.videoCount > 0) {
        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXS))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = WarningColor, modifier = Modifier.size(FotoRaporTokens.IconSizeXS))
            Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingXS))
            Text(stringResource(R.string.export_pdf_video_warning), style = MaterialTheme.typography.bodySmall, color = WarningColor)
        }
    }
}

@Composable
private fun ZipEstimateRow(fileSizes: FileSizeInfo, selectedQuality: Int, totalVideoSizeMB: Double) {
    val estimatedPhotoMB = (when (selectedQuality) {
        100 -> fileSizes.estimatedQ100Bytes
        85 -> fileSizes.estimatedQ85Bytes
        else -> fileSizes.estimatedQ75Bytes
    }) / (1024.0 * 1024.0)
    val totalMB = (estimatedPhotoMB + totalVideoSizeMB).coerceAtLeast(0.1)
    Text(stringResource(R.string.export_zip_estimate, MB_FORMAT.format(totalMB)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXS))
    Text(if (selectedQuality == 100) stringResource(R.string.export_zip_original_desc) else stringResource(R.string.export_zip_compressed_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ExportActionButtons(onDismiss: () -> Unit, onShare: () -> Unit, isEnabled: Boolean, isExporting: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingM)) {
        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(FotoRaporTokens.ButtonHeightL), shape = RoundedCornerShape(FotoRaporTokens.RadiusM), border = BorderStroke(FotoRaporTokens.CardBorderWidth, MaterialTheme.colorScheme.outline)) {
            Text(stringResource(R.string.export_cancel_btn), style = MaterialTheme.typography.labelLarge)
        }
        Button(
            onClick = onShare, modifier = Modifier.weight(1f).height(FotoRaporTokens.ButtonHeightL), shape = RoundedCornerShape(FotoRaporTokens.RadiusM), enabled = isEnabled,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = FotoRaporTokens.ElevationNone, pressedElevation = FotoRaporTokens.ElevationNone),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
        ) {
            if (isExporting) {
                CircularProgressIndicator(modifier = Modifier.size(FotoRaporTokens.IconSizeXS + 2.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Share, stringResource(R.string.acc_share), modifier = Modifier.size(FotoRaporTokens.IconSizeXS + 2.dp))
            }
            Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingXS))
            Text(if (isExporting) stringResource(R.string.export_notif_title) else stringResource(R.string.export_share_btn), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FormatCard(modifier: Modifier = Modifier, icon: ImageVector, title: String, description: String, isSelected: Boolean, accentColor: Color, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (isSelected) accentColor.copy(alpha = if (isDark) 0.12f else 0.06f) else Color.Transparent
    Surface(modifier = modifier.clip(RoundedCornerShape(FotoRaporTokens.RadiusM)).border(if (isSelected) 1.5.dp else FotoRaporTokens.CardBorderWidth, borderColor, RoundedCornerShape(FotoRaporTokens.RadiusM)).clickable(onClick = onClick), shape = RoundedCornerShape(FotoRaporTokens.RadiusM), color = bgColor) {
        Column(modifier = Modifier.padding(FotoRaporTokens.SpacingL).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, title, tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(FotoRaporTokens.IconSizeL))
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingS))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXS))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, size: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = FotoRaporTokens.SpacingXXS + 1.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row {
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            if (size.isNotEmpty()) Text("  ·  $size", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QualityOption(modifier: Modifier = Modifier, title: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainerLow
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    Surface(modifier = modifier.clip(RoundedCornerShape(FotoRaporTokens.RadiusS)).border(if (isSelected) 1.5.dp else 0.dp, borderColor, RoundedCornerShape(FotoRaporTokens.RadiusS)).clickable(onClick = onClick), shape = RoundedCornerShape(FotoRaporTokens.RadiusS), color = bgColor) {
        Column(modifier = Modifier.padding(vertical = FotoRaporTokens.SpacingS, horizontal = FotoRaporTokens.SpacingXS).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
