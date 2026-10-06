package com.fatihenes.photoreport.feature.project.ui.markup.history

import com.fatihenes.photoreport.core.model.MarkupItem
import com.fatihenes.photoreport.core.model.PhotoAdjustments
import com.fatihenes.photoreport.core.model.PhotoEditorSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorHistoryManagerTest {

    @Test
    fun testAddItemAndUndoRedo() {
        val manager = EditorHistoryManager()
        assertFalse(manager.canUndo)
        assertFalse(manager.canRedo)
        assertFalse(manager.isDirty)

        val item1 = MarkupItem.Rectangle(left = 0.1f, top = 0.1f, right = 0.4f, bottom = 0.4f, colorArgb = -1)
        manager.execute(EditorCommand.AddItem(item1))

        assertTrue(manager.canUndo)
        assertFalse(manager.canRedo)
        assertTrue(manager.isDirty)
        assertEquals(1, manager.currentSession.items.size)

        // Undo
        val undone = manager.undo()
        assertEquals(0, undone?.items?.size)
        assertFalse(manager.canUndo)
        assertTrue(manager.canRedo)
        assertFalse(manager.isDirty) // matches initial empty state

        // Redo
        val redone = manager.redo()
        assertEquals(1, redone?.items?.size)
        assertTrue(manager.canUndo)
        assertFalse(manager.canRedo)
        assertTrue(manager.isDirty)
    }

    @Test
    fun testMarkAsSavedResetsDirtyState() {
        val manager = EditorHistoryManager()
        val item = MarkupItem.Circle(centerX = 0.5f, centerY = 0.5f, radius = 0.2f, colorArgb = -1)
        manager.execute(EditorCommand.AddItem(item))

        assertTrue(manager.isDirty)
        manager.markAsSaved()
        assertFalse(manager.isDirty)

        // Modify adjustment
        manager.execute(EditorCommand.UpdateAdjustments(PhotoAdjustments.DEFAULT, PhotoAdjustments(brightness = 0.3f)))
        assertTrue(manager.isDirty)

        // Undo back to saved state
        manager.undo()
        assertFalse(manager.isDirty)
    }

    @Test
    fun testRedoInvalidationOnNewAction() {
        val manager = EditorHistoryManager()
        val item1 = MarkupItem.Line(startX = 0f, startY = 0f, endX = 1f, endY = 1f, colorArgb = -1)
        val item2 = MarkupItem.Circle(centerX = 0.5f, centerY = 0.5f, radius = 0.1f, colorArgb = -1)

        manager.execute(EditorCommand.AddItem(item1))
        manager.undo()
        assertTrue(manager.canRedo)

        // New command branches history, invalidating redo
        manager.execute(EditorCommand.AddItem(item2))
        assertFalse(manager.canRedo)
        assertEquals(1, manager.currentSession.items.size)
        assertEquals(item2.id, manager.currentSession.items[0].id)
    }

    @Test
    fun testMaxHistoryBoundedCapacity() {
        val manager = EditorHistoryManager(maxHistorySize = 5)

        for (i in 1..10) {
            val item = MarkupItem.NumberedPin(x = i * 0.05f, y = 0.5f, number = i, colorArgb = -1)
            manager.execute(EditorCommand.AddItem(item))
        }

        assertEquals(10, manager.currentSession.items.size)

        // Can only undo 5 times
        var undos = 0
        while (manager.canUndo) {
            manager.undo()
            undos++
        }
        assertEquals(5, undos)
        assertEquals(5, manager.currentSession.items.size)
    }
}
