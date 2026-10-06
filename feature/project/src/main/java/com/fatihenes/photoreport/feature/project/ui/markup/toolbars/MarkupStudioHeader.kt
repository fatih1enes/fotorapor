@file:Suppress("MagicNumber")
package com.fatihenes.photoreport.feature.project.ui.markup.toolbars

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.feature.project.ui.markup.MarkupAccentGreen
import com.fatihenes.photoreport.feature.project.ui.markup.MarkupAccentOnGreen
import com.fatihenes.photoreport.feature.project.ui.markup.MarkupCanvasState

@Composable
fun MarkupStudioHeader(
    canvasState: MarkupCanvasState,
    isSaving: Boolean,
    onClose: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(colors = listOf(Color(0xEE0A0E17), Color(0x660A0E17), Color.Transparent)))
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Close Button
            HeaderCircleButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.acc_close), tint = Color.White, modifier = Modifier.size(20.dp))
            }

            // Undo / Redo Actions
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canvasState.canUndo) {
                    HeaderCircleButton(onClick = onUndo) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.markup_undo), tint = Color.White, modifier = Modifier.size(19.dp))
                    }
                }
                if (canvasState.canRedo) {
                    HeaderCircleButton(onClick = onRedo) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = stringResource(R.string.markup_redo), tint = Color.White, modifier = Modifier.size(19.dp))
                    }
                }
            }

            // Save Button
            Button(
                onClick = onSave,
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = MarkupAccentGreen, contentColor = MarkupAccentOnGreen),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MarkupAccentOnGreen, strokeWidth = 2.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.markup_save), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCircleButton(
    onClick: () -> Unit,
    backgroundColor: Color = Color(0x661E232F),
    content: @Composable () -> Unit
) {
    Surface(shape = CircleShape, color = backgroundColor, modifier = Modifier.size(36.dp)) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) { content() }
    }
}
