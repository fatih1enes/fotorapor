@file:Suppress("MagicNumber")
package com.fatihenes.photoreport.feature.project.ui.markup.toolbars

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Filter1
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion
import com.fatihenes.photoreport.core.model.MarkupTool
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.feature.project.ui.markup.MarkupAccentGreen
import com.fatihenes.photoreport.feature.project.ui.markup.MarkupAccentOnGreen

data class PrimaryToolItem(
    val tool: MarkupTool,
    val labelRes: Int,
    val icon: ImageVector
)

val ApprovedMarkupTools = listOf(
    PrimaryToolItem(MarkupTool.FREEHAND, R.string.markup_tool_freehand, Icons.Default.Edit),
    PrimaryToolItem(MarkupTool.ARROW, R.string.markup_tool_arrow, Icons.AutoMirrored.Filled.TrendingFlat),
    PrimaryToolItem(MarkupTool.LINE, R.string.markup_tool_line, Icons.Default.HorizontalRule),
    PrimaryToolItem(MarkupTool.NUMBERED_PIN, R.string.markup_tool_pin, Icons.Default.Filter1),
    PrimaryToolItem(MarkupTool.TEXT, R.string.markup_tool_text, Icons.Default.TextFields),
    PrimaryToolItem(MarkupTool.BLUR, R.string.markup_tool_blur, Icons.Default.BlurOn),
    PrimaryToolItem(MarkupTool.CROP, R.string.markup_crop, Icons.Default.Crop)
)

@Composable
fun MarkupPrimaryToolbar(
    selectedTool: MarkupTool,
    onSelectTool: (MarkupTool) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xEE121620),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(0.5.dp, Color(0x33FFFFFF)),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ApprovedMarkupTools.forEach { toolItem ->
                val isSelected = selectedTool == toolItem.tool
                PrimaryToolButton(
                    item = toolItem,
                    isSelected = isSelected,
                    onClick = { onSelectTool(toolItem.tool) }
                )
            }
        }
    }
}

@Composable
private fun PrimaryToolButton(
    item: PrimaryToolItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = tween(FotoRaporMotion.DURATION_SHORT),
        label = "tool_scale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) MarkupAccentGreen else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                item.icon,
                contentDescription = stringResource(item.labelRes),
                tint = if (isSelected) MarkupAccentOnGreen else Color.White,
                modifier = Modifier.size(18.dp)
            )
            Text(
                stringResource(item.labelRes),
                color = if (isSelected) MarkupAccentOnGreen else Color.White,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
