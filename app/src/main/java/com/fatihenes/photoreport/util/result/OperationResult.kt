package com.fatihenes.photoreport.util.result

/**
 * A discriminated union that encapsulates a successful outcome with a value of type [T]
 * or a failure with an arbitrary [Throwable] exception.
 *
 * This follows Clean Architecture and SOLID principles by ensuring that domain errors
 * are explicitly handled by the UI layer rather than crashing the application or being swallowed.
 */
sealed class OperationResult<out T> {
    /**
     * Represents a successful outcome.
     *
     * @property data The encapsulated data of type [T].
     */
    data class Success<out T>(val data: T) : OperationResult<T>()

    /**
     * Represents a failed outcome.
     *
     * @property error The exception that caused the failure.
     * @property message An optional, user-friendly error message.
     */
    data class Error(val error: Throwable, val message: String? = null) : OperationResult<Nothing>()

    /**
     * Represents a loading or in-progress state, useful for long-running operations like Backup or Export.
     *
     * @property progress Optional progress percentage (0-100).
     */
    data class Loading(val progress: Int? = null) : OperationResult<Nothing>()
}
