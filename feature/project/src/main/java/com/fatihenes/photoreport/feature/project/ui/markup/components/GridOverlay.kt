package com.fatihenes.photoreport.feature.project.ui.markup.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class GridType {
    NONE,
    THIRDS,
    CROSSHAIR,
    SQUARE,
    SAFE_MARGIN
}

@Composable
fun GridOverlay(
    gridType: GridType,
    modifier: Modifier = Modifier
) {
    if (gridType == GridType.NONE) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val gridColor = Color.White.copy(alpha = 0.4f)
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 8.dp.toPx()), 0f)

        when (gridType) {
            GridType.THIRDS -> {
                // Rule of Thirds (2 horizontal, 2 vertical lines)
                val x1 = w / 3f
                val x2 = w * 2f / 3f
                val y1 = h / 3f
                val y2 = h * 2f / 3f

                drawLine(gridColor, Offset(x1, 0f), Offset(x1, h), strokeWidth = 1.dp.toPx(), pathEffect = dashEffect)
                drawLine(gridColor, Offset(x2, 0f), Offset(x2, h), strokeWidth = 1.dp.toPx(), pathEffect = dashEffect)
                drawLine(gridColor, Offset(0f, y1), Offset(w, y1), strokeWidth = 1.dp.toPx(), pathEffect = dashEffect)
                drawLine(gridColor, Offset(0f, y2), Offset(w, y2), strokeWidth = 1.dp.toPx(), pathEffect = dashEffect)
            }

            GridType.CROSSHAIR -> {
                // Center crosshair
                val cx = w / 2f
                val cy = h / 2f
                drawLine(gridColor, Offset(cx, 0f), Offset(cx, h), strokeWidth = 1.5.dp.toPx(), pathEffect = dashEffect)
                drawLine(gridColor, Offset(0f, cy), Offset(w, cy), strokeWidth = 1.5.dp.toPx(), pathEffect = dashEffect)
                drawCircle(gridColor, 12.dp.toPx(), Offset(cx, cy), style = Stroke(1.5.dp.toPx()))
            }

            GridType.SQUARE -> {
                // 6x6 square grid
                for (i in 1..5) {
                    val x = w * i / 6f
                    val y = h * i / 6f
                    drawLine(gridColor.copy(alpha = 0.25f), Offset(x, 0f), Offset(x, h), strokeWidth = 1.dp.toPx())
                    drawLine(gridColor.copy(alpha = 0.25f), Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
                }
            }

            GridType.SAFE_MARGIN -> {
                // 5% safe margin boundary
                val marginX = w * 0.05f
                val marginY = h * 0.05f
                drawRect(
                    gridColor,
                    topLeft = Offset(marginX, marginY),
                    size = Size(w - marginX * 2, h - marginY * 2),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = dashEffect)
                )
            }

            GridType.NONE -> Unit
        }
    }
}
