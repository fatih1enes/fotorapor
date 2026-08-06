package com.fatihenes.photoreport.core.ui.state

/**
 * Generic UI state wrapper for operations in feature modules.
 */
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
