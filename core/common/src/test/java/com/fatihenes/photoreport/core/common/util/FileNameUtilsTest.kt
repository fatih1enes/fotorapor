package com.fatihenes.photoreport.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FileNameUtilsTest {

    @Test
    fun sanitize_emptyInput_returnsFallback() {
        assertEquals("file", FileNameUtils.sanitize(""))
        assertEquals("default", FileNameUtils.sanitize("  ", "default"))
    }

    @Test
    fun sanitize_removesPathTraversal() {
        assertEquals("testfile", FileNameUtils.sanitize("../../test/file"))
        assertEquals("secret", FileNameUtils.sanitize("..\\..\\secret"))
    }

    @Test
    fun `sanitize_replacesIllegalCharacters`() {
        assertEquals("my_file", FileNameUtils.sanitize("my:file"))
        assertEquals("abc_d_e", FileNameUtils.sanitize("a/b\\c?d*e"))
        assertEquals("report_2024_", FileNameUtils.sanitize("report<2024>"))
    }

    @Test
    fun sanitize_trimsWhitespaceAndTrailingDots() {
        assertEquals("my file", FileNameUtils.sanitize("  my file  "))
        assertEquals("report", FileNameUtils.sanitize("report..."))
    }

    @Test
    fun `sanitize_handlesReservedNames`() {
        assertEquals("_CON", FileNameUtils.sanitize("CON"))
        assertEquals("_prn", FileNameUtils.sanitize("prn"))
        assertEquals("_LPT1", FileNameUtils.sanitize("LPT1"))
    }

    @Test
    fun sanitize_complexInput() {
        val input = "  My Project: 2024/05..  "
        // 1. "  My Project: 202405..  " (remove /)
        // 2. "  My Project_ 202405..  " (replace :)
        // 3. "My Project_ 202405" (trim and trimEnd .)
        assertEquals("My Project_ 202405", FileNameUtils.sanitize(input))
    }
}
