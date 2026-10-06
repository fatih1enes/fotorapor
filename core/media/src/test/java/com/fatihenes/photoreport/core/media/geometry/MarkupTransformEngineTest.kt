package com.fatihenes.photoreport.core.media.geometry

import com.fatihenes.photoreport.core.model.MarkupItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkupTransformEngineTest {

    @Test
    fun testResizeRectangleFromBottomRightHandle() {
        val rect = MarkupItem.Rectangle(
            left = 0.2f, top = 0.2f, right = 0.5f, bottom = 0.5f, colorArgb = -1
        )

        val resized = MarkupTransformEngine.resizeItem(
            item = rect,
            handle = TransformHandle.BOTTOM_RIGHT,
            dx = 0.1f,
            dy = 0.1f
        ) as MarkupItem.Rectangle

        assertEquals(0.2f, resized.left, 0.001f)
        assertEquals(0.2f, resized.top, 0.001f)
        assertEquals(0.6f, resized.right, 0.001f)
        assertEquals(0.6f, resized.bottom, 0.001f)
    }

    @Test
    fun testResizeCircleFromHandle() {
        val circle = MarkupItem.Circle(
            centerX = 0.5f, centerY = 0.5f, radius = 0.1f, colorArgb = -1
        )

        val resized = MarkupTransformEngine.resizeItem(
            item = circle,
            handle = TransformHandle.RIGHT,
            dx = 0.05f,
            dy = 0.05f
        ) as MarkupItem.Circle

        assertEquals(0.15f, resized.radius, 0.001f)
    }

    @Test
    fun testRotationCalculation() {
        val center = NormalizedPoint(0.5f, 0.5f)

        // Point directly to the right of center (90 degrees clockwise from top)
        val rotRight = MarkupTransformEngine.calculateRotation(center, 0.8f, 0.5f, snapEnabled = true)
        assertEquals(90f, rotRight, 0.1f)

        // Point directly below center (180 degrees from top)
        val rotBottom = MarkupTransformEngine.calculateRotation(center, 0.5f, 0.8f, snapEnabled = true)
        assertEquals(180f, rotBottom, 0.1f)
    }
}
