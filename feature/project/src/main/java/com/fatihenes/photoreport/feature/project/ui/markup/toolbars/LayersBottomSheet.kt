@file:Suppress("MagicNumber")
package com.fatihenes.photoreport.feature.project.ui.markup.toolbars

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatihenes.photoreport.core.model.MarkupItem
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.feature.project.ui.markup.MarkupAccentGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayersBottomSheet(
    items: List<MarkupItem>,
    selectedItemId: String?,
    onSelectItem: (String) -> Unit,
    onToggleVisibility: (MarkupItem) -> Unit,
    onToggleLock: (MarkupItem) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onDeleteItem: (MarkupItem) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF161B26),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.markup_layers_title),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.acc_close), tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Display layers top-to-bottom (highest zIndex first)
            val reversedItems = items.sortedByDescending { it.zIndex }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(reversedItems, key = { _, item -> item.id }) { _, item ->
                    val isSelected = item.id == selectedItemId

                    Surface(
                        color = if (isSelected) Color(0x3300E676) else Color(0x22FFFFFF),
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MarkupAccentGreen) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectItem(item.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Color swatch
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(item.colorArgb))
                                        .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    getItemTitle(item),
                                    color = if (item.isVisible) Color.White else Color.White.copy(alpha = 0.4f),
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                // Move Up
                                IconButton(onClick = { onMoveUp(item.id) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }

                                // Move Down
                                IconButton(onClick = { onMoveDown(item.id) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }

                                // Visibility
                                IconButton(onClick = { onToggleVisibility(item) }, modifier = Modifier.size(28.dp)) {
                                    Icon(
                                        if (item.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = if (item.isVisible) Color.White else Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Lock
                                IconButton(onClick = { onToggleLock(item) }, modifier = Modifier.size(28.dp)) {
                                    Icon(
                                        if (item.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = if (item.isLocked) Color(0xFFFFD600) else Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Delete
                                IconButton(onClick = { onDeleteItem(item) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun getItemTitle(item: MarkupItem): String {
    return when (item) {
        is MarkupItem.Arrow -> "Arrow"
        is MarkupItem.Rectangle -> "Box"
        is MarkupItem.Circle -> "Circle"
        is MarkupItem.Line -> "Line"
        is MarkupItem.Freehand -> "Drawing (${item.brushType.name.lowercase()})"
        is MarkupItem.Highlighter -> "Highlight"
        is MarkupItem.TextCallout -> "\"${item.text.take(12)}\""
        is MarkupItem.Callout -> "Callout: \"${item.text.take(10)}\""
        is MarkupItem.NumberedPin -> "Pin #${item.number}"
        is MarkupItem.IssueMarker -> "Issue (${item.issueType.name})"
        is MarkupItem.StatusMarker -> "Status (${item.statusType.name})"
        is MarkupItem.Measurement -> "Measurement (${item.formattedDistance()})"
        is MarkupItem.BlurRect -> if (item.isMosaic) "Mosaic Rect" else "Blur Rect"
        is MarkupItem.BlurPath -> "Freehand Blur"
    }
}
