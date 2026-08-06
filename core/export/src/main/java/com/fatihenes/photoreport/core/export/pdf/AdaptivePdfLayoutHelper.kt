package com.fatihenes.photoreport.core.export.pdf

data class PdfRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun width(): Float = right - left
    fun height(): Float = bottom - top
}

class AdaptivePdfLayoutHelper(
    private val maxPageHeight: Int = PdfTheme.PAGE_HEIGHT,
    private val pageMargin: Float = PdfTheme.MARGIN,
    private val pageFooterHeight: Float = PdfTheme.FOOTER_HEIGHT
) {

    private var currentY: Float = 0f
    private var currentColumn: Int = 0

    fun reset(startY: Float) {
        currentY = startY
        currentColumn = 0
    }

    data class LayoutResult(
        val rect: PdfRect,
        val isNewPageRequired: Boolean,
        val nextY: Float
    )

    fun calculateSlot(bitmapWidth: Int, bitmapHeight: Int): LayoutResult {
        val aspectRatio = bitmapWidth.toFloat() / bitmapHeight.toFloat()
        val isLandscape = aspectRatio > PdfTheme.ASPECT_RATIO_LANDSCAPE_THRESHOLD

        val targetWidth: Float
        val targetHeight: Float
        val x: Float
        var isNewPage = false

        if (isLandscape) {
            if (currentColumn > 0) {
                currentY += (PdfTheme.IMAGE_HEIGHT + PdfTheme.GRID_SPACING)
                currentColumn = 0
            }

            targetWidth = PdfTheme.FULL_WIDTH_IMAGE_WIDTH
            targetHeight = (targetWidth / aspectRatio).coerceAtMost(PdfTheme.MAX_LANDSCAPE_IMAGE_HEIGHT)
            x = pageMargin

            if (currentY + targetHeight > maxPageHeight - pageMargin - pageFooterHeight) {
                isNewPage = true
            }
        } else {
            targetWidth = PdfTheme.IMAGE_WIDTH
            targetHeight = PdfTheme.IMAGE_HEIGHT
            x = pageMargin + currentColumn * (targetWidth + PdfTheme.GRID_SPACING)

            if (currentY + targetHeight > maxPageHeight - pageMargin - pageFooterHeight) {
                isNewPage = true
                currentColumn = 0
            }
        }

        val finalRect = PdfRect(x, currentY, x + targetWidth, currentY + targetHeight)
        
        var nextY = currentY
        if (isLandscape) {
            nextY += (targetHeight + PdfTheme.GRID_SPACING)
            currentColumn = 0
        } else {
            currentColumn++
            if (currentColumn >= PdfTheme.COLUMNS) {
                currentColumn = 0
                nextY += (targetHeight + PdfTheme.GRID_SPACING)
            }
        }
        
        return LayoutResult(finalRect, isNewPage, nextY)
    }

    fun updateY(newY: Float) {
        currentY = newY
    }
    
    fun getCurrentY(): Float = currentY
    fun getCurrentColumn(): Int = currentColumn
}
