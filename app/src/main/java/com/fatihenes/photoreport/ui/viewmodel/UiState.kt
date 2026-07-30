package com.fatihenes.photoreport.ui.viewmodel

/**
 * Generic UI state wrapper for operations.
 * Extracted from AppViewModel for reuse and SRP compliance.
 */
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
