package com.sarikaya.santiye.gunlugu.ui.components

import android.content.Context
import androidx.core.net.toUri
import androidx.compose.animation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.sarikaya.santiye.gunlugu.R
import com.sarikaya.santiye.gunlugu.data.PhotoEntity
import com.sarikaya.santiye.gunlugu.data.ProjectEntity
import com.sarikaya.santiye.gunlugu.data.LogWithPhotos

enum class ExportFormat { PDF, ZIP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(
    project: ProjectEntity,
    logs: List<LogWithPhotos>,
    onDismiss: () -> Unit,
    onExportPdf: (Int) -> Unit,
    onExportZip: (Int) -> Unit
) {
    val allPhotos = remember(logs) { logs.flatMap { it.photos } }
    val context = LocalContext.current

    var selectedFormat by remember { mutableStateOf<ExportFormat?>(null) }
    var selectedQuality by remember { mutableIntStateOf(100) } // 100, 85, 75
    var isExporting by remember { mutableStateOf(false) }

    // Dosya boyutlarını hesapla
    var fileSizes by remember { mutableStateOf(FileSizeInfo(0L, 0L, 0, 0, 0L, 0L, 0L)) }
    var isCalculatingSizes by remember { mutableStateOf(true) }

    LaunchedEffect(allPhotos) {
        isCalculatingSizes = true
        fileSizes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            calculateFileSizes(context, allPhotos)
        }
        isCalculatingSizes = false
    }

    val totalPhotoSizeMB = fileSizes.totalPhotoBytes / (1024.0 * 1024.0)
    val totalVideoSizeMB = fileSizes.totalVideoBytes / (1024.0 * 1024.0)
    val videoCount = fileSizes.videoCount
    val photoCount = fileSizes.photoCount

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {

            // Başlık
            Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp).align(Alignment.CenterHorizontally),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.export_share_project),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = project.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Format Seçimi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FormatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.PictureAsPdf,
                        title = "PDF",
                        description = stringResource(R.string.export_pdf_desc),
                        isSelected = selectedFormat == ExportFormat.PDF,
                        accentColor = Color(0xFFE53935),
                        onClick = { selectedFormat = ExportFormat.PDF }
                    )
                    FormatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FolderZip,
                        title = "ZIP",
                        description = stringResource(R.string.export_zip_desc),
                        isSelected = selectedFormat == ExportFormat.ZIP,
                        accentColor = Color(0xFF1E88E5),
                        onClick = { selectedFormat = ExportFormat.ZIP }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dosya bilgileri
                AnimatedVisibility(
                    visible = selectedFormat != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        // Sıkıştırma Seçenekleri
                        Text(
                            text = stringResource(R.string.export_compression_quality),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                        // İçerik özeti
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    stringResource(R.string.export_content_summary),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                if (isCalculatingSizes) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
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

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                    if (selectedFormat == ExportFormat.PDF) {
                                        // PDF boyut tahmini (Daha tutarlı ve gerçeğe yakın)
                                        val estimatedPdfBytes = when (selectedQuality) {
                                            100 -> fileSizes.estimatedQ100Bytes + (photoCount * 50 * 1024L) // 50KB PDF overhead per photo
                                            85 -> fileSizes.estimatedQ85Bytes + (photoCount * 30 * 1024L)
                                            else -> fileSizes.estimatedQ75Bytes + (photoCount * 20 * 1024L)
                                        }
                                        val estimatedPdfMB = (estimatedPdfBytes / (1024.0 * 1024.0)).coerceAtLeast(0.1)
                                        Text(
                                            stringResource(R.string.export_pdf_estimate, "%.1f MB".format(estimatedPdfMB)),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (videoCount > 0) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFFA726),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    stringResource(R.string.export_pdf_video_warning),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFFFFA726)
                                                )
                                            }
                                        }
                                    } else {
                                        // ZIP boyut tahmini
                                        val estimatedPhotoBytes = when (selectedQuality) {
                                            100 -> fileSizes.estimatedQ100Bytes
                                            85 -> fileSizes.estimatedQ85Bytes
                                            else -> fileSizes.estimatedQ75Bytes
                                        }
                                        val estimatedPhotoMB = estimatedPhotoBytes / (1024.0 * 1024.0)
                                        val totalMB = (estimatedPhotoMB + totalVideoSizeMB).coerceAtLeast(0.1)
                                        Text(
                                            stringResource(R.string.export_zip_estimate, "%.1f MB".format(totalMB)),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            if (selectedQuality == 100) stringResource(R.string.export_zip_original_desc) else stringResource(R.string.export_zip_compressed_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Butonlar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.export_cancel_btn))
                    }

                    Button(
                        onClick = {
                            isExporting = true
                            android.widget.Toast.makeText(context, context.getString(R.string.export_started_toast), android.widget.Toast.LENGTH_LONG).show()
                            when (selectedFormat) {
                                ExportFormat.PDF -> onExportPdf(selectedQuality)
                                ExportFormat.ZIP -> onExportZip(selectedQuality)
                                null -> { /* do nothing */ }
                            }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedFormat != null && !isCalculatingSizes && !isExporting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Share, 
                                contentDescription = null, 
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isExporting) stringResource(R.string.export_notif_title) else stringResource(R.string.export_share_btn), color = Color.White, fontWeight = FontWeight.Bold)
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
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val borderColor = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (isSelected) {
        accentColor.copy(alpha = if (isDark) 0.15f else 0.08f)
    } else {
        Color.Transparent
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
            )
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Row {
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            if (size.isNotEmpty()) {
                Text(
                    "  •  $size",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class FileSizeInfo(
    val totalPhotoBytes: Long,
    val totalVideoBytes: Long,
    val photoCount: Int,
    val videoCount: Int,
    val estimatedQ100Bytes: Long,
    val estimatedQ85Bytes: Long,
    val estimatedQ75Bytes: Long
)

private fun calculateFileSizes(context: Context, photos: List<PhotoEntity>): FileSizeInfo {
    var totalPhotoBytes = 0L
    var totalVideoBytes = 0L
    var photoCount = 0
    var videoCount = 0

    var estimatedQ100 = 0L
    var estimatedQ85 = 0L
    var estimatedQ75 = 0L

    val maxBytesQ100 = (1.5 * 1024 * 1024).toLong() // 1.5 MB max for Q100 downsampled
    val maxBytesQ85 = (0.4 * 1024 * 1024).toLong()  // 400 KB max for Q85 downsampled
    val maxBytesQ75 = (0.15 * 1024 * 1024).toLong() // 150 KB max for Q75 downsampled

    photos.forEach { photo ->
        try {
            val uri = photo.filePath.toUri()
            val size = if (photo.filePath.startsWith("content://")) {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { 
                    it.statSize 
                } ?: 0L
            } else {
                val path = if (photo.filePath.startsWith("file://")) uri.path else photo.filePath
                path?.let { java.io.File(it).length() } ?: 0L
            }

            if (photo.filePath.endsWith(".mp4", ignoreCase = true)) {
                totalVideoBytes += size
                videoCount++
            } else {
                totalPhotoBytes += size
                photoCount++
                
                // Estimate size based on original size capped by maximum size for that quality
                estimatedQ100 += minOf(size, maxBytesQ100)
                estimatedQ85 += minOf(size, maxBytesQ85)
                estimatedQ75 += minOf(size, maxBytesQ75)
            }
        } catch (_: Exception) {
            // Dosya boyutu hesaplanamadı, skip
        }
    }

    return FileSizeInfo(
        totalPhotoBytes, 
        totalVideoBytes, 
        photoCount, 
        videoCount,
        estimatedQ100,
        estimatedQ85,
        estimatedQ75
    )
}

@Composable
private fun QualityOption(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
