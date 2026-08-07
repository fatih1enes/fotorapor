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
    Row(modifier = Modifier.fillMaxWidth().padding(end = FotoRaporTokens.SpacingS)) {
        TimelineRail(projectColor)
        TimelineContent(
            log = log,
            photos = photos,
            projectColor = projectColor,
            isSelectionMode = isSelectionMode,
            selectedPhotoIds = selectedPhotoIds,
            onPhotoClick = onPhotoClick,
            onMorePhotosClick = onMorePhotosClick,
            onNoteChange = onNoteChange,
            onAddPhotoClick = onAddPhotoClick,
            onImportPhotoClick = onImportPhotoClick,
            language = language
        )
    }
}

@Composable
private fun RowScope.TimelineContent(
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
    language: String
) {
    Column(modifier = Modifier.weight(1f).padding(start = FotoRaporTokens.SpacingM, bottom = FotoRaporTokens.Spacing4XL)) {
        Text(
            text = DateUtils.formatDate(log.date, language),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingM))

        if (photos.isNotEmpty()) {
            TimelinePhotoGrid(photos, isSelectionMode, selectedPhotoIds, onPhotoClick, onMorePhotosClick)
            Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingM))
        }

        TimelineNoteInput(log, onNoteChange)
        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingM))

        TimelineActionButtons(projectColor, onAddPhotoClick, onImportPhotoClick)
    }
}

@Composable
private fun TimelineRail(color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
        Box(modifier = Modifier.size(10.dp).border(2.dp, color, CircleShape).background(MaterialTheme.colorScheme.surface, CircleShape))
        Box(
            modifier = Modifier.width(1.5.dp).fillMaxHeight().weight(1f)
                .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.25f), Color.Transparent)))
        )
    }
}

@Composable
private fun TimelinePhotoGrid(
    photos: List<Photo>,
    isSelectionMode: Boolean,
    selectedPhotoIds: List<Long>,
    onPhotoClick: (Photo) -> Unit,
    onMorePhotosClick: () -> Unit
) {
    val displayPhotos = remember(photos) { photos.take(4) }
    val remaining = remember(photos) { photos.size - 4 }
    val rows = remember(displayPhotos) { displayPhotos.chunked(2) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingS)) {
        rows.forEach { rowPhotos ->
            TimelinePhotoRow(rowPhotos, displayPhotos, remaining, isSelectionMode, selectedPhotoIds, onPhotoClick, onMorePhotosClick)
        }
    }
}

@Composable
private fun TimelinePhotoRow(
    rowPhotos: List<Photo>,
    displayPhotos: List<Photo>,
    remaining: Int,
    isSelectionMode: Boolean,
    selectedPhotoIds: List<Long>,
    onPhotoClick: (Photo) -> Unit,
    onMorePhotosClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingS)) {
        repeat(2) { i ->
            if (i < rowPhotos.size) {
                val photo = rowPhotos[i]
                val index = displayPhotos.indexOf(photo)
                TimelinePhotoCard(
                    photo = photo,
                    isLast = index == 3 && remaining > 0,
                    remainingCount = remaining,
                    isSelected = selectedPhotoIds.contains(photo.id),
                    isSelectionMode = isSelectionMode,
                    onPhotoClick = onPhotoClick,
                    onMorePhotosClick = onMorePhotosClick,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f).aspectRatio(1.33f))
            }
        }
    }
}

@Composable
private fun TimelinePhotoCard(
    photo: Photo,
    isLast: Boolean,
    remainingCount: Int,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onPhotoClick: (Photo) -> Unit,
    onMorePhotosClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isVideo = photo.filePath.endsWith(".mp4", ignoreCase = true)

    Card(
        modifier = modifier.aspectRatio(1.33f).clickable {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            if (isLast) onMorePhotosClick() else onPhotoClick(photo)
        }.then(if (isSelectionMode && isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(FotoRaporTokens.RadiusM)) else Modifier),
        shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
        elevation = CardDefaults.cardElevation(defaultElevation = FotoRaporTokens.ElevationNone)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val context = LocalContext.current
            val request = remember(photo.filePath) {
                ImageRequest.Builder(context).data(photo.filePath).apply { if (isVideo) decoderFactory(VideoFrameDecoder.Factory()) }
                    .size(256).memoryCachePolicy(CachePolicy.ENABLED).build()
            }
            AsyncImage(
                model = request, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh),
                error = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh)
            )
            if (isVideo && !isLast) VideoOverlay()
            if (isLast) MorePhotosOverlay(remainingCount, onMorePhotosClick)
            if (isSelectionMode && !isLast) SelectionOverlay(isSelected)
        }
    }
}

@Composable
private fun VideoOverlay() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
        Icon(imageVector = Icons.Default.PlayCircleFilled, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(FotoRaporTokens.IconSizeL))
    }
}

@Composable
private fun MorePhotosOverlay(count: Int, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text("+$count", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BoxScope.SelectionOverlay(isSelected: Boolean) {
    Box(modifier = Modifier.fillMaxSize().background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent))
    Checkbox(checked = isSelected, onCheckedChange = null, modifier = Modifier.align(Alignment.TopEnd))
}

@Composable
private fun TimelineNoteInput(log: DailyLog, onNoteChange: (String) -> Unit) {
    var noteText by remember(log.id) { mutableStateOf(log.note) }
    LaunchedEffect(log.note) { if (noteText != log.note) noteText = log.note }

    Surface(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(FotoRaporTokens.RadiusM),
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(FotoRaporTokens.CardBorderWidth, MaterialTheme.colorScheme.outlineVariant)
    ) {
        TextField(
            value = noteText, onValueChange = { noteText = it; onNoteChange(it) },
            placeholder = { Text(stringResource(R.string.note_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyMedium) },
            modifier = Modifier.fillMaxWidth().heightIn(min = FotoRaporTokens.ButtonHeightL),
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, disabledContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp)
        )
    }
}

@Composable
private fun TimelineActionButtons(color: Color, onAddPhotoClick: () -> Unit, onImportPhotoClick: (Uri) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FotoRaporTokens.SpacingS)) {
        Button(
            onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onAddPhotoClick() },
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
            shape = RoundedCornerShape(FotoRaporTokens.RadiusM), modifier = Modifier.weight(1f).height(FotoRaporTokens.ButtonHeightM),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = FotoRaporTokens.ElevationNone)
        ) {
            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(FotoRaporTokens.IconSizeXS + 2.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingXS + 2.dp))
            Text(stringResource(R.string.camera_btn), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        GalleryImportButton(color, onImportPhotoClick)
    }
}

@Composable
private fun RowScope.GalleryImportButton(color: Color, onImportPhotoClick: (Uri) -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isImporting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHostState.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) {
            isImporting = true
            scope.launch(Dispatchers.IO) {
                var hasError = false
                uris.forEach { uri ->
                    val local = PhotoManager.copyUriToInternalStorage(context, uri)
                    if (local != null) withContext(Dispatchers.Main) { onImportPhotoClick(local) } else hasError = true
                }
                withContext(Dispatchers.Main) {
                    isImporting = false
                    if (hasError) scope.launch { snackbarHost.showSnackbar(context.getString(R.string.import_error)) }
                }
            }
        }
    }

    OutlinedButton(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        shape = RoundedCornerShape(FotoRaporTokens.RadiusM), modifier = Modifier.weight(1f).height(FotoRaporTokens.ButtonHeightM), enabled = !isImporting
    ) {
        if (isImporting) {
            CircularProgressIndicator(modifier = Modifier.size(FotoRaporTokens.IconSizeXS + 2.dp), color = color, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingXS + 2.dp))
            Text(stringResource(R.string.loading), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        } else {
            Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(FotoRaporTokens.IconSizeXS + 2.dp), tint = color)
            Spacer(modifier = Modifier.width(FotoRaporTokens.SpacingXS + 2.dp))
            Text(stringResource(R.string.gallery_btn), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}
