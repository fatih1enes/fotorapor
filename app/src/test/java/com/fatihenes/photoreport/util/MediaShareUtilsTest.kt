package com.fatihenes.photoreport.util

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MediaShareUtilsTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `resolveMediaUri preserves content scheme without local file check`() {
        val contentPath = "content://media/external/images/media/999.jpg"
        val (uri, extension) = MediaShareUtils.resolveMediaUri(context, contentPath)
        
        assertEquals(Uri.parse(contentPath), uri)
        assertEquals("jpg", extension)
    }

    @Test
    fun `resolveMediaUri throws SecurityException on path traversal`() {
        val traversalPath = "file://${context.filesDir.absolutePath}/../../etc/passwd"
        assertThrows(SecurityException::class.java) {
            MediaShareUtils.resolveMediaUri(context, traversalPath)
        }
    }
}
