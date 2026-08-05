package com.fatihenes.photoreport.util

import java.util.regex.Pattern

/**
 * Utility class for safe file name handling to prevent Path Traversal attacks
 * and ensure filesystem compatibility across different Android versions.
 */
object FileNameUtils {

    private val ILLEGAL_CHARACTERS = Pattern.compile("[\\\\/:*?\"<>|\\u0000-\\u001f\\u007f-\\u009f]")
    private val RESERVED_NAMES = hashSetOf(
        "CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    )

    /**
     * Sanitizes a string to be used safely as a filename.
     * Removes path traversal sequences, illegal characters, and ensures the name is not reserved.
     *
     * @param input The raw string (e.g., project name).
     * @param fallback The default name to use if the input becomes empty after sanitization.
     * @return A sanitized, safe filename string.
     */
    fun sanitize(input: String, fallback: String = "file"): String {
        if (input.isBlank()) return fallback

        // 1. Remove path traversal sequences (../ or ..\)
        var sanitized = input.replace("..", "").replace("/", "").replace("\\", "")

        // 2. Remove illegal filesystem characters
        sanitized = ILLEGAL_CHARACTERS.matcher(sanitized).replaceAll("_")

        // 3. Trim whitespace and dots at the end (can cause issues on some systems)
        sanitized = sanitized.trim().trimEnd('.')

        // 4. Ensure it's not a reserved system name
        if (RESERVED_NAMES.contains(sanitized.uppercase())) {
            sanitized = "_$sanitized"
        }

        // 5. Final check for empty result
        return sanitized.ifBlank { fallback }
    }
}
