package com.fatihenes.photoreport.core.media.geometry

import com.fatihenes.photoreport.core.model.MarkupItem
import com.fatihenes.photoreport.core.model.MarkupTextStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkupGeometryTest {

    @Test
    fun testPointToSegmentDistance() {
        // Point exactly on segment
        val d1 = MarkupGeometry.distToSegment(0.5f, 0.5f, 0.0f, 0.5f, 1.0f, 0.5f)
        assertEquals(0.0f, d1, 0.001f)

        // Point perpendicular to segment
        val d2 = MarkupGeometry.distToSegment(0.5f, 0.7f, 0.0f, 0.5f, 1.0f, 0.5f)
        assertEquals(0.2f, d2, 0.001f)

        // Point beyond segment endpoints (clamped to end)
        val d3 = MarkupGeometry.distToSegment(1.2f, 0.5f, 0.0f, 0.5f, 1.0f, 0.5f)
        assertEquals(0.2f, d3, 0.001f)
    }

    @Test
    fun testRectangleHitTesting() {
        val rect = MarkupItem.Rectangle(
            left = 0.2f, top = 0.2f, right = 0.6f, bottom = 0.6f, colorArgb = -1
        )

        // Inside
        assertTrue(MarkupGeometry.hitTest(rect, 0.4f, 0.4f))
        assertTrue(MarkupGeometry.hitTest(rect, 0.2f, 0.2f))

        // Outside
        assertFalse(MarkupGeometry.hitTest(rect, 0.1f, 0.1f))
        assertFalse(MarkupGeometry.hitTest(rect, 0.9f, 0.9f))
    }

    @Test
    fun testRotatedHitTesting() {
        // Rectangle centered at (0.5, 0.5), rotated 45 degrees
        val rect = MarkupItem.Rectangle(
            left = 0.4f, top = 0.4f, right = 0.6f, bottom = 0.6f,
            colorArgb = -1, rotation = 45f
        )

        // Center should hit
        assertTrue(MarkupGeometry.hitTest(rect, 0.5f, 0.5f))

        // Corner rotated by 45 degrees: top edge was (0.5, 0.4), rotated goes to (0.5, 0.36) approx
        assertTrue(MarkupGeometry.hitTest(rect, 0.5f, 0.36f))

        // Original unrotated corner (0.4, 0.4) is now outside the rotated diamond
        assertFalse(MarkupGeometry.hitTest(rect, 0.35f, 0.35f, canvasZoom = 2f))
    }

    @Test
    fun testCircleHitTesting() {
        val circle = MarkupItem.Circle(
            centerX = 0.5f, centerY = 0.5f, radius = 0.2f, colorArgb = -1
        )

        assertTrue(MarkupGeometry.hitTest(circle, 0.5f, 0.5f))
        assertTrue(MarkupGeometry.hitTest(circle, 0.65f, 0.5f)) // inside radius
        assertFalse(MarkupGeometry.hitTest(circle, 0.85f, 0.5f)) // outside radius
    }

    @Test
    fun testTextAndCalloutHitTesting() {
        val text = MarkupItem.TextCallout(
            x = 0.5f, y = 0.5f, text = "Crack Notice", colorArgb = -1, style = MarkupTextStyle.BADGE
        )
        assertTrue(MarkupGeometry.hitTest(text, 0.5f, 0.5f))
        assertFalse(MarkupGeometry.hitTest(text, 0.9f, 0.9f))

        val callout = MarkupItem.Callout(
            boxX = 0.3f, boxY = 0.3f, text = "Defect", anchorX = 0.6f, anchorY = 0.6f, colorArgb = -1
        )
        // Hit in box
        assertTrue(MarkupGeometry.hitTest(callout, 0.3f, 0.3f))
        // Hit on leader line
        assertTrue(MarkupGeometry.hitTest(callout, 0.45f, 0.45f))
        // Hit outside
        assertFalse(MarkupGeometry.hitTest(callout, 0.1f, 0.9f))
    }

    @Test
    fun testTopmostHitItemZIndexOrder() {
        val item1 = MarkupItem.Circle(centerX = 0.5f, centerY = 0.5f, radius = 0.3f, colorArgb = -1, zIndex = 0)
        val item2 = MarkupItem.Rectangle(left = 0.4f, top = 0.4f, right = 0.6f, bottom = 0.6f, colorArgb = -1, zIndex = 1)

        val hit = MarkupGeometry.findTopmostHitItem(listOf(item1, item2), 0.5f, 0.5f)
        assertEquals(item2.id, hit?.id)
    }

    @Test
    fun testBlurPathHitTesting() {
        val blurPath = MarkupItem.BlurPath(
            points = listOf(0.2f to 0.2f, 0.5f to 0.5f, 0.8f to 0.8f),
            strokeWidthNormalized = 0.05f
        )
        assertTrue(MarkupGeometry.hitTest(blurPath, 0.5f, 0.5f))
        assertFalse(MarkupGeometry.hitTest(blurPath, 0.2f, 0.8f))
    }

    @Test
    fun testFluidArrowHitTesting() {
        val fluidArrow = MarkupItem.Arrow(
            startX = 0.2f, startY = 0.2f, endX = 0.8f, endY = 0.8f,
            points = listOf(0.2f to 0.2f, 0.5f to 0.5f, 0.8f to 0.8f),
            colorArgb = -65536
        )
        assertTrue(MarkupGeometry.hitTest(fluidArrow, 0.5f, 0.5f))
        assertFalse(MarkupGeometry.hitTest(fluidArrow, 0.1f, 0.9f))
    }
}
