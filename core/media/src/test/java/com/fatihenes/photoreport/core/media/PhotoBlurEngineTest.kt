package com.fatihenes.photoreport.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoBlurEngineTest {

    @Test
    fun testBoxBlurDiffusesSharpEdges() {
        val w = 50
        val h = 50
        val pixels = IntArray(w * h)

        // Left half black (0xFF000000), right half white (0xFFFFFFFF)
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        for (y in 0 until h) {
            for (x in 0 until w) {
                pixels[y * w + x] = if (x < 25) black else white
            }
        }

        // Before blur: pixel at x=24 is pure black, pixel at x=25 is pure white
        assertEquals(black, pixels[25 * w + 24])
        assertEquals(white, pixels[25 * w + 25])

        // Apply box blur with radius 5
        PhotoBlurEngine.boxBlur(pixels, w, h, 5)

        // After blur: edge pixels must be diffused into gray
        val edgeLeft = pixels[25 * w + 24]
        val edgeRight = pixels[25 * w + 25]

        assertNotEquals(black, edgeLeft)
        assertNotEquals(white, edgeRight)

        val redLeft = (edgeLeft ushr 16) and 0xFF
        val redRight = (edgeRight ushr 16) and 0xFF

        assertTrue("Left edge should be brightened (red=$redLeft)", redLeft > 20)
        assertTrue("Right edge should be darkened (red=$redRight)", redRight < 235)
    }

    @Test
    fun testThreePassBoxBlurSmoothsGradients() {
        val w = 60
        val h = 60
        val pixels = IntArray(w * h)

        // Checkerboard high-frequency pattern
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        for (y in 0 until h) {
            for (x in 0 until w) {
                pixels[y * w + x] = if ((x + y) % 2 == 0) black else white
            }
        }

        // 3 passes of box blur (Gaussian approximation)
        val radius = 6
        PhotoBlurEngine.boxBlur(pixels, w, h, radius)
        PhotoBlurEngine.boxBlur(pixels, w, h, radius)
        PhotoBlurEngine.boxBlur(pixels, w, h, radius)

        // Center pixel should be well-averaged around mid-gray (~128)
        val center = pixels[30 * w + 30]
        val r = (center ushr 16) and 0xFF
        val g = (center ushr 8) and 0xFF
        val b = center and 0xFF

        assertTrue("Red channel should be smoothed to middle gray (r=$r)", r in 110..145)
        assertTrue("Green channel should be smoothed to middle gray (g=$g)", g in 110..145)
        assertTrue("Blue channel should be smoothed to middle gray (b=$b)", b in 110..145)
    }

    @Test
    fun testBoxBlurPreservesAlphaChannel() {
        val w = 20
        val h = 20
        val pixels = IntArray(w * h) { 0xFF123456.toInt() }

        PhotoBlurEngine.boxBlur(pixels, w, h, 4)

        for (p in pixels) {
            val alpha = (p ushr 24) and 0xFF
            assertEquals("Alpha channel must remain fully opaque (255)", 255, alpha)
        }
    }
}
