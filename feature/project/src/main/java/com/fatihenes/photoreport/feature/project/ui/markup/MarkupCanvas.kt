@file:Suppress("MagicNumber", "LongMethod", "CyclomaticComplexMethod", "ComplexCondition", "MaxLineLength", "TooManyFunctions")
package com.fatihenes.photoreport.feature.project.ui.markup

import android.graphics.BitmapFactory
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.fatihenes.photoreport.core.media.ImageProcessor
import com.fatihenes.photoreport.core.media.geometry.MarkupGeometry
import com.fatihenes.photoreport.core.media.geometry.MarkupTransformEngine
import com.fatihenes.photoreport.core.media.geometry.TransformHandle
import com.fatihenes.photoreport.core.model.MarkupItem
import com.fatihenes.photoreport.core.model.MarkupTextStyle
import com.fatihenes.photoreport.core.model.MarkupTool
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.feature.project.ui.markup.selection.MarkupSelectionController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class CropDragMode {
    NONE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER_MOVE
}

@Composable
fun MarkupCanvas(
    photo: Photo,
    canvasState: MarkupCanvasState,
    onItemCreated: (MarkupItem) -> Unit,
    onItemUpdated: (before: MarkupItem, after: MarkupItem) -> Unit,
    onItemUpdatedLive: (MarkupItem) -> Unit,
    onCropBoundsChanged: (left: Float, top: Float, right: Float, bottom: Float) -> Unit,
    onCropBoundsChangedLive: (left: Float, top: Float, right: Float, bottom: Float) -> Unit,
    onSelectItem: (String?) -> Unit,
    onItemDeleted: (String) -> Unit = {},
    onTextTap: (Offset) -> Unit,
    onDoubleTapItem: (MarkupItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val currentCanvasState by rememberUpdatedState(canvasState)
    val currentOnItemCreated by rememberUpdatedState(onItemCreated)
    val currentOnItemUpdated by rememberUpdatedState(onItemUpdated)
    val currentOnItemUpdatedLive by rememberUpdatedState(onItemUpdatedLive)
    val currentOnCropBoundsChanged by rememberUpdatedState(onCropBoundsChanged)
    val currentOnCropBoundsChangedLive by rememberUpdatedState(onCropBoundsChangedLive)
    val currentOnSelectItem by rememberUpdatedState(onSelectItem)
    val currentOnTextTap by rememberUpdatedState(onTextTap)
    val currentOnDoubleTapItem by rememberUpdatedState(onDoubleTapItem)

    var currentDragStart by remember { mutableStateOf<Offset?>(null) }
    var currentDragEnd by remember { mutableStateOf<Offset?>(null) }
    val currentFreehandPoints = remember { mutableStateListOf<Pair<Float, Float>>() }

    var lastTapTime by remember { mutableStateOf(0L) }
    var lastTappedItem by remember { mutableStateOf<MarkupItem?>(null) }

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
            if (w > 0 && h > 0) imageAspectRatio = w.toFloat() / h.toFloat()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight
        val containerAspect = containerWidth.value / containerHeight.value

        val (fittedWidth, fittedHeight) = if (imageAspectRatio > containerAspect) {
            containerWidth to (containerWidth / imageAspectRatio)
        } else {
            (containerHeight * imageAspectRatio) to containerHeight
        }

        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }
        var lastTapTime by remember { mutableStateOf(0L) }
        var activeSnapLines by remember { mutableStateOf<List<com.fatihenes.photoreport.core.media.geometry.SnapGuideLine>>(emptyList()) }
        var isPointerOverTrash by remember { mutableStateOf(false) }
        var isDraggingItem by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .size(fittedWidth, fittedHeight)
                .clipToBounds()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                    rotationZ = currentCanvasState.cropState.rotationDegrees.toFloat()
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastTapTime < 300) {
                            // Double Tap: Reset pan/zoom
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                            lastTapTime = 0L
                            down.consume()
                            return@awaitEachGesture
                        }
                        lastTapTime = currentTime

                        var transformActive = false
                        var drawingActive = false
                        var movingActive = false
                        var handleActive = false
                        var cropActive = false
                        var cropDragMode = CropDragMode.NONE

                        val state = currentCanvasState
                        val normX = (down.position.x / size.width).coerceIn(0f, 1f)
                        val normY = (down.position.y / size.height).coerceIn(0f, 1f)

                        val selectedItem = state.items.find { it.id == state.selectedItemId }
                        var itemBeforeTransform: MarkupItem? = selectedItem
                        var currentLiveItem: MarkupItem? = selectedItem
                        
                        var lastDidSnapX = false
                        var lastDidSnapY = false

                        val crop = state.cropState
                        var curCropL = crop.cropLeft
                        var curCropT = crop.cropTop
                        var curCropR = crop.cropRight
                        var curCropB = crop.cropBottom

                        var totalDx = 0f
                        var totalDy = 0f

                        // 1. Check Crop Mode gestures
                        if (state.selectedTool == MarkupTool.CROP) {
                            val tol = 0.08f
                            cropDragMode = when {
                                abs(normX - curCropL) < tol && abs(normY - curCropT) < tol -> CropDragMode.TOP_LEFT
                                abs(normX - curCropR) < tol && abs(normY - curCropT) < tol -> CropDragMode.TOP_RIGHT
                                abs(normX - curCropL) < tol && abs(normY - curCropB) < tol -> CropDragMode.BOTTOM_LEFT
                                abs(normX - curCropR) < tol && abs(normY - curCropB) < tol -> CropDragMode.BOTTOM_RIGHT
                                normX in curCropL..curCropR && normY in curCropT..curCropB -> CropDragMode.CENTER_MOVE
                                else -> CropDragMode.NONE
                            }
                            if (cropDragMode != CropDragMode.NONE) {
                                cropActive = true
                            }
                        }

                        // 2. Check handle touch on selected item
                        val touchedHandle = if (!cropActive && selectedItem != null) {
                            MarkupSelectionController.findTouchedHandle(selectedItem, normX, normY, scale)
                        } else null

                        val activeHandle = touchedHandle
                        if (touchedHandle != null) {
                            handleActive = true
                        }

                        val hitItem = if (!handleActive && !cropActive) {
                            MarkupGeometry.findTopmostHitItem(state.items, normX, normY, scale)
                        } else null

                        do {
                            val event = awaitPointerEvent()
                            val pointers = event.changes

                            if (pointers.size >= 2) {
                                // 2-finger Pan & Zoom with bounding limits
                                transformActive = true
                                drawingActive = false
                                handleActive = false
                                movingActive = false
                                cropActive = false

                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()

                                val nextScale = (scale * zoomChange).coerceIn(1f, 8f)
                                scale = nextScale

                                val maxOffsetX = (size.width * (nextScale - 1f) / 2f).coerceAtLeast(0f)
                                val maxOffsetY = (size.height * (nextScale - 1f) / 2f).coerceAtLeast(0f)

                                if (nextScale <= 1.05f) {
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    offsetX = (offsetX + panChange.x).coerceIn(-maxOffsetX, maxOffsetX)
                                    offsetY = (offsetY + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
                                }

                                pointers.forEach { it.consume() }
                            } else if (pointers.size == 1 && !transformActive) {
                                val pointer = pointers.first()
                                if (pointer.pressed) {
                                    val currNormX = (pointer.position.x / size.width).coerceIn(0f, 1f)
                                    val currNormY = (pointer.position.y / size.height).coerceIn(0f, 1f)

                                    val frameDx = (pointer.position.x - pointer.previousPosition.x) / size.width
                                    val frameDy = (pointer.position.y - pointer.previousPosition.y) / size.height
                                    totalDx += frameDx
                                    totalDy += frameDy

                                    if (cropActive) {
                                        // Live Crop handle manipulation
                                        when (cropDragMode) {
                                            CropDragMode.TOP_LEFT -> {
                                                curCropL = (curCropL + frameDx).coerceIn(0f, curCropR - 0.1f)
                                                curCropT = (curCropT + frameDy).coerceIn(0f, curCropB - 0.1f)
                                            }
                                            CropDragMode.TOP_RIGHT -> {
                                                curCropR = (curCropR + frameDx).coerceIn(curCropL + 0.1f, 1f)
                                                curCropT = (curCropT + frameDy).coerceIn(0f, curCropB - 0.1f)
                                            }
                                            CropDragMode.BOTTOM_LEFT -> {
                                                curCropL = (curCropL + frameDx).coerceIn(0f, curCropR - 0.1f)
                                                curCropB = (curCropB + frameDy).coerceIn(curCropT + 0.1f, 1f)
                                            }
                                            CropDragMode.BOTTOM_RIGHT -> {
                                                curCropR = (curCropR + frameDx).coerceIn(curCropL + 0.1f, 1f)
                                                curCropB = (curCropB + frameDy).coerceIn(curCropT + 0.1f, 1f)
                                            }
                                            CropDragMode.CENTER_MOVE -> {
                                                val w = curCropR - curCropL
                                                val h = curCropB - curCropT
                                                curCropL = (curCropL + frameDx).coerceIn(0f, 1f - w)
                                                curCropT = (curCropT + frameDy).coerceIn(0f, 1f - h)
                                                curCropR = curCropL + w
                                                curCropB = curCropT + h
                                            }
                                            CropDragMode.NONE -> Unit
                                        }
                                        currentOnCropBoundsChangedLive(curCropL, curCropT, curCropR, curCropB)
                                        pointer.consume()
                                    } else if (handleActive && itemBeforeTransform != null && activeHandle != null) {
                                        // Resize / Scale item
                                        val base = itemBeforeTransform
                                        val updated = if (activeHandle == TransformHandle.ROTATION) {
                                            val center = MarkupGeometry.getItemCenter(base)
                                            val newRot = MarkupTransformEngine.calculateRotation(center, currNormX, currNormY, snapEnabled = true)
                                            base.rotateBy(newRot - base.rotation)
                                        } else {
                                            MarkupTransformEngine.resizeItem(base, activeHandle, totalDx, totalDy)
                                        }
                                        currentLiveItem = updated
                                        currentOnItemUpdatedLive(updated)
                                        pointer.consume()
                                    } else if (selectedItem != null && (movingActive || MarkupGeometry.hitTest(selectedItem, currNormX, currNormY, scale))) {
                                        // Move selected item smoothly with Snapping & Haptics
                                        movingActive = true
                                        val base = itemBeforeTransform ?: selectedItem
                                        itemBeforeTransform = base
                                        
                                        val rawUpdated = base.translateBy(totalDx, totalDy)
                                        val rawCenter = MarkupGeometry.getItemCenter(rawUpdated)
                                        val rawBounds = MarkupGeometry.getItemBounds(rawUpdated)
                                        
                                        val snapResult = com.fatihenes.photoreport.core.media.geometry.MarkupSnapEngine.snapPosition(
                                            rawCenter, rawBounds, state.items.filter { it.id != base.id }, scale, enabled = true
                                        )
                                        
                                        val snapDx = snapResult.snappedX - rawCenter.x
                                        val snapDy = snapResult.snappedY - rawCenter.y
                                        var updated = rawUpdated.translateBy(snapDx, snapDy)
                                        
                                        if ((snapResult.didSnapX && !lastDidSnapX) || (snapResult.didSnapY && !lastDidSnapY)) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        lastDidSnapX = snapResult.didSnapX
                                        lastDidSnapY = snapResult.didSnapY
                                        activeSnapLines = snapResult.guideLines
                                        
                                        isDraggingItem = true
                                        isPointerOverTrash = currNormY > 0.85f
                                        
                                        if (isPointerOverTrash) {
                                            // Scale down to show it will be deleted
                                            updated = updated.scaleBy(0.8f / updated.scale)
                                        }
                                        
                                        currentLiveItem = updated
                                        currentOnItemUpdatedLive(updated)
                                        pointer.consume()
                                    } else if (state.selectedTool != MarkupTool.TEXT &&
                                        state.selectedTool != MarkupTool.NUMBERED_PIN &&
                                        state.selectedTool != MarkupTool.CROP
                                    ) {
                                        // Drawing tool (Freehand, Fluid Arrow, Straight Line, Blur Brush, Blur Box)
                                        drawingActive = true
                                        if (currentDragStart == null) {
                                            currentDragStart = Offset(currNormX, currNormY)
                                            currentFreehandPoints.clear()
                                            currentFreehandPoints.add(currNormX to currNormY)
                                        }
                                        currentDragEnd = Offset(currNormX, currNormY)

                                        // Micro-smoothing to filter out hand tremors
                                        val lastPt = currentFreehandPoints.last()
                                        val dist = hypot(currNormX - lastPt.first, currNormY - lastPt.second)
                                        if (dist >= 0.0025f) {
                                            val smoothX = lastPt.first * 0.2f + currNormX * 0.8f
                                            val smoothY = lastPt.second * 0.2f + currNormY * 0.8f
                                            currentFreehandPoints.add(smoothX to smoothY)
                                        }
                                        pointer.consume()
                                    }
                                }
                            }
                        } while (pointers.any { it.pressed })

                        // Commit completed gesture
                        if (transformActive) {
                            if (scale <= 1.05f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            }
                        } else if (cropActive) {
                            currentOnCropBoundsChanged(curCropL, curCropT, curCropR, curCropB)
                        } else if (handleActive || movingActive) {
                            activeSnapLines = emptyList()
                            isDraggingItem = false
                            
                            val before = itemBeforeTransform
                            val after = currentLiveItem
                            
                            if (movingActive && isPointerOverTrash && before != null) {
                                onItemDeleted(before.id)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } else if (before != null && after != null && before != after) {
                                currentOnItemUpdated(before, after)
                            }
                            isPointerOverTrash = false
                        } else if (drawingActive) {
                            val start = currentDragStart
                            val end = currentDragEnd
                            if (start != null && end != null) {
                                val item = createDragItem(
                                    tool = state.selectedTool,
                                    blurMode = state.blurMode,
                                    start = start,
                                    end = end,
                                    freehandPoints = currentFreehandPoints.toList(),
                                    color = state.selectedColor,
                                    strokeThickness = state.strokeThickness,
                                    blurStrokeWidth = state.blurStrokeWidth
                                )
                                item?.let {
                                    currentOnItemCreated(it)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        } else {
                            val now = System.currentTimeMillis()
                            if (hitItem != null) {
                                if (hitItem == lastTappedItem && (now - lastTapTime) < 400) {
                                    currentOnDoubleTapItem(hitItem)
                                } else {
                                    currentOnSelectItem(hitItem.id)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                lastTappedItem = hitItem
                                lastTapTime = now
                            } else {
                                if ((now - lastTapTime) < 400 && scale > 1.05f) {
                                    // Double tap on empty canvas resets zoom & pan
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    currentOnSelectItem(null)
                                    lastTappedItem = null
                                    when (state.selectedTool) {
                                        MarkupTool.TEXT -> currentOnTextTap(Offset(normX, normY))
                                        MarkupTool.NUMBERED_PIN -> {
                                            val pin = MarkupItem.NumberedPin(
                                                x = normX, y = normY, number = 0,
                                                colorArgb = state.selectedColor.toArgb()
                                            )
                                            currentOnItemCreated(pin)
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        else -> Unit
                                    }
                                }
                                lastTapTime = now
                            }
                        }

                        currentDragStart = null
                        currentDragEnd = null
                        currentFreehandPoints.clear()
                    }
                }
        ) {
            // 1. Photo Image
            AsyncImage(
                model = ImageRequest.Builder(context).data(photo.filePath).build(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )

            // 2. Vector & Pixel Annotations Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val minDim = minOf(w, h)

                // Render items in ascending zIndex order
                canvasState.items.filter { it.isVisible }.sortedBy { it.zIndex }.forEach { item ->
                    renderMarkupPreview(this, item, w, h, minDim, canvasState.blurredImageBitmap)
                }

                // Render active drag creation preview
                val start = currentDragStart
                val end = currentDragEnd
                if (start != null && end != null) {
                    val preview = createDragItem(
                        tool = canvasState.selectedTool,
                        blurMode = canvasState.blurMode,
                        start = start,
                        end = end,
                        freehandPoints = currentFreehandPoints.toList(),
                        color = canvasState.selectedColor,
                        strokeThickness = canvasState.strokeThickness,
                        blurStrokeWidth = canvasState.blurStrokeWidth
                    )
                    preview?.let { renderMarkupPreview(this, it, w, h, minDim, canvasState.blurredImageBitmap) }
                }

                // Render Selection Overlay for selected item
                val selectedId = canvasState.selectedItemId
                if (!isDraggingItem && selectedId != null && canvasState.selectedTool != MarkupTool.CROP) {
                    val selectedItem = canvasState.items.find { it.id == selectedId }
                    selectedItem?.let { drawCleanSelectionOverlay(this, it, w, h) }
                }

                // Render Snap Guide Lines
                if (activeSnapLines.isNotEmpty()) {
                    val guidePaint = androidx.compose.ui.graphics.Paint().apply {
                        color = MarkupAccentGreen
                        strokeWidth = 2f
                    }
                    activeSnapLines.forEach { line ->
                        if (line.isVertical) {
                            drawLine(MarkupAccentGreen, Offset(line.position * w, line.start * h), Offset(line.position * w, line.end * h), strokeWidth = 2.dp.toPx())
                        } else {
                            drawLine(MarkupAccentGreen, Offset(line.start * w, line.position * h), Offset(line.end * w, line.position * h), strokeWidth = 2.dp.toPx())
                        }
                    }
                }

                // Render Live Interactive Crop Overlay when Crop tool is active
                if (canvasState.selectedTool == MarkupTool.CROP) {
                    val cropState = canvasState.cropState
                    drawLiveCropOverlay(this, cropState.cropLeft, cropState.cropTop, cropState.cropRight, cropState.cropBottom, w, h)
                }

                // Render Trash Can if dragging
                if (isDraggingItem) {
                    val trashSize = if (isPointerOverTrash) 72.dp.toPx() else 56.dp.toPx()
                    val trashColor = if (isPointerOverTrash) Color.Red else Color.Black.copy(alpha = 0.5f)
                    val trashCenter = Offset(w / 2f, h - 80.dp.toPx())
                    
                    drawCircle(trashColor, trashSize / 2f, trashCenter)
                    drawCircle(Color.White, trashSize / 2f - 4f, trashCenter, style = Stroke(width = 2f))
                    
                    // Simple X or Icon for Trash
                    val p1 = Offset(trashCenter.x - 12f, trashCenter.y - 12f)
                    val p2 = Offset(trashCenter.x + 12f, trashCenter.y + 12f)
                    val p3 = Offset(trashCenter.x + 12f, trashCenter.y - 12f)
                    val p4 = Offset(trashCenter.x - 12f, trashCenter.y + 12f)
                    
                    drawLine(Color.White, p1, p2, strokeWidth = 4f, cap = StrokeCap.Round)
                    drawLine(Color.White, p3, p4, strokeWidth = 4f, cap = StrokeCap.Round)
                }
            }
        }
    }
}

// ── Live Interactive Crop Overlay ───────────────────────────────────

private fun drawLiveCropOverlay(
    scope: DrawScope,
    cropLeft: Float, cropTop: Float, cropRight: Float, cropBottom: Float,
    width: Float, height: Float
) {
    val l = (cropLeft * width).coerceIn(0f, width)
    val t = (cropTop * height).coerceIn(0f, height)
    val r = (cropRight * width).coerceIn(l + 1f, width)
    val b = (cropBottom * height).coerceIn(t + 1f, height)
    val cropW = r - l
    val cropH = b - t

    val dimColor = Color(0xB3000000)

    // 4 Outer Dimmed Rectangles
    scope.drawRect(dimColor, Offset(0f, 0f), Size(width, t))
    scope.drawRect(dimColor, Offset(0f, b), Size(width, height - b))
    scope.drawRect(dimColor, Offset(0f, t), Size(l, cropH))
    scope.drawRect(dimColor, Offset(r, t), Size(width - r, cropH))

    // Inner 3x3 Rule-of-Thirds Grid lines
    val gridColor = Color.White.copy(alpha = 0.35f)
    val lineW = (1f * scope.density).coerceAtLeast(1f)
    val x1 = l + cropW / 3f; val x2 = l + cropW * 2f / 3f
    val y1 = t + cropH / 3f; val y2 = t + cropH * 2f / 3f
    scope.drawLine(gridColor, Offset(x1, t), Offset(x1, b), lineW)
    scope.drawLine(gridColor, Offset(x2, t), Offset(x2, b), lineW)
    scope.drawLine(gridColor, Offset(l, y1), Offset(r, y1), lineW)
    scope.drawLine(gridColor, Offset(l, y2), Offset(r, y2), lineW)

    // White Crop Boundary
    scope.drawRect(Color.White.copy(alpha = 0.8f), Offset(l, t), Size(cropW, cropH), style = Stroke((1.5f * scope.density).coerceAtLeast(1.5f)))

    // 4 Corner 'L' Handles
    val bracketLen = (22f * scope.density).coerceAtMost(cropW / 3f).coerceAtMost(cropH / 3f)
    val bracketThick = (3.5f * scope.density).coerceAtLeast(3f)
    val bracketColor = Color.White

    // Top-Left
    scope.drawLine(bracketColor, Offset(l - bracketThick / 2, t), Offset(l + bracketLen, t), bracketThick)
    scope.drawLine(bracketColor, Offset(l, t), Offset(l, t + bracketLen), bracketThick)

    // Top-Right
    scope.drawLine(bracketColor, Offset(r + bracketThick / 2, t), Offset(r - bracketLen, t), bracketThick)
    scope.drawLine(bracketColor, Offset(r, t), Offset(r, t + bracketLen), bracketThick)

    // Bottom-Left
    scope.drawLine(bracketColor, Offset(l - bracketThick / 2, b), Offset(l + bracketLen, b), bracketThick)
    scope.drawLine(bracketColor, Offset(l, b), Offset(l, b - bracketLen), bracketThick)

    // Bottom-Right
    scope.drawLine(bracketColor, Offset(r + bracketThick / 2, b), Offset(r - bracketLen, b), bracketThick)
    scope.drawLine(bracketColor, Offset(r, b), Offset(r, b - bracketLen), bracketThick)
}

// ── Clean Selection Overlay ──────────────────────────────────────────

private fun drawCleanSelectionOverlay(scope: DrawScope, item: MarkupItem, width: Float, height: Float) {
    val bounds = MarkupGeometry.getItemBounds(item)
    val center = MarkupGeometry.getItemCenter(item)
    val centerOffset = Offset(center.x * width, center.y * height)

    val selectionColor = Color(0xFF007AFF) // Premium iOS Blue
    val paddingX = with(scope) { 12.dp.toPx() }
    val paddingY = with(scope) { 12.dp.toPx() }

    scope.withTransform({
        rotate(item.rotation, centerOffset)
        scale(item.scale, item.scale, centerOffset)
    }) {
        val rectTopLeft = Offset(bounds.left * width - paddingX, bounds.top * height - paddingY)
        val rectSize = Size(bounds.width * width + paddingX * 2, bounds.height * height + paddingY * 2)

        // Solid selection box with rounded corners
        drawRoundRect(
            color = selectionColor,
            topLeft = rectTopLeft,
            size = rectSize,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )

        val handleRadius = 6.dp.toPx()

        val corners = listOf(
            rectTopLeft,
            Offset(rectTopLeft.x + rectSize.width, rectTopLeft.y),
            Offset(rectTopLeft.x, rectTopLeft.y + rectSize.height),
            Offset(rectTopLeft.x + rectSize.width, rectTopLeft.y + rectSize.height)
        )

        corners.forEach { corner ->
            // Drop shadow for handle
            drawCircle(Color.Black.copy(alpha = 0.15f), handleRadius + 2f, corner)
            // Handle fill
            drawCircle(Color.White, handleRadius, corner)
            // Handle border
            drawCircle(selectionColor, handleRadius, corner, style = Stroke(width = 1.5.dp.toPx()))
        }
    }
}

// ── Drag Item Factory ───────────────────────────────────────────────

private fun createDragItem(
    tool: MarkupTool,
    blurMode: BlurMode,
    start: Offset, end: Offset,
    freehandPoints: List<Pair<Float, Float>>,
    color: Color, strokeThickness: Float,
    blurStrokeWidth: Float
): MarkupItem? = when (tool) {
    MarkupTool.FREEHAND -> {
        if (freehandPoints.size >= 2) MarkupItem.Freehand(freehandPoints, color.toArgb(), strokeThickness)
        else null
    }
    MarkupTool.ARROW -> {
        if (freehandPoints.size >= 2) {
            // Fluid curved natural arrow
            MarkupItem.Arrow(
                startX = start.x, startY = start.y,
                endX = end.x, endY = end.y,
                colorArgb = color.toArgb(),
                strokeWidthNormalized = strokeThickness,
                points = freehandPoints
            )
        } else null
    }
    MarkupTool.LINE -> {
        // Pure unconstrained precise line
        MarkupItem.Line(start.x, start.y, end.x, end.y, color.toArgb(), strokeThickness)
    }
    MarkupTool.BLUR -> {
        if (blurMode == BlurMode.FREEHAND_BRUSH) {
            if (freehandPoints.size >= 2) MarkupItem.BlurPath(freehandPoints, blurStrokeWidth)
            else null
        } else {
            MarkupItem.BlurRect(start.x, start.y, end.x, end.y, isMosaic = false)
        }
    }
    MarkupTool.NUMBERED_PIN, MarkupTool.TEXT, MarkupTool.CROP -> null
    else -> null
}

// ── Preview Rendering ───────────────────────────────────────────────

private fun renderMarkupPreview(
    scope: DrawScope, item: MarkupItem,
    width: Float, height: Float, minDim: Float,
    blurredImageBitmap: androidx.compose.ui.graphics.ImageBitmap? = null
) {
    val center = MarkupGeometry.getItemCenter(item)
    val centerOffset = Offset(center.x * width, center.y * height)

    scope.withTransform({
        rotate(item.rotation, centerOffset)
        scale(item.scale, item.scale, centerOffset)
    }) {
        when (item) {
            is MarkupItem.Arrow -> renderArrowPreview(this, item, width, height, minDim)
            is MarkupItem.Line -> renderLinePreview(this, item, width, height, minDim)
            is MarkupItem.Freehand -> renderFreehandPreview(this, item, width, height, minDim)
            is MarkupItem.NumberedPin -> renderNumberedPinPreview(this, item, width, height, minDim)
            is MarkupItem.TextCallout -> renderTextCalloutPreview(this, item, width, height, minDim)
            is MarkupItem.BlurRect -> renderBlurRectPreview(this, item, width, height, blurredImageBitmap)
            is MarkupItem.BlurPath -> renderBlurPathPreview(this, item, width, height, minDim, blurredImageBitmap)
            is MarkupItem.Rectangle -> renderRectanglePreview(this, item, width, height, minDim)
            is MarkupItem.Circle -> renderCirclePreview(this, item, width, height, minDim)
            is MarkupItem.Highlighter -> renderHighlighterPreview(this, item, width, height, minDim)
            is MarkupItem.Callout -> renderCalloutPreview(this, item, width, height, minDim)
            is MarkupItem.IssueMarker, is MarkupItem.StatusMarker, is MarkupItem.Measurement -> Unit
        }
    }
}

private fun renderArrowPreview(scope: DrawScope, item: MarkupItem.Arrow, width: Float, height: Float, minDim: Float) {
    val strokePx = (item.strokeWidthNormalized * minDim).coerceAtLeast(3.5f)
    val alpha = item.opacity
    val arrowHeadLength = maxOf(minDim * 0.045f, strokePx * 3.5f)

    if (item.points.size >= 2) {
        val p0 = item.points.first()
        val last = item.points.last()
        val prev = if (item.points.size >= 3) item.points[item.points.size - 2] else p0
        val fromX = prev.first * width; val fromY = prev.second * height
        val toX = last.first * width; val toY = last.second * height

        val angle = atan2((toY - fromY), (toX - fromX))
        val shaftTrim = arrowHeadLength * 0.65f
        val shaftEndX = toX - shaftTrim * cos(angle)
        val shaftEndY = toY - shaftTrim * sin(angle)

        val path = buildSmoothPreviewPathWithEnd(item.points, width, height, shaftEndX, shaftEndY)
        scope.drawPath(path, Color(item.colorArgb).copy(alpha = alpha), style = Stroke(strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))

        drawArrowHeadCompose(scope, fromX, fromY, toX, toY, arrowHeadLength, Color(item.colorArgb).copy(alpha = alpha))
    } else {
        val sX = item.startX * width; val sY = item.startY * height
        val eX = item.endX * width; val eY = item.endY * height

        val angle = atan2((eY - sY), (eX - sX))
        val shaftTrim = arrowHeadLength * 0.65f
        val shaftEndX = eX - shaftTrim * cos(angle)
        val shaftEndY = eY - shaftTrim * sin(angle)

        scope.drawLine(Color(item.colorArgb).copy(alpha = alpha), Offset(sX, sY), Offset(shaftEndX, shaftEndY), strokeWidth = strokePx, cap = StrokeCap.Round)
        drawArrowHeadCompose(scope, sX, sY, eX, eY, arrowHeadLength, Color(item.colorArgb).copy(alpha = alpha))
    }
}

private fun drawArrowHeadCompose(scope: DrawScope, fromX: Float, fromY: Float, toX: Float, toY: Float, arrowHeadLength: Float, color: Color) {
    val angle = atan2((toY - fromY), (toX - fromX))
    val headAngle = 0.54f
    val x1 = toX - arrowHeadLength * cos(angle - headAngle)
    val y1 = toY - arrowHeadLength * sin(angle - headAngle)
    val x2 = toX - arrowHeadLength * cos(angle + headAngle)
    val y2 = toY - arrowHeadLength * sin(angle + headAngle)
    val headPath = Path().apply { moveTo(toX, toY); lineTo(x1, y1); lineTo(x2, y2); close() }
    scope.drawPath(headPath, color, style = Fill)
}

private fun renderLinePreview(scope: DrawScope, item: MarkupItem.Line, width: Float, height: Float, minDim: Float) {
    val sX = item.startX * width; val sY = item.startY * height
    val eX = item.endX * width; val eY = item.endY * height
    val strokePx = (item.strokeWidthNormalized * minDim).coerceAtLeast(3.5f)
    scope.drawLine(Color(item.colorArgb), Offset(sX, sY), Offset(eX, eY), strokePx, cap = StrokeCap.Round)
}

private fun renderFreehandPreview(scope: DrawScope, item: MarkupItem.Freehand, width: Float, height: Float, minDim: Float) {
    if (item.points.size < 2) return
    val strokePx = (item.strokeWidthNormalized * minDim).coerceAtLeast(3f)
    val path = buildSmoothPreviewPath(item.points, width, height)
    scope.drawPath(path, Color(item.colorArgb), style = Stroke(strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun renderNumberedPinPreview(scope: DrawScope, item: MarkupItem.NumberedPin, width: Float, height: Float, minDim: Float) {
    val cx = item.x * width; val cy = item.y * height
    val radius = (item.strokeWidthNormalized * minDim).coerceIn(16f, 40f)

    val r = (item.colorArgb ushr 16) and 0xFF
    val g = (item.colorArgb ushr 8) and 0xFF
    val b = item.colorArgb and 0xFF
    val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    val isLight = luminance > 0.65

    scope.drawCircle(Color.Black.copy(alpha = 0.4f), radius + 2f, Offset(cx, cy))
    scope.drawCircle(Color(item.colorArgb), radius, Offset(cx, cy))
    val ringColor = if (isLight) Color.Black.copy(alpha = 0.35f) else Color.White
    scope.drawCircle(ringColor, radius - 1.5f, Offset(cx, cy), style = Stroke(2f))

    val textPaint = android.graphics.Paint().apply {
        color = if (isLight) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        textSize = radius * 1.15f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        textAlign = android.graphics.Paint.Align.CENTER
    }
    scope.drawContext.canvas.nativeCanvas.drawText(item.number.toString(), cx, cy - (textPaint.descent() + textPaint.ascent()) / 2f, textPaint)
}

private fun renderTextCalloutPreview(scope: DrawScope, item: MarkupItem.TextCallout, width: Float, height: Float, minDim: Float) {
    if (item.text.isBlank()) return
    val x = item.x * width; val y = item.y * height; val fontSize = (item.fontSizeNormalized * minDim).coerceIn(18f, 54f)
    val textPaint = android.graphics.Paint().apply {
        color = if (item.style == MarkupTextStyle.OUTLINE) item.colorArgb else android.graphics.Color.WHITE
        textSize = fontSize
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, if (item.isBold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    }
    val textWidth = textPaint.measureText(item.text); val padH = fontSize * 0.5f; val padV = fontSize * 0.35f
    val pillRect = android.graphics.RectF(x - padH, y - fontSize - padV, x + textWidth + padH, y + padV)

    when (item.style) {
        MarkupTextStyle.BADGE -> {
            scope.drawContext.canvas.nativeCanvas.drawRoundRect(pillRect, 8f, 8f, android.graphics.Paint().apply { color = item.colorArgb; style = android.graphics.Paint.Style.FILL; setShadowLayer(6f, 1f, 2f, 0x77000000.toInt()) })
            scope.drawContext.canvas.nativeCanvas.drawText(item.text, x, y - padV * 0.2f, textPaint)
        }
        MarkupTextStyle.FROSTED -> {
            scope.drawContext.canvas.nativeCanvas.drawRoundRect(pillRect, 8f, 8f, android.graphics.Paint().apply { color = android.graphics.Color.BLACK; alpha = 200; style = android.graphics.Paint.Style.FILL })
            scope.drawContext.canvas.nativeCanvas.drawRoundRect(pillRect, 8f, 8f, android.graphics.Paint().apply { color = item.colorArgb; strokeWidth = 2.5f; style = android.graphics.Paint.Style.STROKE })
            scope.drawContext.canvas.nativeCanvas.drawText(item.text, x, y - padV * 0.2f, textPaint)
        }
        MarkupTextStyle.OUTLINE -> {
            scope.drawContext.canvas.nativeCanvas.drawText(item.text, x, y - padV * 0.2f, android.graphics.Paint().apply { color = android.graphics.Color.BLACK; textSize = fontSize; typeface = textPaint.typeface; style = android.graphics.Paint.Style.STROKE; strokeWidth = 5f })
            scope.drawContext.canvas.nativeCanvas.drawText(item.text, x, y - padV * 0.2f, textPaint)
        }
        MarkupTextStyle.TRANSPARENT -> {
            scope.drawContext.canvas.nativeCanvas.drawText(item.text, x, y - padV * 0.2f, textPaint)
        }
    }
}

private fun renderBlurRectPreview(scope: DrawScope, item: MarkupItem.BlurRect, width: Float, height: Float, blurredImageBitmap: androidx.compose.ui.graphics.ImageBitmap?) {
    val l = minOf(item.left, item.right) * width; val r = maxOf(item.left, item.right) * width
    val t = minOf(item.top, item.bottom) * height; val b = maxOf(item.top, item.bottom) * height

    if (blurredImageBitmap != null) {
        scope.clipRect(l, t, r, b) { scope.drawImage(blurredImageBitmap, dstSize = androidx.compose.ui.unit.IntSize(width.toInt(), height.toInt())) }
    } else {
        scope.drawRect(Color(0x88334455), Offset(l, t), Size(r - l, b - t))
    }
    scope.drawRect(Color.White.copy(alpha = 0.6f), Offset(l, t), Size(r - l, b - t), style = Stroke(2f))
}

private fun renderBlurPathPreview(
    scope: DrawScope, item: MarkupItem.BlurPath,
    width: Float, height: Float, minDim: Float,
    blurredImageBitmap: androidx.compose.ui.graphics.ImageBitmap?
) {
    if (item.points.size < 2) return
    val strokePx = (item.strokeWidthNormalized * minDim).coerceAtLeast(14f)

    // Build true 2D stroked outline contour using getFillPath
    val nativePath = AndroidPath()
    val p0 = item.points.first()
    nativePath.moveTo(p0.first * width, p0.second * height)
    for (i in 1 until item.points.size) {
        val prev = item.points[i - 1]
        val curr = item.points[i]
        val midX = (prev.first + curr.first) / 2f * width
        val midY = (prev.second + curr.second) / 2f * height
        nativePath.quadTo(prev.first * width, prev.second * height, midX, midY)
    }
    val last = item.points.last()
    nativePath.lineTo(last.first * width, last.second * height)

    val strokePaint = AndroidPaint().apply {
        style = AndroidPaint.Style.STROKE
        this.strokeWidth = strokePx
        strokeCap = AndroidPaint.Cap.ROUND
        strokeJoin = AndroidPaint.Join.ROUND
    }
    val filledRibbonPath = AndroidPath()
    strokePaint.getFillPath(nativePath, filledRibbonPath)

    if (blurredImageBitmap != null) {
        // Live Gaussian blur rendered strictly inside the finger-drawn brush stroke
        scope.clipPath(filledRibbonPath.asComposePath()) {
            scope.drawImage(blurredImageBitmap, dstSize = androidx.compose.ui.unit.IntSize(width.toInt(), height.toInt()))
        }
        scope.drawPath(nativePath.asComposePath(), Color.White.copy(alpha = 0.2f), style = Stroke(strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))
    } else {
        scope.drawPath(nativePath.asComposePath(), Color(0xAA334455), style = Stroke(strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private fun renderRectanglePreview(scope: DrawScope, item: MarkupItem.Rectangle, width: Float, height: Float, minDim: Float) {
    val l = minOf(item.left, item.right) * width; val r = maxOf(item.left, item.right) * width
    val t = minOf(item.top, item.bottom) * height; val b = maxOf(item.top, item.bottom) * height
    val strokePx = (item.strokeWidthNormalized * minDim).coerceAtLeast(3f)
    val cr = minDim * item.cornerRadius; val crO = CornerRadius(cr, cr)
    scope.drawRoundRect(Color(item.colorArgb), Offset(l, t), Size(r - l, b - t), crO, Stroke(strokePx))
}

private fun renderCirclePreview(scope: DrawScope, item: MarkupItem.Circle, width: Float, height: Float, minDim: Float) {
    val cx = item.centerX * width; val cy = item.centerY * height; val r = item.radius * minDim
    val strokePx = (item.strokeWidthNormalized * minDim).coerceAtLeast(3f)
    scope.drawCircle(Color(item.colorArgb), r, Offset(cx, cy), style = Stroke(strokePx))
}

private fun renderHighlighterPreview(scope: DrawScope, item: MarkupItem.Highlighter, width: Float, height: Float, minDim: Float) {
    if (item.points.size < 2) return
    val strokePx = (item.strokeWidthNormalized * minDim).coerceAtLeast(16f)
    val path = buildSmoothPreviewPath(item.points, width, height)
    scope.drawPath(path, Color(item.colorArgb).copy(alpha = item.opacity), style = Stroke(strokePx, cap = StrokeCap.Round))
}

private fun renderCalloutPreview(scope: DrawScope, item: MarkupItem.Callout, width: Float, height: Float, minDim: Float) {
    if (item.text.isBlank()) return
    val boxX = item.boxX * width; val boxY = item.boxY * height
    val anchorX = item.anchorX * width; val anchorY = item.anchorY * height
    val fontSize = (0.032f * minDim).coerceIn(18f, 54f)

    scope.drawLine(Color(item.colorArgb), Offset(boxX, boxY), Offset(anchorX, anchorY), strokeWidth = 3f)

    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE; textSize = fontSize
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
    }
    val textWidth = textPaint.measureText(item.text); val padH = fontSize * 0.5f; val padV = fontSize * 0.35f
    val pillRect = android.graphics.RectF(boxX - padH, boxY - fontSize - padV, boxX + textWidth + padH, boxY + padV)

    scope.drawContext.canvas.nativeCanvas.drawRoundRect(pillRect, 8f, 8f, android.graphics.Paint().apply { color = item.colorArgb; style = android.graphics.Paint.Style.FILL; setShadowLayer(6f, 1f, 2f, 0x77000000.toInt()) })
    scope.drawContext.canvas.nativeCanvas.drawText(item.text, boxX, boxY - padV * 0.2f, textPaint)
}

private fun buildSmoothPreviewPath(points: List<Pair<Float, Float>>, width: Float, height: Float): Path {
    val path = Path(); if (points.isEmpty()) return path
    val first = points.first(); path.moveTo(first.first * width, first.second * height)
    for (i in 1 until points.size) {
        val prev = points[i - 1]; val curr = points[i]
        path.quadraticTo(prev.first * width, prev.second * height, (prev.first + curr.first) / 2f * width, (prev.second + curr.second) / 2f * height)
    }
    path.lineTo(points.last().first * width, points.last().second * height)
    return path
}

private fun buildSmoothPreviewPathWithEnd(points: List<Pair<Float, Float>>, width: Float, height: Float, endX: Float, endY: Float): Path {
    val path = Path(); if (points.isEmpty()) return path
    val first = points.first(); path.moveTo(first.first * width, first.second * height)
    for (i in 1 until points.size - 1) {
        val prev = points[i - 1]; val curr = points[i]
        path.quadraticTo(prev.first * width, prev.second * height, (prev.first + curr.first) / 2f * width, (prev.second + curr.second) / 2f * height)
    }
    if (points.size > 2) {
        val secondLast = points[points.size - 2]
        path.quadraticTo(secondLast.first * width, secondLast.second * height, endX, endY)
    } else {
        path.lineTo(endX, endY)
    }
    return path
}
