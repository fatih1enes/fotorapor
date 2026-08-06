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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
import com.fatihenes.photoreport.core.designsystem.theme.WarningColor
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.core.model.DailyLogWithPhotos
import com.fatihenes.photoreport.core.model.Project
import com.fatihenes.photoreport.core.ui.navigation.LocalSnackbarHostState
import com.fatihenes.photoreport.feature.export.viewmodel.ExportViewModel
import kotlinx.coroutines.launch

enum class ExportFormat { PDF, ZIP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(
    project: Project,
    logs: List<DailyLogWithPhotos>,
    onDismiss: () -> Unit,
    onExportPdf: (Int) -> Unit,
    onExportZip: (Int) -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
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

    LaunchedEffect(allPhotos) {
        viewModel.calculateFileSizes(allPhotos)
    }

    val totalPhotoSizeMB = (fileSizes?.totalPhotoBytes ?: 0L) / (1024.0 * 1024.0)
    val totalVideoSizeMB = (fileSizes?.totalVideoBytes ?: 0L) / (1024.0 * 1024.0)
    val videoCount = fileSizes?.videoCount ?: 0
    val photoCount = fileSizes?.photoCount ?: 0

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = FotoRaporTokens.RadiusXL, topEnd = FotoRaporTokens.RadiusXL)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FotoRaporTokens.ScreenPaddingHorizontal)
                .padding(bottom = FotoRaporTokens.Spacing3XL)
        ) {
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingS))

            // Header
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = stringResource(R.string.acc_share),
                modifier = Modifier
                    .size(FotoRaporTokens.IconSizeL + 4.dp)
                    .align(Alignment.CenterHorizontally),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingM))
            Text(
                text = stringResource(R.string.export_share_project),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXS))
            Text(
                text = project.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXL))

            // Format Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingM)
            ) {
                FormatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.PictureAsPdf,
                    title = "PDF",
                    description = stringResource(R.string.export_pdf_desc),
                    isSelected = selectedFormat == ExportFormat.PDF,
                    accentColor = Color(0xFFDC2626),
                    onClick = { selectedFormat = ExportFormat.PDF }
                )
                FormatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.FolderZip,
                    title = "ZIP",
                    description = stringResource(R.string.export_zip_desc),
                    isSelected = selectedFormat == ExportFormat.ZIP,
                    accentColor = Color(0xFF2563EB),
                    onClick = { selectedFormat = ExportFormat.ZIP }
                )
            }

            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingL))

            // Quality & Summary (expandable)
            AnimatedVisibility(
                visible = selectedFormat != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.export_compression_quality).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 1.5f,
                        modifier = Modifier.padding(
                            start = FotoRaporTokens.SpacingXS,
                            bottom = FotoRaporTokens.SpacingS
                        )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = FotoRaporTokens.SpacingL),
                        horizontalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingS)
                    ) {
                        QualityOption(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.export_quality_original),
                            subtitle = stringResource(R.string.export_quality_original_sub),
                            isSelected = selectedQuality == 100,
                            onClick = { selectedQuality = 100 }
                        )
                        QualityOption(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.export_quality_high),
                            subtitle = stringResource(R.string.export_quality_high_sub),
                            isSelected = selectedQuality == 85,
                            onClick = { selectedQuality = 85 }
                        )
                        QualityOption(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.export_quality_standard),
                            subtitle = stringResource(R.string.export_quality_standard_sub),
                            isSelected = selectedQuality == 75,
                            onClick = { selectedQuality = 75 }
                        )
                    }

                    // Content Summary
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Column(modifier = Modifier.padding(FotoRaporTokens.SpacingL)) {
                            Text(
                                stringResource(R.string.export_content_summary).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 1.5f
                            )
                            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingS))

                            if (isCalculatingSizes) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = FotoRaporTokens.SpacingL),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(FotoRaporTokens.IconSizeM),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingM))
                                        Text(
                                            stringResource(R.string.export_calculating),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                InfoRow(stringResource(R.string.export_photo_label), "$photoCount adet", "%.1f MB".format(totalPhotoSizeMB))
                                if (videoCount > 0) {
                                    if (selectedFormat == ExportFormat.PDF) {
                                        InfoRow(stringResource(R.string.export_video_label), "$videoCount adet (Oynatılamaz)", "Dahil edilmez (0.0 MB)")
                                    } else {
                                        InfoRow(stringResource(R.string.export_video_label), "$videoCount adet (Oynatılabilir)", "%.1f MB".format(totalVideoSizeMB))
                                    }
                                }
                                InfoRow(stringResource(R.string.export_day_label), "${logs.size} gün", "")

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = FotoRaporTokens.SpacingS),
                                    thickness = FotoRaporTokens.DividerThickness,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                if (selectedFormat == ExportFormat.PDF) {
                                    val estimatedPdfBytes = when (selectedQuality) {
                                        100 -> (fileSizes?.estimatedQ100Bytes ?: 0L) + (photoCount * 50 * 1024L)
                                        85 -> (fileSizes?.estimatedQ85Bytes ?: 0L) + (photoCount * 30 * 1024L)
                                        else -> (fileSizes?.estimatedQ75Bytes ?: 0L) + (photoCount * 20 * 1024L)
                                    }
                                    val estimatedPdfMB = (estimatedPdfBytes / (1024.0 * 1024.0)).coerceAtLeast(0.1)
                                    Text(
                                        stringResource(R.string.export_pdf_estimate, "%.1f MB".format(estimatedPdfMB)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (videoCount > 0) {
                                        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXS))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = WarningColor,
                                                modifier = Modifier.size(FotoRaporTokens.IconSizeXS)
                                            )
                                            Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingXS))
                                            Text(
                                                stringResource(R.string.export_pdf_video_warning),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = WarningColor
                                            )
                                        }
                                    }
                                } else {
                                    val estimatedPhotoBytes = when (selectedQuality) {
                                        100 -> (fileSizes?.estimatedQ100Bytes ?: 0L)
                                        85 -> (fileSizes?.estimatedQ85Bytes ?: 0L)
                                        else -> (fileSizes?.estimatedQ75Bytes ?: 0L)
                                    }
                                    val estimatedPhotoMB = estimatedPhotoBytes / (1024.0 * 1024.0)
                                    val totalMB = (estimatedPhotoMB + totalVideoSizeMB).coerceAtLeast(0.1)
                                    Text(
                                        stringResource(R.string.export_zip_estimate, "%.1f MB".format(totalMB)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXS))
                                    Text(
                                        if (selectedQuality == 100) stringResource(R.string.export_zip_original_desc) else stringResource(R.string.export_zip_compressed_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingL))
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingM)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(FotoRaporTokens.ButtonHeightL),
                    shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
                    border = BorderStroke(
                        FotoRaporTokens.CardBorderWidth,
                        MaterialTheme.colorScheme.outline
                    )
                ) {
                    Text(
                        stringResource(R.string.export_cancel_btn),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Button(
                    onClick = {
                        isExporting = true
                        coroutineScope.launch { snackbarHost.showSnackbar(context.getString(R.string.export_started_toast)) }
                        when (selectedFormat) {
                            ExportFormat.PDF -> onExportPdf(selectedQuality)
                            ExportFormat.ZIP -> onExportZip(selectedQuality)
                            null -> { }
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(FotoRaporTokens.ButtonHeightL),
                    shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
                    enabled = selectedFormat != null && !isCalculatingSizes && !isExporting,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = FotoRaporTokens.ElevationNone,
                        pressedElevation = FotoRaporTokens.ElevationNone
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(FotoRaporTokens.IconSizeXS + 2.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = stringResource(R.string.acc_share),
                            modifier = Modifier.size(FotoRaporTokens.IconSizeXS + 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingXS))
                    Text(
                        if (isExporting) stringResource(R.string.export_notif_title) else stringResource(R.string.export_share_btn),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (isSelected) {
        accentColor.copy(alpha = if (isDark) 0.12f else 0.06f)
    } else {
        Color.Transparent
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(FotoRaporTokens.RadiusM))
            .border(
                if (isSelected) 1.5.dp else FotoRaporTokens.CardBorderWidth,
                borderColor,
                RoundedCornerShape(FotoRaporTokens.RadiusM)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
        color = bgColor
    ) {
        Column(
            modifier = Modifier
                .padding(FotoRaporTokens.SpacingL)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(FotoRaporTokens.IconSizeL)
            )
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingS))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXXS))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, size: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FotoRaporTokens.SpacingXXS + 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row {
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            if (size.isNotEmpty()) {
                Text(
                    "  ·  $size",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QualityOption(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainerLow
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(FotoRaporTokens.RadiusS))
            .border(
                if (isSelected) 1.5.dp else 0.dp,
                borderColor,
                RoundedCornerShape(FotoRaporTokens.RadiusS)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(FotoRaporTokens.RadiusS),
        color = bgColor
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = FotoRaporTokens.SpacingS, horizontal = FotoRaporTokens.SpacingXS)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
