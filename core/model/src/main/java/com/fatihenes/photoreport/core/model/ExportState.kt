package com.fatihenes.photoreport.core.model

import android.net.Uri

sealed interface ExportState {
    data object Loading : ExportState
    data class Success(val uri: Uri) : ExportState
    data class Error(val message: String) : ExportState
}
