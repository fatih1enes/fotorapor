@file:Suppress("MagicNumber")
package com.fatihenes.photoreport.feature.project.ui.markup.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatihenes.photoreport.core.model.MeasurementUnit
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.feature.project.ui.markup.MarkupAccentGreen
import com.fatihenes.photoreport.feature.project.ui.markup.MarkupAccentOnGreen

@Composable
fun MeasureCalibrationModal(
    currentUnit: MeasurementUnit,
    onDismiss: () -> Unit,
    onConfirm: (realDistance: Float, unit: MeasurementUnit) -> Unit
) {
    var distanceText by remember { mutableStateOf("1.0") }
    var selectedUnit by remember { mutableStateOf(currentUnit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.markup_measure_dialog_title), color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.markup_measure_dialog_msg), color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = distanceText,
                    onValueChange = { distanceText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF161B26),
                        unfocusedContainerColor = Color(0xFF161B26),
                        focusedIndicatorColor = MarkupAccentGreen,
                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(R.string.markup_measure_unit), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MeasurementUnit.entries.forEach { unit ->
                        val isSel = selectedUnit == unit
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) MarkupAccentGreen else Color(0x33FFFFFF))
                                .clickable { selectedUnit = unit }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                unit.symbol,
                                color = if (isSel) MarkupAccentOnGreen else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dist = distanceText.toFloatOrNull() ?: 1.0f
                    onConfirm(dist, selectedUnit)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MarkupAccentGreen, contentColor = MarkupAccentOnGreen)
            ) {
                Text(stringResource(R.string.save_label), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_label), color = Color.White.copy(alpha = 0.8f))
            }
        },
        containerColor = Color(0xFF1E232F)
    )
}
