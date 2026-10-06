package com.fatihenes.photoreport.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoMarkupModelTest {

    @Test
    fun testMarkupItemCreation() {
        val arrow = MarkupItem.Arrow(
            startX = 0.1f,
            startY = 0.2f,
            endX = 0.8f,
            endY = 0.9f,
            colorArgb = -65536, // Red
            strokeWidthNormalized = 0.008f
        )
        assertEquals(0.1f, arrow.startX, 0.001f)
        assertEquals(0.8f, arrow.endX, 0.001f)
        assertEquals(-65536, arrow.colorArgb)

        val fluidArrow = MarkupItem.Arrow(
            startX = 0.1f, startY = 0.2f, endX = 0.8f, endY = 0.9f,
            points = listOf(0.1f to 0.2f, 0.4f to 0.5f, 0.8f to 0.9f),
            colorArgb = -65536
        )
        assertEquals(3, fluidArrow.points.size)
        val movedFluidArrow = fluidArrow.translateBy(0.1f, 0.1f) as MarkupItem.Arrow
        assertEquals(0.2f, movedFluidArrow.points[0].first, 0.001f)
        assertEquals(0.3f, movedFluidArrow.points[0].second, 0.001f)

        val rect = MarkupItem.Rectangle(
            left = 0.1f,
            top = 0.1f,
            right = 0.5f,
            bottom = 0.5f,
            colorArgb = -1,
            strokeWidthNormalized = 0.008f
        )
        assertNotNull(rect)

        val circle = MarkupItem.Circle(
            centerX = 0.5f,
            centerY = 0.5f,
            radius = 0.2f,
            colorArgb = -16711936,
            strokeWidthNormalized = 0.008f
        )
        assertEquals(0.5f, circle.centerX, 0.001f)

        val line = MarkupItem.Line(
            startX = 0.0f,
            startY = 0.0f,
            endX = 1.0f,
            endY = 1.0f,
            colorArgb = -1,
            strokeWidthNormalized = 0.008f
        )
        assertEquals(1.0f, line.endX, 0.001f)

        val highlighter = MarkupItem.Highlighter(
            points = listOf(0.1f to 0.1f, 0.2f to 0.2f),
            colorArgb = -256,
            strokeWidthNormalized = 0.024f
        )
        assertEquals(2, highlighter.points.size)

        val text = MarkupItem.TextCallout(
            x = 0.4f,
            y = 0.4f,
            text = "Damage Area",
            colorArgb = -16711936,
            strokeWidthNormalized = 0.024f,
            style = MarkupTextStyle.FROSTED
        )
        assertEquals("Damage Area", text.text)
        assertEquals(MarkupTextStyle.FROSTED, text.style)

        val pin = MarkupItem.NumberedPin(
            x = 0.3f,
            y = 0.7f,
            number = 1,
            colorArgb = -65536
        )
        assertEquals(1, pin.number)

        val blur = MarkupItem.BlurRect(
            left = 0.2f,
            top = 0.3f,
            right = 0.6f,
            bottom = 0.7f
        )
        assertNotNull(blur)

        val callout = MarkupItem.Callout(
            boxX = 0.2f,
            boxY = 0.2f,
            text = "Main crack",
            anchorX = 0.4f,
            anchorY = 0.5f,
            colorArgb = -65536
        )
        assertEquals("Main crack", callout.text)
        assertEquals(0.4f, callout.anchorX, 0.001f)

        val measure = MarkupItem.Measurement(
            startX = 0.1f,
            startY = 0.1f,
            endX = 0.5f,
            endY = 0.5f,
            realDistance = 2.45f,
            unit = MeasurementUnit.M,
            colorArgb = -1
        )
        assertEquals("2.45 m", measure.formattedDistance())

        val issue = MarkupItem.IssueMarker(
            x = 0.5f,
            y = 0.5f,
            issueType = IssueType.ELECTRICAL,
            colorArgb = -256
        )
        assertEquals(IssueType.ELECTRICAL, issue.issueType)

        val blurPath = MarkupItem.BlurPath(
            points = listOf(0.1f to 0.2f, 0.3f to 0.4f),
            strokeWidthNormalized = 0.05f
        )
        assertEquals(2, blurPath.points.size)
        val movedBlurPath = blurPath.translateBy(0.1f, 0.1f) as MarkupItem.BlurPath
        assertEquals(0.2f, movedBlurPath.points[0].first, 0.001f)
        assertEquals(0.3f, movedBlurPath.points[0].second, 0.001f)
    }

    @Test
    fun testMarkupItemTransformations() {
        val rect = MarkupItem.Rectangle(
            left = 0.1f, top = 0.1f, right = 0.5f, bottom = 0.5f, colorArgb = -1
        )
        val moved = rect.translateBy(0.2f, 0.1f) as MarkupItem.Rectangle
        assertEquals(0.3f, moved.left, 0.001f)
        assertEquals(0.2f, moved.top, 0.001f)
        assertEquals(0.7f, moved.right, 0.001f)
        assertEquals(0.6f, moved.bottom, 0.001f)

        val rotated = rect.rotateBy(45f)
        assertEquals(45f, rotated.rotation, 0.001f)

        val scaled = rect.scaleBy(1.5f)
        assertEquals(1.5f, scaled.scale, 0.001f)

        val locked = rect.withLocked(true)
        assertTrue(locked.isLocked)

        val hidden = rect.withVisibility(false)
        assertFalse(hidden.isVisible)

        val opaque = rect.withOpacity(0.8f)
        assertEquals(0.8f, opaque.opacity, 0.001f)
    }

    @Test
    fun testPhotoEditorSessionAndDirtyChecking() {
        val initialSession = PhotoEditorSession.EMPTY
        assertTrue(initialSession.isClean)
        assertFalse(initialSession.isDirtyComparedTo(PhotoEditorSession.EMPTY))

        val modifiedSession = initialSession.copy(
            items = listOf(
                MarkupItem.Circle(centerX = 0.5f, centerY = 0.5f, radius = 0.1f, colorArgb = -1)
            )
        )
        assertTrue(modifiedSession.isDirtyComparedTo(initialSession))
        assertFalse(modifiedSession.isClean)

        val adjustedSession = initialSession.copy(
            adjustments = PhotoAdjustments(brightness = 0.2f)
        )
        assertTrue(adjustedSession.isDirtyComparedTo(initialSession))

        val cropSession = initialSession.copy(
            cropState = PhotoCropState(rotationDegrees = 90)
        )
        assertTrue(cropSession.isDirtyComparedTo(initialSession))
    }

    @Test
    fun testMarkupToolEnum() {
        val tools = MarkupTool.entries
        assertEquals(18, tools.size)
    }
}
