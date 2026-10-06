package com.fatihenes.photoreport.feature.project.ui.markup

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fatihenes.photoreport.core.ui.R

/**
 * Değişiklikleri iptal etme onay dialogu.
 * "Düzenlemeleriniz silinecek" mesajı ile Discard / Keep Editing seçenekleri sunar.
 */
@Composable
fun MarkupDiscardDialog(
    onDismiss: () -> Unit,
    onConfirmDiscard: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color(0xFF1E232E),
        title = {
            Text(
                stringResource(R.string.markup_discard_title),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                stringResource(R.string.markup_discard_msg),
                color = Color(0xFFB0B7C3)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDiscard,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF334B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.markup_discard_btn), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.markup_discard_cancel), color = Color.White)
            }
        }
    )
}
