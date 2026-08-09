package com.fatihenes.photoreport.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens

@Suppress("FunctionNaming")
@Composable
fun <T> PillSegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    itemTitle: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(FotoRaporTokens.RadiusM))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(FotoRaporTokens.RadiusM),
            )
            .padding(4.dp),
    ) {
        val widthPerItem = (maxWidth - 8.dp) / items.size.coerceAtLeast(1)
        val indicatorOffset by animateDpAsState(
            targetValue = widthPerItem * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "pill_offset",
        )

        PillIndicator(
            modifier = Modifier
                .offset { IntOffset(x = indicatorOffset.roundToPx(), y = 0) }
                .width(widthPerItem),
        )

        PillLabels(
            items = items,
            selectedItem = selectedItem,
            onItemSelected = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onItemSelected(it)
            },
            itemTitle = itemTitle,
        )
    }
}

@Composable
private fun PillIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(FotoRaporTokens.RadiusS))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(FotoRaporTokens.RadiusS),
            ),
    )
}

@Composable
private fun <T> PillLabels(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    itemTitle: @Composable (T) -> String,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        items.forEach { item ->
            val isSelected = item == selectedItem
            val textColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(durationMillis = 200),
                label = "pill_text_color",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(FotoRaporTokens.RadiusS))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (!isSelected) {
                            onItemSelected(item)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = itemTitle(item),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor,
                )
            }
        }
    }
}
