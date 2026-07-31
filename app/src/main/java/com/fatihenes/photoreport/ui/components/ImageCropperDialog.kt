package com.fatihenes.photoreport.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fatihenes.photoreport.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

@Composable
fun ImageCropperDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropSuccess: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // State for gestures
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // 1f = 1:1, 1.33f = 4:3, 1.77f = 16:9
    var aspectRatio by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            try {
                // OOM Önlemi: Resmi tam çözünürlükte değil, ekran boyutuna uygun şekilde (max 1024x1024) yükle
                val bmp = com.fatihenes.photoreport.util.ImageUtils.loadScaledBitmap(context, imageUri.toString(), 1024, 1024)
                if (bmp != null) {
                    originalBitmap = bmp
                    imageBitmap = bmp.asImageBitmap()
                }
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    DisposableEffect(originalBitmap) {
        onDispose {
            originalBitmap?.recycle()
        }
    }

    if (imageBitmap == null) {
        Dialog(onDismissRequest = onDismiss) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val canvasWidth = constraints.maxWidth.toFloat()
            val canvasHeight = constraints.maxHeight.toFloat()

            Box(modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = max(0.1f, scale * zoom)
                        val newOffset = offset + pan
                        offset = newOffset
                    }
                }
            ) {
            Canvas(modifier = Modifier.fillMaxSize()) {

                val imgWidth = imageBitmap!!.width.toFloat()
                val imgHeight = imageBitmap!!.height.toFloat()

                // Calculate base scale to fit image to screen (center crop style base)
                val baseScale = max(canvasWidth / imgWidth, canvasHeight / imgHeight)

                val finalScale = baseScale * scale

                // Calculate image drawing rect
                val imgDrawWidth = imgWidth * finalScale
                val imgDrawHeight = imgHeight * finalScale

                val imgLeft = (canvasWidth - imgDrawWidth) / 2f + offset.x
                val imgTop = (canvasHeight - imgDrawHeight) / 2f + offset.y

                // Draw image
                drawImage(
                    image = imageBitmap!!,
                    dstOffset = IntOffset(imgLeft.toInt(), imgTop.toInt()),
                    dstSize = IntSize(imgDrawWidth.toInt(), imgDrawHeight.toInt())
                )

                // The Crop Mask (Darkened area outside crop box)
                val cropBoxWidth = canvasWidth * 0.8f
                val cropBoxHeight = cropBoxWidth / aspectRatio
                val cropBoxLeft = (canvasWidth - cropBoxWidth) / 2f
                val cropBoxTop = (canvasHeight - cropBoxHeight) / 2f
                val cropRect = Rect(cropBoxLeft, cropBoxTop, cropBoxLeft + cropBoxWidth, cropBoxTop + cropBoxHeight)

                // Draw darkened overlay
                with(drawContext.canvas.nativeCanvas) {
                    val checkPoint = saveLayer(null, null)
                    drawRect(
                        color = Color.Black.copy(alpha = 0.7f),
                        topLeft = Offset.Zero,
                        size = size
                    )
                    // Clear the crop area
                    drawRect(
                        color = Color.Transparent,
                        topLeft = cropRect.topLeft,
                        size = cropRect.size,
                        blendMode = BlendMode.Clear
                    )
                    restoreToCount(checkPoint)
                }

                // Draw Grid inside the crop area
                val strokeWidth = 1.dp.toPx()
                val gridColor = Color.White.copy(alpha = 0.6f)

                // Border
                drawRect(
                    color = Color.White,
                    topLeft = cropRect.topLeft,
                    size = cropRect.size,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Vertical lines
                drawLine(
                    color = gridColor,
                    start = Offset(cropBoxLeft + cropBoxWidth / 3f, cropBoxTop),
                    end = Offset(cropBoxLeft + cropBoxWidth / 3f, cropBoxTop + cropBoxHeight),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = gridColor,
                    start = Offset(cropBoxLeft + 2f * cropBoxWidth / 3f, cropBoxTop),
                    end = Offset(cropBoxLeft + 2f * cropBoxWidth / 3f, cropBoxTop + cropBoxHeight),
                    strokeWidth = strokeWidth
                )

                // Horizontal lines
                drawLine(
                    color = gridColor,
                    start = Offset(cropBoxLeft, cropBoxTop + cropBoxHeight / 3f),
                    end = Offset(cropBoxLeft + cropBoxWidth, cropBoxTop + cropBoxHeight / 3f),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = gridColor,
                    start = Offset(cropBoxLeft, cropBoxTop + 2f * cropBoxHeight / 3f),
                    end = Offset(cropBoxLeft + cropBoxWidth, cropBoxTop + 2f * cropBoxHeight / 3f),
                    strokeWidth = strokeWidth
                )
            }

            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel_label), tint = Color.White)
                }

                Text(stringResource(R.string.crop_logo_title), color = Color.White, style = MaterialTheme.typography.titleMedium)

                IconButton(
                    onClick = {
                        originalBitmap?.let { obmp ->
                            val imgW = obmp.width.toFloat()
                            val imgH = obmp.height.toFloat()

                            val baseScale = max(canvasWidth / imgW, canvasHeight / imgH)
                            val finalScale = baseScale * scale

                            val imgDrawWidth = imgW * finalScale
                            val imgDrawHeight = imgH * finalScale

                            val imgLeft = (canvasWidth - imgDrawWidth) / 2f + offset.x
                            val imgTop = (canvasHeight - imgDrawHeight) / 2f + offset.y

                            val cropBoxWidth = canvasWidth * 0.8f
                            val cropBoxHeight = cropBoxWidth / aspectRatio
                            val cropBoxLeft = (canvasWidth - cropBoxWidth) / 2f
                            val cropBoxTop = (canvasHeight - cropBoxHeight) / 2f

                            val targetW = 512
                            val targetH = (512 / aspectRatio).toInt()

                            val finalSized = createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(finalSized)

                            // Transparent background is default for ARGB_8888

                            // Calculate transformation
                            val scaleFactor = targetW.toFloat() / cropBoxWidth
                            val drawX = imgLeft - cropBoxLeft
                            val drawY = imgTop - cropBoxTop

                            val matrix = android.graphics.Matrix()
                            matrix.postScale(finalScale * scaleFactor, finalScale * scaleFactor)
                            matrix.postTranslate(drawX * scaleFactor, drawY * scaleFactor)

                            // Use a paint with filtering for better downscaling quality
                            val paint = android.graphics.Paint().apply {
                                isFilterBitmap = true
                                isAntiAlias = true
                            }

                            canvas.drawBitmap(obmp, matrix, paint)

                            onCropSuccess(finalSized)
                        }
                    },
                    enabled = originalBitmap != null
                ) {
                    Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save_label), tint = if (originalBitmap != null) Color.White else Color.Gray)
                }
            }

            // Aspect Ratio Bottom Menu
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 32.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RatioButton("1:1", 1f, aspectRatio) { aspectRatio = 1f }
                RatioButton("4:3", 4f/3f, aspectRatio) { aspectRatio = 4f/3f }
                RatioButton("16:9", 16f/9f, aspectRatio) { aspectRatio = 16f/9f }
            }
            }
        }
    }
}

@Composable
private fun RatioButton(label: String, ratio: Float, currentRatio: Float, onClick: () -> Unit) {
    val isSelected = kotlin.math.abs(ratio - currentRatio) < 0.01f
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
        )
    ) {
        Text(label, fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal)
    }
}
