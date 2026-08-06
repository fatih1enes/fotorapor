@file:Suppress("LocalContextGetResourceValueCall", "SameParameterValue")
package com.fatihenes.photoreport.feature.project.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion
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
            .height(IntrinsicSize.Min)
            .padding(end = FotoRaporTokens.SpacingS)
    ) {
        // ── Timeline Rail ────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .border(2.5.dp, projectColor, CircleShape)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                projectColor.copy(alpha = 0.4f),
                                projectColor.copy(alpha = 0.1f)
                            )
                        )
                    )
            )
        }

        // ── Content ──────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = FotoRaporTokens.SpacingL, bottom = FotoRaporTokens.Spacing4XL)
        ) {
            // Date heading
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = DateUtils.formatDate(log.date, language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.weight(1f))
                if (photos.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.height(20.dp)
                    ) {
                        Text(
                            text = "${photos.size} Foto",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingL))

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

                                    val itemInteractionSource = remember { MutableInteractionSource() }
                                    val isItemPressed by itemInteractionSource.collectIsPressedAsState()
                                    val itemScale by animateFloatAsState(
                                        targetValue = if (isItemPressed) 0.95f else 1.0f,
                                        animationSpec = FotoRaporMotion.pressSpring(),
                                        label = "img_scale"
                                    )

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1.33f)
                                            .graphicsLayer {
                                                scaleX = itemScale
                                                scaleY = itemScale
                                            }
                                            .clip(RoundedCornerShape(FotoRaporTokens.RadiusM))
                                            .clickable(
                                                interactionSource = itemInteractionSource,
                                                indication = ripple(),
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    if (isLast) onMorePhotosClick() else onPhotoClick(photo)
                                                }
                                            )
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
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                                                .size(400) // Quality vs performance balance
                                                .memoryCachePolicy(CachePolicy.ENABLED)
                                                .build()
                                        }

                                        Box(modifier = Modifier.fillMaxSize()) {
                                            AsyncImage(
                                                model = request,
                                                contentDescription = stringResource(R.string.photo_label),
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                onSuccess = {
                                                    // Smooth fade-in could be added here if not handled by Coil
                                                }
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
                                                        contentDescription = null,
                                                        tint = Color.White.copy(alpha = 0.9f),
                                                        modifier = Modifier.size(FotoRaporTokens.IconSizeL)
                                                    )
                                                }
                                            }
                                            if (isLast) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.Black.copy(alpha = 0.6f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "+$remaining",
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.headlineSmall,
                                                        fontWeight = FontWeight.Bold
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
                Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingL))
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
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(
                    FotoRaporTokens.CardBorderWidth,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                var isFocused by remember { mutableStateOf(false) }
                val animatedBorderColor by animateColorAsState(
                    targetValue = if (isFocused) projectColor.copy(alpha = 0.5f) else Color.Transparent,
                    animationSpec = tween(300),
                    label = "border_focus"
                )

                TextField(
                    value = noteText,
                    onValueChange = { noteText = it; onNoteChange(it) },
                    placeholder = {
                        Text(
                            stringResource(R.string.note_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = FotoRaporTokens.ButtonHeightM)
                        .onFocusChanged { isFocused = it.isFocused }
                        .border(1.dp, animatedBorderColor, RoundedCornerShape(FotoRaporTokens.RadiusM)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = projectColor
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingL))

            // ── Action Buttons ───────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingS)
            ) {
                // Camera button
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAddPhotoClick()
                    },
                    color = projectColor,
                    shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
                    modifier = Modifier
                        .weight(1f)
                        .height(FotoRaporTokens.ButtonHeightM)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(FotoRaporTokens.IconSizeXS + 2.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.camera_btn),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
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
                    border = BorderStroke(1.dp, projectColor.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = projectColor),
                    shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
                    modifier = Modifier
                        .weight(1f)
                        .height(FotoRaporTokens.ButtonHeightM),
                    enabled = !isImporting
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = projectColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(FotoRaporTokens.IconSizeXS + 2.dp),
                            tint = projectColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.gallery_btn),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = projectColor
                        )
                    }
                }
            }
        }
    }
}
