package com.fatihenes.photoreport.manager.pdf

/**
 * POJO for layout results to keep the helper testable in local JVM.
 */
data class PdfRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun width(): Float = right - left
    fun height(): Float = bottom - top
}

/**
 * Helper to calculate coordinates and dimensions for photos in the PDF report.
 * Supports adaptive layout switching between 1-column and 2-column based on aspect ratio.
 */
class AdaptivePdfLayoutHelper(
    private val maxPageHeight: Int = PdfTheme.PAGE_HEIGHT,
    private val pageMargin: Float = PdfTheme.MARGIN,
    private val pageFooterHeight: Float = PdfTheme.FOOTER_HEIGHT
) {

    private var currentY: Float = 0f
    private var currentColumn: Int = 0

    /**
     * Resets the layout state (e.g., when a new page starts).
     */
    fun reset(startY: Float) {
        currentY = startY
        currentColumn = 0
    }

    /**
     * Represents a calculated slot for a photo.
     */
    data class LayoutResult(
        val rect: PdfRect,
        val isNewPageRequired: Boolean,
        val nextY: Float
    )

    /**
     * Calculates where to place the next photo.
     * @param bitmapWidth The width of the bitmap to be placed.
     * @param bitmapHeight The height of the bitmap to be placed.
     * @return [LayoutResult] containing the position and if a page break is needed.
     */
    fun calculateSlot(bitmapWidth: Int, bitmapHeight: Int): LayoutResult {
        val aspectRatio = bitmapWidth.toFloat() / bitmapHeight.toFloat()
        val isLandscape = aspectRatio > PdfTheme.ASPECT_RATIO_LANDSCAPE_THRESHOLD

        val targetWidth: Float
        val targetHeight: Float
        val x: Float
        var isNewPage = false

        if (isLandscape) {
            // Force wrap if we are in the middle of a 2-column row
            if (currentColumn > 0) {
                currentY += (PdfTheme.IMAGE_HEIGHT + PdfTheme.GRID_SPACING)
                currentColumn = 0
            }

            targetWidth = PdfTheme.FULL_WIDTH_IMAGE_WIDTH
            // Scale height proportionally but cap it to avoid taking too much page space
            targetHeight = (targetWidth / aspectRatio).coerceAtMost(PdfTheme.MAX_LANDSCAPE_IMAGE_HEIGHT)
            x = pageMargin

            if (currentY + targetHeight > maxPageHeight - pageMargin - pageFooterHeight) {
                isNewPage = true
            }
        } else {
            // Standard 2-column logic
            targetWidth = PdfTheme.IMAGE_WIDTH
            targetHeight = PdfTheme.IMAGE_HEIGHT
            x = pageMargin + currentColumn * (targetWidth + PdfTheme.GRID_SPACING)

            if (currentY + targetHeight > maxPageHeight - pageMargin - pageFooterHeight) {
                isNewPage = true
                currentColumn = 0
            }
        }

        val finalRect = PdfRect(x, currentY, x + targetWidth, currentY + targetHeight)
        
        // Update state for next call
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
