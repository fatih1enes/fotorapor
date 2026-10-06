package com.fatihenes.photoreport.core.media.geometry

import com.fatihenes.photoreport.core.model.MarkupItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkupSnapEngineTest {

    @Test
    fun testSnapToCenterAxes() {
        val nearCenter = NormalizedPoint(0.505f, 0.495f)
        val bounds = NormalizedRect(0.455f, 0.445f, 0.555f, 0.545f)

        val result = MarkupSnapEngine.snapPosition(
            targetCenter = nearCenter,
            targetBounds = bounds,
            otherItems = emptyList(),
            canvasZoom = 1f
        )

        assertTrue(result.didSnapX)
        assertTrue(result.didSnapY)
        assertEquals(0.5f, result.snappedX, 0.0001f)
        assertEquals(0.5f, result.snappedY, 0.0001f)
        assertEquals(2, result.guideLines.size)
    }

    @Test
    fun testSnapToOtherItemCenter() {
        val other = MarkupItem.Circle(centerX = 0.3f, centerY = 0.7f, radius = 0.1f, colorArgb = -1)
        val movingCenter = NormalizedPoint(0.308f, 0.2f)
        val bounds = NormalizedRect(0.258f, 0.15f, 0.358f, 0.25f)

        val result = MarkupSnapEngine.snapPosition(
            targetCenter = movingCenter,
            targetBounds = bounds,
            otherItems = listOf(other),
            canvasZoom = 1f
        )

        assertTrue(result.didSnapX)
        assertEquals(0.3f, result.snappedX, 0.0001f)
    }

    @Test
    fun testRotationSnapping() {
        // Near 90 degrees
        val snap90 = MarkupSnapEngine.snapRotation(88.5f)
        assertTrue(snap90.didSnap)
        assertEquals(90f, snap90.snappedValue, 0.001f)

        // Near 45 degrees
        val snap45 = MarkupSnapEngine.snapRotation(46.0f)
        assertTrue(snap45.didSnap)
        assertEquals(45f, snap45.snappedValue, 0.001f)

        // Far from any snap angle (e.g. 23 degrees)
        val snap23 = MarkupSnapEngine.snapRotation(23.0f)
        assertFalse(snap23.didSnap)
        assertEquals(23.0f, snap23.snappedValue, 0.001f)
    }
}
