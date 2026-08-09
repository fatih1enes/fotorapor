@file:Suppress("LocalContextGetResourceValueCall", "SameParameterValue", "TooManyFunctions")
package com.fatihenes.photoreport.feature.project.ui

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.fatihenes.photoreport.core.common.util.DateUtils
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
import com.fatihenes.photoreport.core.media.PhotoManager
import com.fatihenes.photoreport.core.model.DailyLog
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.core.ui.navigation.LocalSnackbarHostState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_DISPLAY_PHOTOS = 4
private const val LAST_PHOTO_INDEX = 3
private const val GRID_COLUMNS = 2
private const val PHOTO_ASPECT_RATIO = 1.33f
private const val PRESSED_SCALE = 0.96f
private const val RAIL_WIDTH = 28
private const val RAIL_DOT_SIZE = 10
private const val RAIL_LINE_WIDTH = 1.5f
private const val THUMB_SIZE = 256
private const val OVERLAY_ALPHA = 0.15f
private const val MORE_PHOTOS_OVERLAY_ALPHA = 0.55f
private const val SELECTION_OVERLAY_ALPHA = 0.15f
private const val VIDEO_ICON_ALPHA = 0.9f
private const val DATE_TEXT_LINE_HEIGHT = 20
private const val PLACEHOLDER_ALPHA = 0.5f
private const val BORDER_ALPHA = 0.5f

data class TimelineViewState(
    val log: DailyLog,
    val photos: List<Photo>,
    val projectColor: Color,
    val isSelectionMode: Boolean,
    val selectedPhotoIds: List<Long>,
    val language: String = "tr",
)

data class TimelineActions(
    val onPhotoClick: (Photo) -> Unit,
    val onMorePhotosClick: () -> Unit,
    val onNoteChange: (String) -> Unit,
    val onAddPhotoClick: () -> Unit,
    val onImportPhotoClick: (Uri) -> Unit,
)

@Suppress("FunctionName")
@Composable
fun TimelineBlock(state: TimelineViewState, actions: TimelineActions) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = FotoRaporTokens.SpacingS),
    ) {
        TimelineRail(state.projectColor)
        TimelineContent(state = state, actions = actions)
    }
}

@Suppress("FunctionName")
@Composable
private fun RowScope.TimelineContent(state: TimelineViewState, actions: TimelineActions) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(start = FotoRaporTokens.SpacingM, bottom = FotoRaporTokens.Spacing4XL),
    ) {
        Text(
            text = DateUtils.formatDate(state.log.date, state.language),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingM))
        if (state.photos.isNotEmpty()) {
            TimelinePhotoGrid(
                photos = state.photos,
                isSelectionMode = state.isSelectionMode,
                selectedPhotoIds = state.selectedPhotoIds,
                onPhotoClick = actions.onPhotoClick,
                onMorePhotosClick = actions.onMorePhotosClick,
            )
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingM))
        }
        TimelineNoteInput(state.log, actions.onNoteChange)
        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingM))
        TimelineActionsRow(state.projectColor, actions.onAddPhotoClick, actions.onImportPhotoClick)
    }
}

@Suppress("FunctionName")
@Composable
private fun TimelineRail(color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(RAIL_WIDTH.dp),
    ) {
        Box(
            modifier = Modifier
                .size(RAIL_DOT_SIZE.dp)
                .border(2.dp, color, CircleShape)
                .background(MaterialTheme.colorScheme.surface, CircleShape),
        )
        Box(
            modifier = Modifier
                .width(RAIL_LINE_WIDTH.dp)
                .fillMaxHeight()
                .weight(1f)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            color.copy(alpha = 0.25f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

@Suppress("FunctionName")
@Composable
private fun TimelinePhotoGrid(
    photos: List<Photo>,
    isSelectionMode: Boolean,
    selectedPhotoIds: List<Long>,
    onPhotoClick: (Photo) -> Unit,
    onMorePhotosClick: () -> Unit,
) {
    val displayPhotos = remember(photos) { photos.take(MAX_DISPLAY_PHOTOS) }
    val remaining = remember(photos) { photos.size - MAX_DISPLAY_PHOTOS }
    val rows = remember(displayPhotos) { displayPhotos.chunked(GRID_COLUMNS) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingS),
    ) {
        rows.forEach { rowPhotos ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingS),
            ) {
                repeat(GRID_COLUMNS) { i ->
                    if (i < rowPhotos.size) {
                        val photo = rowPhotos[i]
                        val index = displayPhotos.indexOf(photo)
                        val isLast = (index == LAST_PHOTO_INDEX) && (remaining > 0)
                        TimelinePhotoCard(
                            params = PhotoCardParams(
                                photo = photo,
                                isLast = isLast,
                                remainingCount = remaining,
                                isSelected = selectedPhotoIds.contains(photo.id),
                                isSelectionMode = isSelectionMode,
                                onPhotoClick = onPhotoClick,
                                onMorePhotosClick = onMorePhotosClick,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                    } else Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(PHOTO_ASPECT_RATIO),
                    )
                }
            }
        }
    }
}

private data class PhotoCardParams(
    val photo: Photo,
    val isLast: Boolean,
    val remainingCount: Int,
    val isSelected: Boolean,
    val isSelectionMode: Boolean,
    val onPhotoClick: (Photo) -> Unit,
    val onMorePhotosClick: () -> Unit,
)

@Suppress("FunctionName")
@Composable
private fun TimelinePhotoCard(params: PhotoCardParams, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESSED_SCALE else 1f,
        animationSpec = FotoRaporMotion.pressSpring(),
        label = "timeline_photo_scale",
    )
    Card(
        modifier = modifier
            .aspectRatio(PHOTO_ASPECT_RATIO)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interactionSource, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (params.isLast) params.onMorePhotosClick() else params.onPhotoClick(params.photo)
            }
            .then(
                if (params.isSelectionMode && params.isSelected) {
                    Modifier.border(
                        2.5.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(FotoRaporTokens.RadiusM),
                    )
                } else {
                    Modifier
                },
            ),
        shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        TimelinePhotoCardContent(params)
    }
}

@Suppress("FunctionName")
@Composable
private fun TimelinePhotoCardContent(params: PhotoCardParams) {
    val isVideo = params.photo.filePath.endsWith(".mp4", ignoreCase = true)
    Box(modifier = Modifier.fillMaxSize()) {
        val context = LocalContext.current
        val request = remember(params.photo.filePath) {
            ImageRequest.Builder(context)
                .data(params.photo.filePath)
                .apply { if (isVideo) decoderFactory(VideoFrameDecoder.Factory()) }
                .size(THUMB_SIZE)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .crossfade(FotoRaporMotion.DURATION_SHORT)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            placeholder = androidx.compose.ui.graphics.painter.ColorPainter(
                MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            error = androidx.compose.ui.graphics.painter.ColorPainter(
                MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        )
        if (isVideo && !params.isLast) VideoOverlay()
        if (params.isLast) MorePhotosOverlay(params.remainingCount, params.onMorePhotosClick)
        if (params.isSelectionMode && !params.isLast) SelectionOverlay(params.isSelected)
    }
}

@Suppress("FunctionName")
@Composable
private fun VideoOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(OVERLAY_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.PlayCircleFilled,
            null,
            tint = Color.White.copy(VIDEO_ICON_ALPHA),
            modifier = Modifier.size(FotoRaporTokens.IconSizeL),
        )
    }
}

@Suppress("FunctionName")
@Composable
private fun MorePhotosOverlay(count: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(MORE_PHOTOS_OVERLAY_ALPHA))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+$count",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Suppress("FunctionName")
@Composable
private fun BoxScope.SelectionOverlay(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(SELECTION_OVERLAY_ALPHA)
                } else {
                    Color.Transparent
                },
            ),
    )
    Checkbox(
        checked = isSelected,
        onCheckedChange = null,
        modifier = Modifier.align(Alignment.TopEnd),
    )
}

@Suppress("FunctionName")
@Composable
private fun TimelineNoteInput(log: DailyLog, onNoteChange: (String) -> Unit) {
    var noteText by remember(log.id) { mutableStateOf(log.note) }
    LaunchedEffect(log.note) { if (noteText != log.note) noteText = log.note }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(FotoRaporTokens.CardBorderWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        TextField(
            value = noteText,
            onValueChange = {
                noteText = it
                onNoteChange(it)
            },
            placeholder = {
                Text(
                    text = stringResource(R.string.note_placeholder),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(PLACEHOLDER_ALPHA),
                    style = MaterialTheme.typography.bodyMedium,
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
                unfocusedIndicatorColor = Color.Transparent,
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = DATE_TEXT_LINE_HEIGHT.sp,
            ),
        )
    }
}

@Suppress("FunctionName")
@Composable
private fun TimelineActionsRow(
    color: Color,
    onAddPhotoClick: () -> Unit,
    onImportPhotoClick: (Uri) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var isImporting by remember { mutableStateOf(value = false) }
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHostState.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            isImporting = true
            @Suppress("kotlin:S6310")
            scope.launch(Dispatchers.IO) {
                val err = performImport(context, uris, onImportPhotoClick)
                @Suppress("kotlin:S6310")
                withContext(Dispatchers.Main) {
                    isImporting = false
                    if (err) snackbarHost.showSnackbar(context.getString(R.string.import_error))
                }
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingS),
    ) {
        CameraButton(color, haptic, onAddPhotoClick)
        ImportButton(color, haptic, launcher, isImporting)
    }
}

@Suppress("kotlin:S6310")
private suspend fun performImport(
    context: android.content.Context,
    uris: List<Uri>,
    onImportPhotoClick: (Uri) -> Unit,
): Boolean {
    var err = false
    uris.forEach { uri ->
        val local = PhotoManager.copyUriToInternalStorage(context, uri)
        if (local != null) {
            withContext(Dispatchers.Main) { onImportPhotoClick(local) }
        } else {
            err = true
        }
    }
    return err
}

@Suppress("FunctionName")
@Composable
private fun RowScope.CameraButton(
    color: Color,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onClick: () -> Unit,
) {
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
        ),
        shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
        modifier = Modifier
            .weight(1f)
            .height(FotoRaporTokens.ButtonHeightM),
        elevation = ButtonDefaults.buttonElevation(0.dp),
    ) {
        Icon(
            Icons.Default.CameraAlt,
            null,
            modifier = Modifier.size(FotoRaporTokens.IconSizeXS + 2.dp),
        )
        Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingXS + 2.dp))
        Text(
            stringResource(R.string.camera_btn),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Suppress("FunctionName")
@Composable
private fun RowScope.ImportButton(
    color: Color,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    launcher: ManagedActivityResultLauncher<PickVisualMediaRequest, List<Uri>>,
    isImporting: Boolean,
) {
    OutlinedButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
            )
        },
        border = BorderStroke(1.dp, color.copy(BORDER_ALPHA)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
        modifier = Modifier
            .weight(1f)
            .height(FotoRaporTokens.ButtonHeightM),
        enabled = !isImporting,
    ) {
        if (isImporting) {
            CircularProgressIndicator(
                modifier = Modifier.size(FotoRaporTokens.IconSizeXS + 2.dp),
                color = color,
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingXS + 2.dp))
            Text(
                stringResource(R.string.loading),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            Icon(
                Icons.Default.PhotoLibrary,
                null,
                modifier = Modifier.size(FotoRaporTokens.IconSizeXS + 2.dp),
            )
            Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingXS + 2.dp))
            Text(
                stringResource(R.string.gallery_btn),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
