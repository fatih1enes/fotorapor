package com.fatihenes.photoreport.feature.project.ui

import androidx.compose.runtime.Composable
import com.fatihenes.photoreport.core.model.MarkupItem
import com.fatihenes.photoreport.core.model.Photo
import com.fatihenes.photoreport.feature.project.ui.markup.PhotoMarkupScreen

/**
 * Backward compatibility adapter for PhotoMarkupDialog, delegating to the modular PhotoMarkupScreen.
 */
@Composable
@Suppress("UnusedParameter")
fun PhotoMarkupDialog(
    photo: Photo,
    onDismiss: () -> Unit,
    onSaveMarkups: (List<MarkupItem>) -> Unit = { }
) {
    PhotoMarkupScreen(
        photo = photo,
        onDismiss = onDismiss,
        onSaveComplete = { onDismiss() },
        onSaveError = { },
        onContentRefresh = { }
    )
}
