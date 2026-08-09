package com.fatihenes.photoreport.core.export.pdf

data class PdfRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun width(): Float = right - left
    fun height(): Float = bottom - top
}

class AdaptivePdfLayoutHelper(
    private val maxPageHeight: Int = PdfTheme.PAGE_HEIGHT,
    private val pageMargin: Float = PdfTheme.MARGIN,
    private val pageFooterHeight: Float = PdfTheme.FOOTER_HEIGHT,
) {

    var currentY: Float = 0f
        private set
    var currentColumn: Int = 0
        private set

    private val maxYLimit: Float
        get() = maxPageHeight - pageMargin - pageFooterHeight

    fun reset(startY: Float) {
        currentY = startY
        currentColumn = 0
    }

    fun updateY(newY: Float) {
        currentY = newY
    }

    fun flushRow() {
        if (currentColumn > 0) {
            currentY += (PdfTheme.IMAGE_HEIGHT + PdfTheme.GRID_SPACING)
            currentColumn = 0
        }
    }

    fun hasSpaceFor(height: Float): Boolean {
        var testY = currentY
        if (currentColumn > 0) {
            testY += (PdfTheme.IMAGE_HEIGHT + PdfTheme.GRID_SPACING)
        }
        return (testY + height) <= maxYLimit
    }

    data class LayoutResult(
        val rect: PdfRect,
        val isNewPageRequired: Boolean,
        val nextY: Float
    )

    companion object {
        private const val FALLBACK_DIMENSION = 1000f
    }

    fun calculateSlot(bitmapWidth: Int, bitmapHeight: Int): LayoutResult {
        val safeW = if (bitmapWidth > 0) bitmapWidth.toFloat() else FALLBACK_DIMENSION
        val safeH = if (bitmapHeight > 0) bitmapHeight.toFloat() else FALLBACK_DIMENSION
        val aspectRatio = safeW / safeH
        val isLandscape = aspectRatio > PdfTheme.ASPECT_RATIO_LANDSCAPE_THRESHOLD

        var slotY = currentY
        var slotCol = currentColumn

        if (isLandscape && slotCol > 0) {
            slotY += (PdfTheme.IMAGE_HEIGHT + PdfTheme.GRID_SPACING)
            slotCol = 0
        }

        val targetWidth: Float
        val targetHeight: Float
        val x: Float

        if (isLandscape) {
            targetWidth = PdfTheme.FULL_WIDTH_IMAGE_WIDTH
            targetHeight = (targetWidth / aspectRatio).coerceAtMost(PdfTheme.MAX_LANDSCAPE_IMAGE_HEIGHT)
            x = pageMargin
        } else {
            targetWidth = PdfTheme.IMAGE_WIDTH
            targetHeight = PdfTheme.IMAGE_HEIGHT
            x = pageMargin + slotCol * (targetWidth + PdfTheme.GRID_SPACING)
        }

        if ((slotY + targetHeight) > maxYLimit) {
            return LayoutResult(
                rect = PdfRect(pageMargin, slotY, pageMargin + targetWidth, slotY + targetHeight),
                isNewPageRequired = true,
                nextY = slotY
            )
        }

        val rect = PdfRect(x, slotY, x + targetWidth, slotY + targetHeight)
        var nextY = slotY
        var nextCol = slotCol

        if (isLandscape) {
            nextY = slotY + targetHeight + PdfTheme.GRID_SPACING
            nextCol = 0
        } else {
            nextCol++
            if (nextCol >= PdfTheme.COLUMNS) {
                nextCol = 0
                nextY = slotY + targetHeight + PdfTheme.GRID_SPACING
            }
        }

        currentY = nextY
        currentColumn = nextCol

        return LayoutResult(rect, false, nextY)
    }
}

