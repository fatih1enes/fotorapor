@file:Suppress("MagicNumber")
package com.fatihenes.photoreport.feature.project.ui.markup.toolbars

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fatihenes.photoreport.core.model.MarkupItem
import com.fatihenes.photoreport.core.ui.R

@Composable
fun SelectedItemToolbar(
    selectedItem: MarkupItem?,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = selectedItem != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        if (selectedItem == null) return@AnimatedVisibility

        Surface(
            color = Color(0xEE161B26),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit button for Text
                if (selectedItem is MarkupItem.TextCallout || selectedItem is MarkupItem.Callout) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.markup_action_edit), tint = Color.White, modifier = Modifier.size(17.dp))
                    }
                }

                // Duplicate
                IconButton(onClick = onDuplicate, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.markup_action_duplicate), tint = Color.White, modifier = Modifier.size(17.dp))
                }

                // Delete
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.markup_action_delete), tint = Color(0xFFFF5252), modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}
