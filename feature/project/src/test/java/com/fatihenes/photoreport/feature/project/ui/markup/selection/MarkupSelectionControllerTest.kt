package com.fatihenes.photoreport.feature.project.ui.markup.selection

import com.fatihenes.photoreport.core.media.geometry.TransformHandle
import com.fatihenes.photoreport.core.model.MarkupItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkupSelectionControllerTest {

    @Test
    fun testHandlePositionsCalculation() {
        val rect = MarkupItem.Rectangle(
            left = 0.2f, top = 0.2f, right = 0.6f, bottom = 0.6f, colorArgb = -1
        )
        val handles = MarkupSelectionController.getHandlePositions(rect)

        assertEquals(9, handles.size) // 8 bbox handles + 1 rotation handle
        assertTrue(handles.any { it.handle == TransformHandle.TOP_LEFT })
        assertTrue(handles.any { it.handle == TransformHandle.BOTTOM_RIGHT })
        assertTrue(handles.any { it.handle == TransformHandle.ROTATION })
    }

    @Test
    fun testLayerReordering() {
        val item1 = MarkupItem.Circle(centerX = 0.1f, centerY = 0.1f, radius = 0.1f, colorArgb = -1, zIndex = 0)
        val item2 = MarkupItem.Rectangle(left = 0.2f, top = 0.2f, right = 0.4f, bottom = 0.4f, colorArgb = -1, zIndex = 1)
        val item3 = MarkupItem.Line(startX = 0.5f, startY = 0.5f, endX = 0.8f, endY = 0.8f, colorArgb = -1, zIndex = 2)

        val items = listOf(item1, item2, item3)

        // Bring item1 forward (should swap with item2)
        val fwd = MarkupSelectionController.bringForward(items, item1.id)
        assertEquals(item2.id, fwd[0].id)
        assertEquals(item1.id, fwd[1].id)
        assertEquals(item3.id, fwd[2].id)

        // Bring item1 to front (should move to end)
        val front = MarkupSelectionController.bringToFront(items, item1.id)
        assertEquals(item1.id, front.last().id)

        // Send item3 backward (should swap with item2)
        val back = MarkupSelectionController.sendBackward(items, item3.id)
        assertEquals(item3.id, back[1].id)
        assertEquals(item2.id, back[2].id)
    }

    @Test
    fun testDuplicationProducesNewIdAndOffset() {
        val original = MarkupItem.Rectangle(
            left = 0.2f, top = 0.2f, right = 0.5f, bottom = 0.5f, colorArgb = -65536
        )
        val duplicated = MarkupSelectionController.duplicate(original) as MarkupItem.Rectangle

        assertNotEquals(original.id, duplicated.id)
        assertEquals(-65536, duplicated.colorArgb)
        assertTrue(duplicated.left > original.left)
        assertTrue(duplicated.top > original.top)
    }
}
