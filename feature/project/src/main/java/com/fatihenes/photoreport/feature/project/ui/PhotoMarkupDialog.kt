@file:Suppress("LongMethod", "TooManyFunctions", "MagicNumber", "CyclomaticComplexMethod", "MaxLineLength", "LargeClass")
package com.fatihenes.photoreport.feature.project.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Filter1
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Rectangle
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush as ComposeBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.fatihenes.photoreport.core.media.ImageProcessor
import com.fatihenes.photoreport.core.model.MarkupItem
import com.fatihenes.photoreport.core.model.MarkupTextStyle
import com.fatihenes.photoreport.core.model.MarkupTool
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.core.ui.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private val StudioColors = listOf(
    Color(0xFFFF1744), // Red
    Color(0xFFFFD600), // Yellow
    Color(0xFF00E676), // Green
    Color(0xFF00E5FF), // Cyan
    Color(0xFFFFFFFF), // White
    Color(0xFFFF6D00), // Orange
    Color(0xFFE040FB), // Magenta
    Color(0xFF212121), // Charcoal
)

@Deprecated(
    message = "Use PhotoMarkupScreen from the markup package instead. This monolithic composable will be removed.",
    replaceWith = ReplaceWith("com.fatihenes.photoreport.feature.project.ui.markup.PhotoMarkupScreen")
)
@Composable
fun PhotoMarkupDialog(
    photo: Photo,
    onDismiss: () -> Unit,
    onSaveMarkups: (List<MarkupItem>) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val items = remember { mutableStateListOf<MarkupItem>() }
    val redoStack = remember { mutableStateListOf<MarkupItem>() }

    var selectedTool by remember { mutableStateOf(MarkupTool.FREEHAND) }
    var selectedColor by remember { mutableStateOf(StudioColors[0]) }
    var strokeThickness by remember { mutableFloatStateOf(0.008f) }

    var currentDragStart by remember { mutableStateOf<Offset?>(null) }
    var currentDragEnd by remember { mutableStateOf<Offset?>(null) }
    val currentFreehandPoints = remember { mutableStateListOf<Pair<Float, Float>>() }

    var showTextPrompt by remember { mutableStateOf(false) }
    var textPromptPosition by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    var textInput by remember { mutableStateOf("") }
    var textStyle by remember { mutableStateOf(MarkupTextStyle.BADGE) }

    var showDiscardConfirm by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Read image aspect ratio accurately taking EXIF into account
    var imageAspectRatio by remember(photo.filePath) { mutableFloatStateOf(1f) }
    LaunchedEffect(photo.filePath) {
        withContext(Dispatchers.IO) {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ImageProcessor.openInputStreamSafe(context, photo.filePath)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            val exifRotation = ImageProcessor.getExifRotation(context, photo.filePath)
            val isSwapped = exifRotation == 90f || exifRotation == 270f
            val w = if (isSwapped) options.outHeight else options.outWidth
            val h = if (isSwapped) options.outWidth else options.outHeight
            if (w > 0 && h > 0) {
                imageAspectRatio = w.toFloat() / h.toFloat()
            }
        }
    }

    val handleClose = {
        if (items.isNotEmpty()) {
            showDiscardConfirm = true
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { handleClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        BackHandler { handleClose() }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. Full Screen Immersive Photo Canvas (Edge-to-Edge)
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 56.dp, bottom = 96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val containerWidth = maxWidth
                    val containerHeight = maxHeight
                    val containerAspect = containerWidth.value / containerHeight.value

                    val (fittedWidth, fittedHeight) = if (imageAspectRatio > containerAspect) {
                        containerWidth to (containerWidth / imageAspectRatio)
                    } else {
                        (containerHeight * imageAspectRatio) to containerHeight
                    }

                    Box(
                        modifier = Modifier
                            .size(fittedWidth, fittedHeight)
                            .clipToBounds()
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(photo.filePath)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Touch Interaction Canvas Overlay
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(selectedTool, selectedColor, strokeThickness, textStyle) {
                                    if (selectedTool == MarkupTool.TEXT) {
                                        detectTapGestures { offset ->
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val normX = (offset.x / size.width).coerceIn(0.05f, 0.95f)
                                            val normY = (offset.y / size.height).coerceIn(0.05f, 0.95f)
                                            textPromptPosition = Offset(normX, normY)
                                            textInput = ""
                                            showTextPrompt = true
                                        }
                                    } else if (selectedTool == MarkupTool.NUMBERED_PIN) {
                                        detectTapGestures { offset ->
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val normX = (offset.x / size.width).coerceIn(0.02f, 0.98f)
                                            val normY = (offset.y / size.height).coerceIn(0.02f, 0.98f)
                                            val nextNum = (items.filterIsInstance<MarkupItem.NumberedPin>().maxOfOrNull { it.number } ?: 0) + 1
                                            items.add(
                                                MarkupItem.NumberedPin(
                                                    x = normX,
                                                    y = normY,
                                                    number = nextNum,
                                                    colorArgb = selectedColor.toArgb()
                                                )
                                            )
                                            redoStack.clear()
                                        }
                                    } else {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                val normX = (offset.x / size.width).coerceIn(0f, 1f)
                                                val normY = (offset.y / size.height).coerceIn(0f, 1f)
                                                currentDragStart = Offset(normX, normY)
                                                currentDragEnd = Offset(normX, normY)
                                                if (selectedTool == MarkupTool.FREEHAND || selectedTool == MarkupTool.HIGHLIGHTER || selectedTool == MarkupTool.NEON_PEN) {
                                                    currentFreehandPoints.clear()
                                                    currentFreehandPoints.add(normX to normY)
                                                }
                                            },
                                            onDrag = { change, _ ->
                                                val normX = (change.position.x / size.width).coerceIn(0f, 1f)
                                                val normY = (change.position.y / size.height).coerceIn(0f, 1f)
                                                currentDragEnd = Offset(normX, normY)
                                                if (selectedTool == MarkupTool.FREEHAND || selectedTool == MarkupTool.HIGHLIGHTER || selectedTool == MarkupTool.NEON_PEN) {
                                                    currentFreehandPoints.add(normX to normY)
                                                }
                                            },
                                            onDragEnd = {
                                                val start = currentDragStart
                                                val end = currentDragEnd
                                                if (start != null && end != null) {
                                                    val item: MarkupItem? = when (selectedTool) {
                                                        MarkupTool.ARROW -> MarkupItem.Arrow(
                                                            start.x, start.y, end.x, end.y,
                                                            selectedColor.toArgb(), strokeThickness
                                                        )
                                                        MarkupTool.RECTANGLE -> MarkupItem.Rectangle(
                                                            start.x, start.y, end.x, end.y,
                                                            selectedColor.toArgb(), strokeThickness
                                                        )
                                                        MarkupTool.CIRCLE -> {
                                                            val dx = end.x - start.x
                                                            val dy = end.y - start.y
                                                            val radius = kotlin.math.sqrt(dx * dx + dy * dy)
                                                            MarkupItem.Circle(
                                                                start.x, start.y, radius,
                                                                selectedColor.toArgb(), strokeThickness
                                                            )
                                                        }
                                                        MarkupTool.LINE -> MarkupItem.Line(
                                                            start.x, start.y, end.x, end.y,
                                                            selectedColor.toArgb(), strokeThickness
                                                        )
                                                        MarkupTool.FREEHAND -> {
                                                            if (currentFreehandPoints.size >= 2) {
                                                                MarkupItem.Freehand(
                                                                    currentFreehandPoints.toList(),
                                                                    selectedColor.toArgb(), strokeThickness
                                                                )
                                                            } else null
                                                        }
                                                        MarkupTool.HIGHLIGHTER -> {
                                                            if (currentFreehandPoints.size >= 2) {
                                                                MarkupItem.Highlighter(
                                                                    currentFreehandPoints.toList(),
                                                                    selectedColor.toArgb(), strokeThickness * 2.5f
                                                                )
                                                            } else null
                                                        }
                                                        MarkupTool.NEON_PEN -> {
                                                            if (currentFreehandPoints.size >= 2) {
                                                                MarkupItem.Freehand(
                                                                    currentFreehandPoints.toList(),
                                                                    selectedColor.toArgb(), strokeThickness,
                                                                    isNeon = true
                                                                )
                                                            } else null
                                                        }
                                                        MarkupTool.BLUR -> MarkupItem.BlurRect(
                                                            start.x, start.y, end.x, end.y
                                                        )
                                                        MarkupTool.MOSAIC -> MarkupItem.BlurRect(
                                                            start.x, start.y, end.x, end.y,
                                                            isMosaic = true
                                                        )
                                                        else -> null
                                                    }
                                                    item?.let {
                                                        items.add(it)
                                                        redoStack.clear()
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                }
                                                currentDragStart = null
                                                currentDragEnd = null
                                                currentFreehandPoints.clear()
                                            }
                                        )
                                    }
                                }
                        ) {
                            val w = size.width
                            val h = size.height
                            val minDim = minOf(w, h)

                            // Render confirmed markups
                            items.forEach { item ->
                                renderMarkupPreview(this, item, w, h, minDim)
                            }

                            // Render live drag preview
                            val start = currentDragStart
                            val end = currentDragEnd
                            if (start != null && end != null) {
                                val preview: MarkupItem? = when (selectedTool) {
                                    MarkupTool.ARROW -> MarkupItem.Arrow(start.x, start.y, end.x, end.y, selectedColor.toArgb(), strokeThickness)
                                    MarkupTool.RECTANGLE -> MarkupItem.Rectangle(start.x, start.y, end.x, end.y, selectedColor.toArgb(), strokeThickness)
                                    MarkupTool.CIRCLE -> {
                                        val dx = end.x - start.x
                                        val dy = end.y - start.y
                                        MarkupItem.Circle(start.x, start.y, kotlin.math.sqrt(dx * dx + dy * dy), selectedColor.toArgb(), strokeThickness)
                                    }
                                    MarkupTool.LINE -> MarkupItem.Line(start.x, start.y, end.x, end.y, selectedColor.toArgb(), strokeThickness)
                                    MarkupTool.FREEHAND -> {
                                        if (currentFreehandPoints.size >= 2) {
                                            MarkupItem.Freehand(currentFreehandPoints.toList(), selectedColor.toArgb(), strokeThickness)
                                        } else null
                                    }
                                    MarkupTool.HIGHLIGHTER -> {
                                        if (currentFreehandPoints.size >= 2) {
                                            MarkupItem.Highlighter(currentFreehandPoints.toList(), selectedColor.toArgb(), strokeThickness * 2.5f)
                                        } else null
                                    }
                                    MarkupTool.NEON_PEN -> {
                                        if (currentFreehandPoints.size >= 2) {
                                            MarkupItem.Freehand(currentFreehandPoints.toList(), selectedColor.toArgb(), strokeThickness, isNeon = true)
                                        } else null
                                    }
                                    MarkupTool.BLUR -> MarkupItem.BlurRect(start.x, start.y, end.x, end.y)
                                    MarkupTool.MOSAIC -> MarkupItem.BlurRect(start.x, start.y, end.x, end.y, isMosaic = true)
                                    else -> null
                                }
                                preview?.let { renderMarkupPreview(this, it, w, h, minDim) }
                            }
                        }
                    }
                }

                // 2. WhatsApp-Style Minimalist Floating Header
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(
                            ComposeBrush.verticalGradient(
                                colors = listOf(Color(0xCC000000), Color(0x66000000), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Close / Cancel Icon
                        Surface(
                            shape = CircleShape,
                            color = Color(0x66000000),
                            modifier = Modifier.size(38.dp)
                        ) {
                            IconButton(onClick = handleClose, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_label), tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }

                        // Center Actions: Undo, Redo, Clear (Compact Icons)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (items.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0x66000000),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val popped = items.removeAt(items.lastIndex)
                                            redoStack.add(popped)
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.markup_undo), tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            if (redoStack.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0x66000000),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val item = redoStack.removeAt(redoStack.lastIndex)
                                            items.add(item)
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = stringResource(R.string.markup_redo), tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            if (items.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0x66000000),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            items.clear()
                                            redoStack.clear()
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.markup_clear), tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }

                        // Right: Sleek "Kaydet" Pill (Guaranteed Single-Line)
                        Button(
                            onClick = {
                                if (!isSaving) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isSaving = true
                                    onSaveMarkups(items.toList())
                                }
                            },
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E676),
                                contentColor = Color(0xFF0A1810)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF0A1810),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(
                                        stringResource(R.string.markup_save),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. WhatsApp-Style Minimalist Floating Bottom Dock
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            ComposeBrush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x99000000), Color(0xFA000000))
                            )
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Row 1: Color Dots & Thickness Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Colors
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StudioColors.forEach { color ->
                                    val isSelected = selectedColor == color
                                    val scale by animateFloatAsState(if (isSelected) 1.25f else 1.0f, label = "color_scale")
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .scale(scale)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) Color.White else Color(0x55FFFFFF),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedColor = color
                                            }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Thickness Selector (3 Simple Clean Dots)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color(0x881E232F), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                listOf(
                                    0.004f to 5.dp,
                                    0.008f to 9.dp,
                                    0.016f to 13.dp
                                ).forEach { (thick, dotSize) ->
                                    val isSelected = strokeThickness == thick
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color(0xFF00E676) else Color.Transparent)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                strokeThickness = thick
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(dotSize)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color(0xFF0A1810) else Color.White)
                                        )
                                    }
                                }
                            }
                        }

                        // Row 2: Clean Icon-Only Floating Tool Capsule
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xCC161B26),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x33FFFFFF))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StudioIconTool(Icons.Default.Brush, selectedTool == MarkupTool.FREEHAND) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTool = MarkupTool.FREEHAND
                                }
                                StudioIconTool(Icons.Default.AutoAwesome, selectedTool == MarkupTool.NEON_PEN) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTool = MarkupTool.NEON_PEN
                                }
                                StudioIconTool(Icons.Default.Highlight, selectedTool == MarkupTool.HIGHLIGHTER) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTool = MarkupTool.HIGHLIGHTER
                                }
                                StudioIconTool(Icons.AutoMirrored.Filled.ArrowForward, selectedTool == MarkupTool.ARROW) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTool = MarkupTool.ARROW
                                }
                                StudioIconTool(Icons.Default.Rectangle, selectedTool == MarkupTool.RECTANGLE) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTool = MarkupTool.RECTANGLE
                                }
                                StudioIconTool(Icons.Default.RadioButtonUnchecked, selectedTool == MarkupTool.CIRCLE) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTool = MarkupTool.CIRCLE
                                }
                                StudioIconTool(Icons.Default.HorizontalRule, selectedTool == MarkupTool.LINE) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTool = MarkupTool.LINE
                                }
                                StudioIconTool(Icons.Default.TextFields, selectedTool == MarkupTool.TEXT) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTool = MarkupTool.TEXT
                                }
                                StudioIconTool(Icons.Default.Filter1, selectedTool == MarkupTool.NUMBERED_PIN) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTool = MarkupTool.NUMBERED_PIN
                                }
                                StudioIconTool(Icons.Default.BlurOn, selectedTool == MarkupTool.BLUR) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTool = MarkupTool.BLUR
                                }
                                StudioIconTool(Icons.Default.GridOn, selectedTool == MarkupTool.MOSAIC) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTool = MarkupTool.MOSAIC
                                }
                            }
                        }
                    }
                }

                // 4. WhatsApp-Style Text Editor Modal
                if (showTextPrompt) {
                    StudioTextDialog(
                        initialText = textInput,
                        initialColor = selectedColor,
                        initialStyle = textStyle,
                        onDismiss = { showTextPrompt = false },
                        onConfirm = { text, color, style ->
                            if (text.isNotBlank()) {
                                items.add(
                                    MarkupItem.TextCallout(
                                        x = textPromptPosition.x,
                                        y = textPromptPosition.y,
                                        text = text.trim(),
                                        colorArgb = color.toArgb(),
                                        style = style,
                                        strokeWidthNormalized = 0.026f
                                    )
                                )
                                redoStack.clear()
                            }
                            showTextPrompt = false
                        }
                    )
                }

                // 5. Discard Changes Confirmation
                if (showDiscardConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDiscardConfirm = false },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = Color(0xFF1E232E),
                        title = {
                            Text(
                                stringResource(R.string.markup_discard_title),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                stringResource(R.string.markup_discard_msg),
                                color = Color(0xFFB0B7C3)
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDiscardConfirm = false
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF334B)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.markup_discard_btn), fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDiscardConfirm = false }) {
                                Text(stringResource(R.string.markup_discard_cancel), color = Color.White)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioIconTool(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF00E676) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isSelected) Color(0xFF0A1810) else Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun StudioTextDialog(
    initialText: String,
    initialColor: Color,
    initialStyle: MarkupTextStyle,
    onDismiss: () -> Unit,
    onConfirm: (String, Color, MarkupTextStyle) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var color by remember { mutableStateOf(initialColor) }
    var style by remember { mutableStateOf(initialStyle) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xEE0A0D14)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                    }

                    // Style Switcher Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            MarkupTextStyle.BADGE to R.string.markup_text_style_badge,
                            MarkupTextStyle.FROSTED to R.string.markup_text_style_frosted,
                            MarkupTextStyle.OUTLINE to R.string.markup_text_style_outline
                        ).forEach { (s, labelRes) ->
                            val isSel = style == s
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) Color(0xFF00E5FF) else Color(0xFF222836),
                                modifier = Modifier.clickable { style = s }
                            ) {
                                Text(
                                    stringResource(labelRes),
                                    color = if (isSel) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { onConfirm(text, color, style) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.add_btn), color = Color(0xFF0A1810), fontWeight = FontWeight.Bold)
                    }
                }

                // Centered Input
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = {
                            Text(
                                stringResource(R.string.markup_text_prompt_hint),
                                color = Color(0x66FFFFFF),
                                fontSize = 18.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = color,
                            unfocusedBorderColor = Color(0x44FFFFFF)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )
                }

                // Color Palette
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StudioColors.forEach { c ->
                        val isSel = color == c
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (isSel) 3.dp else 1.dp,
                                    color = if (isSel) Color.White else Color(0x44FFFFFF),
                                    shape = CircleShape
                                )
                                .clickable { color = c }
                        )
                    }
                }
            }
        }
    }
}

private fun renderMarkupPreview(
    scope: DrawScope,
    item: MarkupItem,
    width: Float,
    height: Float,
    minDim: Float
) {
    when (item) {
        is MarkupItem.Arrow -> {
            val startX = item.startX * width
            val startY = item.startY * height
            val endX = item.endX * width
            val endY = item.endY * height
            val strokePx = (item.strokeWidthNormalized * minDim).coerceAtLeast(3f)

            // Outline shadow
            scope.drawLine(
                color = Color.Black,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokePx + 3.5f
            )
            // Main line
            scope.drawLine(
                color = Color(item.colorArgb),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokePx
            )

            // Arrowhead
            val angle = atan2((endY - startY).toDouble(), (endX - startX).toDouble())
            val headLen = minDim * 0.045f
            val x1 = endX - headLen * cos(angle - 0.52).toFloat()
            val y1 = endY - headLen * sin(angle - 0.52).toFloat()
            val x2 = endX - headLen * cos(angle + 0.52).toFloat()
            val y2 = endY - headLen * sin(angle + 0.52).toFloat()

            val headPath = Path().apply {
                moveTo(endX, endY)
                lineTo(x1, y1)
                lineTo(x2, y2)
                close()
            }
            scope.drawPath(headPath, color = Color.Black, style = Stroke(width = 2.5f))
            scope.drawPath(headPath, color = Color(item.colorArgb), style = Fill)
        }
        is MarkupItem.Rectangle -> {
            val left = minOf(item.left, item.right) * width
            val right = maxOf(item.left, item.right) * width
            val top = minOf(item.top, item.bottom) * height
            val bottom = maxOf(item.top, item.bottom) * height
            val strokePx = (item.strokeWidthNormalized * minDim).coerceAtLeast(3f)

            scope.drawRect(
                color = Color.Black,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = strokePx + 3f)
            )
            scope.drawRect(
                color = Color(item.colorArgb),
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = strokePx)
            )
        }
        is MarkupItem.Circle -> {
            val cx = item.centerX * width
            val cy = item.centerY * height
            val r = item.radius * minDim
            val strokePx = (item.strokeWidthNormalized * minDim).coerceAtLeast(3f)

            scope.drawCircle(
                color = Color.Black,
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = strokePx + 3f)
            )
            scope.drawCircle(
                color = Color(item.colorArgb),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = strokePx)
            )
        }
        is MarkupItem.Line -> {
            val startX = item.startX * width
            val startY = item.startY * height
            val endX = item.endX * width
            val endY = item.endY * height
            val strokePx = (item.strokeWidthNormalized * minDim).coerceAtLeast(3f)

            scope.drawLine(
                color = Color.Black,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokePx + 3.5f
            )
            scope.drawLine(
                color = Color(item.colorArgb),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokePx
            )
        }
        is MarkupItem.Freehand -> {
            if (item.points.size < 2) return
            val strokePx = (item.strokeWidthNormalized * minDim).coerceAtLeast(3f)
            val path = Path()
            val first = item.points.first()
            path.moveTo(first.first * width, first.second * height)
            for (i in 1 until item.points.size) {
                val prev = item.points[i - 1]
                val curr = item.points[i]
                val midX = (prev.first + curr.first) / 2f * width
                val midY = (prev.second + curr.second) / 2f * height
                path.quadraticTo(prev.first * width, prev.second * height, midX, midY)
            }
            val last = item.points.last()
            path.lineTo(last.first * width, last.second * height)

            scope.drawPath(path, color = Color.Black, style = Stroke(width = strokePx + 2.5f))
            scope.drawPath(path, color = Color(item.colorArgb), style = Stroke(width = strokePx))
        }
        is MarkupItem.Highlighter -> {
            if (item.points.size < 2) return
            val strokePx = (item.strokeWidthNormalized * minDim).coerceAtLeast(16f)
            val path = Path()
            val first = item.points.first()
            path.moveTo(first.first * width, first.second * height)
            for (i in 1 until item.points.size) {
                val prev = item.points[i - 1]
                val curr = item.points[i]
                val midX = (prev.first + curr.first) / 2f * width
                val midY = (prev.second + curr.second) / 2f * height
                path.quadraticTo(prev.first * width, prev.second * height, midX, midY)
            }
            val last = item.points.last()
            path.lineTo(last.first * width, last.second * height)

            scope.drawPath(path, color = Color(item.colorArgb).copy(alpha = 0.45f), style = Stroke(width = strokePx))
        }
        is MarkupItem.NumberedPin -> {
            val cx = item.x * width
            val cy = item.y * height
            val radius = (item.strokeWidthNormalized * minDim).coerceIn(16f, 44f)

            // Shadow
            scope.drawCircle(
                color = Color.Black.copy(alpha = 0.6f),
                radius = radius + 2.5f,
                center = Offset(cx, cy)
            )
            // Color disc
            scope.drawCircle(
                color = Color(item.colorArgb),
                radius = radius,
                center = Offset(cx, cy)
            )
            // White ring
            scope.drawCircle(
                color = Color.White,
                radius = radius - 1.5f,
                center = Offset(cx, cy),
                style = Stroke(width = 2f)
            )

            // Number text
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = radius * 1.15f
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
            scope.drawContext.canvas.nativeCanvas.drawText(
                item.number.toString(),
                cx,
                textY,
                textPaint
            )
        }
        is MarkupItem.BlurRect -> {
            val left = minOf(item.left, item.right) * width
            val right = maxOf(item.left, item.right) * width
            val top = minOf(item.top, item.bottom) * height
            val bottom = maxOf(item.top, item.bottom) * height

            if (item.isMosaic) {
                // Mosaic: Darker, more opaque pattern-like fill
                scope.drawRect(
                    color = Color(0xBB222222),
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top)
                )
                // Optional: Draw a subtle grid to simulate mosaic pixels
                val gridSize = 10f
                var x = left
                while (x < right) {
                    scope.drawLine(Color(0x33FFFFFF), Offset(x, top), Offset(x, bottom), 1f)
                    x += gridSize
                }
                var y = top
                while (y < bottom) {
                    scope.drawLine(Color(0x33FFFFFF), Offset(left, y), Offset(right, y), 1f)
                    y += gridSize
                }
            } else {
                // Blur: Soft translucent blue-ish mask
                scope.drawRect(
                    color = Color(0x88334455),
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top)
                )
            }

            scope.drawRect(
                color = Color.White,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 1.5f)
            )
        }
        is MarkupItem.TextCallout -> {
            if (item.text.isBlank()) return
            val x = item.x * width
            val y = item.y * height
            val fontSize = (item.strokeWidthNormalized * minDim).coerceIn(18f, 54f)

            val textPaint = android.graphics.Paint().apply {
                color = when (item.style) {
                    MarkupTextStyle.BADGE, MarkupTextStyle.FROSTED -> android.graphics.Color.WHITE
                    MarkupTextStyle.OUTLINE, MarkupTextStyle.TRANSPARENT -> item.colorArgb
                }
                textSize = fontSize
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            }
            val textWidth = textPaint.measureText(item.text)
            val padH = fontSize * 0.45f
            val padV = fontSize * 0.3f
            val pillRect = android.graphics.RectF(
                x - padH,
                y - fontSize - padV,
                x + textWidth + padH,
                y + padV
            )
            val radius = fontSize * 0.25f

            when (item.style) {
                MarkupTextStyle.BADGE -> {
                    val bgPaint = android.graphics.Paint().apply {
                        color = item.colorArgb
                        style = android.graphics.Paint.Style.FILL
                        setShadowLayer(5f, 1f, 2f, android.graphics.Color.argb(120, 0, 0, 0))
                    }
                    scope.drawContext.canvas.nativeCanvas.drawRoundRect(pillRect, radius, radius, bgPaint)
                    scope.drawContext.canvas.nativeCanvas.drawText(item.text, x, y - padV * 0.2f, textPaint)
                }
                MarkupTextStyle.FROSTED -> {
                    val bgPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        alpha = 190
                        style = android.graphics.Paint.Style.FILL
                    }
                    val borderPaint = android.graphics.Paint().apply {
                        color = item.colorArgb
                        strokeWidth = 2.5f
                        style = android.graphics.Paint.Style.STROKE
                    }
                    scope.drawContext.canvas.nativeCanvas.drawRoundRect(pillRect, radius, radius, bgPaint)
                    scope.drawContext.canvas.nativeCanvas.drawRoundRect(pillRect, radius, radius, borderPaint)
                    scope.drawContext.canvas.nativeCanvas.drawText(item.text, x, y - padV * 0.2f, textPaint)
                }
                MarkupTextStyle.OUTLINE, MarkupTextStyle.TRANSPARENT -> {
                    val outlinePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = fontSize
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 4f
                    }
                    scope.drawContext.canvas.nativeCanvas.drawText(item.text, x, y - padV * 0.2f, outlinePaint)
                    scope.drawContext.canvas.nativeCanvas.drawText(item.text, x, y - padV * 0.2f, textPaint)
                }
            }
        }
        else -> Unit
    }
}
