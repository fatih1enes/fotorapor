@file:Suppress("LocalContextGetResourceValueCall", "SameParameterValue")
package com.fatihenes.photoreport.feature.project.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.video.VideoFrameDecoder
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.core.model.DailyLog
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.common.util.DateUtils
import com.fatihenes.photoreport.core.media.PhotoManager
import com.fatihenes.photoreport.core.ui.navigation.LocalSnackbarHostState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TimelineBlock(
    log: DailyLog,
    photos: List<Photo>,
    projectColor: Color,
    isSelectionMode: Boolean,
    selectedPhotoIds: List<Long>,
    onPhotoClick: (Photo) -> Unit,
    onMorePhotosClick: () -> Unit,
    onNoteChange: (String) -> Unit,
    onAddPhotoClick: () -> Unit,
    onImportPhotoClick: (Uri) -> Unit,
    language: String = "tr"
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = FotoRaporTokens.SpacingS)
    ) {
        // ── Timeline Rail ────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .border(2.dp, projectColor, CircleShape)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
            )
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .fillMaxHeight()
                    .weight(1f)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                projectColor.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // ── Content ──────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = FotoRaporTokens.SpacingM, bottom = FotoRaporTokens.Spacing4XL)
        ) {
            // Date heading
            Text(
                text = DateUtils.formatDate(log.date, language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingM))

            // ── Photo Grid ───────────────────────────
            if (photos.isNotEmpty()) {
                val displayPhotos = remember(photos) { photos.take(4) }
                val remaining = remember(photos) { photos.size - 4 }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingS)
                ) {
                    val rows = remember(displayPhotos) { displayPhotos.chunked(2) }
                    for (rowPhotos in rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingS)
                        ) {
                            for (i in 0 until 2) {
                                if (i < rowPhotos.size) {
                                    val photo = rowPhotos[i]
                                    val index = displayPhotos.indexOf(photo)
                                    val isSelected = selectedPhotoIds.contains(photo.id)
                                    val isLast = index == 3 && remaining > 0
                                    val isVideo = photo.filePath.endsWith(".mp4", ignoreCase = true)

                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1.33f)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                if (isLast) onMorePhotosClick() else onPhotoClick(photo)
                                            }
                                            .then(
                                                if (isSelectionMode && isSelected) {
                                                    Modifier.border(
                                                        2.5.dp,
                                                        MaterialTheme.colorScheme.primary,
                                                        RoundedCornerShape(FotoRaporTokens.RadiusM)
                                                    )
                                                } else Modifier
                                            ),
                                        shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
                                        elevation = CardDefaults.cardElevation(
                                            defaultElevation = FotoRaporTokens.ElevationNone
                                        )
                                    ) {
                                        val context = LocalContext.current
                                        val request = remember(photo.filePath) {
                                            ImageRequest.Builder(context)
                                                .data(photo.filePath)
                                                .apply {
                                                    if (isVideo) {
                                                        decoderFactory(VideoFrameDecoder.Factory())
                                                    }
                                                }
                                                .size(256)
                                                .memoryCachePolicy(CachePolicy.ENABLED)
                                                .build()
                                        }

                                        Box(modifier = Modifier.fillMaxSize()) {
                                            AsyncImage(
                                                model = request,
                                                contentDescription = stringResource(R.string.photo_label),
                                                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(
                                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                                ),
                                                error = androidx.compose.ui.graphics.painter.ColorPainter(
                                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                                ),
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            if (isVideo && !isLast) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.Black.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayCircleFilled,
                                                        contentDescription = stringResource(R.string.loading),
                                                        tint = Color.White.copy(alpha = 0.9f),
                                                        modifier = Modifier.size(FotoRaporTokens.IconSizeL)
                                                    )
                                                }
                                            }
                                            if (isLast) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.Black.copy(alpha = 0.55f))
                                                        .clickable { onMorePhotosClick() },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "+$remaining",
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.headlineSmall,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                            if (isSelectionMode && !isLast) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                            else Color.Transparent
                                                        )
                                                )
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = null,
                                                    modifier = Modifier.align(Alignment.TopEnd)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1.33f)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingM))
            }

            // ── Note Input ──────────────────────────
            var noteText by remember(log.id) { mutableStateOf(log.note) }
            LaunchedEffect(log.note) {
                if (noteText != log.note) {
                    noteText = log.note
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    FotoRaporTokens.CardBorderWidth,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                TextField(
                    value = noteText,
                    onValueChange = { noteText = it; onNoteChange(it) },
                    placeholder = {
                        Text(
                            stringResource(R.string.note_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = FotoRaporTokens.ButtonHeightL),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingM))

            // ── Action Buttons ───────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingS)
            ) {
                // Camera button
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAddPhotoClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = projectColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
                    modifier = Modifier
                        .weight(1f)
                        .height(FotoRaporTokens.ButtonHeightM),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = FotoRaporTokens.ElevationNone
                    )
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = stringResource(R.string.acc_shutter),
                        modifier = Modifier.size(FotoRaporTokens.IconSizeXS + 2.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingXS + 2.dp))
                    Text(
                        stringResource(R.string.camera_btn),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // Gallery import button
                val context = LocalContext.current
                var isImporting by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()
                val snackbarHost = LocalSnackbarHostState.current

                val galleryLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickMultipleVisualMedia()
                ) { uris ->
                    if (uris.isNotEmpty()) {
                        isImporting = true
                        coroutineScope.launch(Dispatchers.IO) {
                            var hasError = false
                            uris.forEach { uri ->
                                val localUri = PhotoManager.copyUriToInternalStorage(context, uri)
                                if (localUri != null) {
                                    withContext(Dispatchers.Main) {
                                        onImportPhotoClick(localUri)
                                    }
                                } else {
                                    hasError = true
                                }
                            }
                            withContext(Dispatchers.Main) {
                                isImporting = false
                                if (hasError) {
                                    coroutineScope.launch { snackbarHost.showSnackbar(context.getString(R.string.import_error)) }
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    border = BorderStroke(1.dp, projectColor.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = projectColor),
                    shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
                    modifier = Modifier
                        .weight(1f)
                        .height(FotoRaporTokens.ButtonHeightM),
                    enabled = !isImporting
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(FotoRaporTokens.IconSizeXS + 2.dp),
                            color = projectColor,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingXS + 2.dp))
                        Text(
                            stringResource(R.string.loading),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = projectColor
                        )
                    } else {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = stringResource(R.string.acc_gallery),
                            modifier = Modifier.size(FotoRaporTokens.IconSizeXS + 2.dp),
                            tint = projectColor
                        )
                        Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingXS + 2.dp))
                        Text(
                            stringResource(R.string.gallery_btn),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = projectColor
                        )
                    }
                }
            }
        }
    }
}
