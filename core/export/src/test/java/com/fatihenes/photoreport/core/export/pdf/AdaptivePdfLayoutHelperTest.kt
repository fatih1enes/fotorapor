package com.fatihenes.photoreport.core.export.pdf

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AdaptivePdfLayoutHelperTest {

    private lateinit var helper: AdaptivePdfLayoutHelper
    private val pageHeight = 842
    private val margin = 40f
    private val footerHeight = 45f

    @Before
    fun setup() {
        helper = AdaptivePdfLayoutHelper(
            maxPageHeight = pageHeight,
            pageMargin = margin,
            pageFooterHeight = footerHeight
        )
    }

    @Test
    fun `calculateSlot for portrait photo returns 2-column position`() {
        helper.reset(margin)
        
        // Portrait (W=100, H=150) -> AspectRatio ~0.66
        val result = helper.calculateSlot(100, 150)
        
        assertFalse("Should not require new page", result.isNewPageRequired)
        assertEquals("X should be at left margin", margin, result.rect.left, 0.1f)
        assertEquals("Width should be standard image width", PdfTheme.IMAGE_WIDTH, result.rect.width(), 0.1f)
    }

    @Test
    fun `calculateSlot for landscape photo returns 1-column full width position`() {
        helper.reset(margin)
        
        // Landscape (W=200, H=100) -> AspectRatio 2.0
        val result = helper.calculateSlot(200, 100)
        
        assertFalse("Should not require new page", result.isNewPageRequired)
        assertEquals("X should be at left margin", margin, result.rect.left, 0.1f)
        assertEquals("Width should be full page width minus margins", PdfTheme.FULL_WIDTH_IMAGE_WIDTH, result.rect.width(), 0.1f)
        assertEquals("Column index should be reset to 0 after landscape", 0, helper.getCurrentColumn())
    }

    @Test
    fun `two portrait photos should be side by side`() {
        helper.reset(margin)
        
        val first = helper.calculateSlot(1000, 1500)
        helper.updateY(first.nextY)
        
        val second = helper.calculateSlot(1000, 1500)
        
        assertEquals("First photo should be in col 0", margin, first.rect.left, 0.1f)
        assertEquals("Second photo should be in col 1", margin + PdfTheme.IMAGE_WIDTH + PdfTheme.GRID_SPACING, second.rect.left, 0.1f)
        assertEquals("Y should be the same for both in same row", first.rect.top, second.rect.top, 0.1f)
    }

    @Test
    fun `landscape photo should trigger wrapping if middle of row`() {
        helper.reset(margin)
        
        // 1. Add one portrait
        helper.calculateSlot(1000, 1500)
        
        // 2. Add one landscape
        val result = helper.calculateSlot(1500, 1000)
        
        assertEquals("Landscape should start at col 0", margin, result.rect.left, 0.1f)
        assertTrue("Landscape should be lower than first row", result.rect.top > margin)
    }

    @Test
    fun `should trigger new page when Y limit exceeded`() {
        // Start near the bottom
        val startY = pageHeight - footerHeight - margin - 50f
        helper.reset(startY)
        
        val result = helper.calculateSlot(1000, 1000)
        
        assertTrue("Should trigger new page", result.isNewPageRequired)
    }
}
