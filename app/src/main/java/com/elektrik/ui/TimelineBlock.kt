package com.sarikaya.santiye.gunlugu.ui

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
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.sarikaya.santiye.gunlugu.R
import com.sarikaya.santiye.gunlugu.data.DailyLogEntity
import com.sarikaya.santiye.gunlugu.data.PhotoEntity
import com.sarikaya.santiye.gunlugu.util.DateUtils

@Suppress("SameParameterValue")
@Composable
fun TimelineBlock(
    log: DailyLogEntity,
    photos: List<PhotoEntity>,
    projectColor: Color,
    isSelectionMode: Boolean,
    selectedPhotoIds: List<Long>,
    onPhotoClick: (PhotoEntity) -> Unit,
    onMorePhotosClick: () -> Unit,
    onNoteChange: (String) -> Unit,
    onAddPhotoClick: () -> Unit,
    onImportPhotoClick: (Uri) -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Row(modifier = Modifier.fillMaxWidth().padding(end = 8.dp)) {
        // Timeline dot & line
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
            Box(modifier = Modifier.size(12.dp).border(2.dp, projectColor, CircleShape).background(MaterialTheme.colorScheme.surface, CircleShape))
            Box(modifier = Modifier.width(2.dp).fillMaxHeight().weight(1f).background(Brush.verticalGradient(listOf(projectColor.copy(alpha = 0.3f), Color.Transparent))))
        }

        Column(modifier = Modifier.weight(1f).padding(start = 12.dp, bottom = 40.dp)) {
            Text(
                text = DateUtils.formatDate(log.date),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (photos.isNotEmpty()) {
                val displayPhotos = remember(photos) { photos.take(4) }
                val remaining = remember(photos) { photos.size - 4 }
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val rows = remember(displayPhotos) { displayPhotos.chunked(2) }
                    for (rowPhotos in rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (i in 0 until 2) {
                                if (i < rowPhotos.size) {
                                    val photo = rowPhotos[i]
                                    val index = displayPhotos.indexOf(photo)
                                    val isSelected = selectedPhotoIds.contains(photo.id)
                                    val isLast = index == 3 && remaining > 0
                                    val isVideo = photo.filePath.endsWith(".mp4", ignoreCase = true)
                                    
                                    Card(
                                        modifier = Modifier.weight(1f).aspectRatio(1.33f).clickable { 
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                            if (isLast) onMorePhotosClick() else onPhotoClick(photo) 
                                        }
                                            .then(if (isSelectionMode && isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)) else Modifier),
                                        shape = RoundedCornerShape(16.dp)
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
                                                contentDescription = null,
                                                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color.LightGray),
                                                error = androidx.compose.ui.graphics.painter.ColorPainter(Color.DarkGray),
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            if (isVideo && !isLast) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color.Black.copy(alpha = 0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayCircleFilled,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(36.dp)
                                                    )
                                                }
                                            }
                                            if (isLast) {
                                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                                                    Text("+$remaining", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            if (isSelectionMode && !isLast) {
                                                Box(modifier = Modifier.fillMaxSize().background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent))
                                                Checkbox(checked = isSelected, onCheckedChange = null, modifier = Modifier.align(Alignment.TopEnd))
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1.33f))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            var noteText by remember(log.id) { mutableStateOf(log.note) }
            LaunchedEffect(log.note) {
                if (noteText != log.note) {
                    noteText = log.note
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                TextField(
                    value = noteText,
                    onValueChange = { noteText = it; onNoteChange(it) },
                    placeholder = { Text(stringResource(R.string.note_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onAddPhotoClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = projectColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(46.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.camera_btn), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                val context = LocalContext.current
                var isImporting by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()

                val galleryLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickMultipleVisualMedia()
                ) { uris ->
                    if (uris.isNotEmpty()) {
                        isImporting = true
                        coroutineScope.launch(Dispatchers.IO) {
                            var hasError = false
                            uris.forEach { uri ->
                                val localUri = com.sarikaya.santiye.gunlugu.util.PhotoManager.copyUriToInternalStorage(context, uri)
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
                                    android.widget.Toast.makeText(context, context.getString(R.string.import_error), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { 
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    border = BorderStroke(1.5.dp, projectColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = projectColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(46.dp),
                    enabled = !isImporting
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = projectColor, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.loading), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = projectColor)
                    } else {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp), tint = projectColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.gallery_btn), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = projectColor)
                    }
                }
            }
        }
    }
}
