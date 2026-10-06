@file:Suppress("MagicNumber")
package com.fatihenes.photoreport.feature.project.ui.markup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Premium Thickness Selector — Yatay bir slider ile hassas kalınlık kontrolü.
 */
@Composable
fun MarkupThicknessSelector(
    selectedThickness: Float,
    onSelectThickness: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(140.dp)
            .height(36.dp)
            .background(Color(0x991E232F), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Slider(
            value = selectedThickness,
            onValueChange = onSelectThickness,
            valueRange = 0.002f..0.035f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = MarkupAccentGreen,
                inactiveTrackColor = Color.White.copy(alpha = 0.24f)
            ),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
