@file:Suppress("MagicNumber")
package com.fatihenes.photoreport.feature.project.ui.markup.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatihenes.photoreport.core.model.MarkupTextStyle
import com.fatihenes.photoreport.core.ui.R
import com.fatihenes.photoreport.feature.project.ui.markup.MarkupAccentGreen

@Composable
fun AdvancedTextEditorModal(
    initialText: String,
    initialColor: Color,
    initialStyle: MarkupTextStyle,
    initialIsBold: Boolean = true,
    initialIsItalic: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (text: String, color: Color, style: MarkupTextStyle, isBold: Boolean, isItalic: Boolean) -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    var color by remember { mutableStateOf(initialColor) }
    var style by remember { mutableStateOf(initialStyle) }
    var isBold by remember { mutableStateOf(initialIsBold) }
    var isItalic by remember { mutableStateOf(initialIsItalic) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.82f)
    ) {
        Box(modifier = Modifier.fillMaxSize().imePadding()) {

            // Backdrop tap
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (text.isNotBlank()) onConfirm(text.trim(), color, style, isBold, isItalic)
                            else onDismiss()
                        }
                    )
            )

            // Top Control Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.acc_close), tint = Color.White)
                }

                // Style Selector Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        MarkupTextStyle.BADGE to R.string.markup_text_style_badge,
                        MarkupTextStyle.FROSTED to R.string.markup_text_style_frosted,
                        MarkupTextStyle.OUTLINE to R.string.markup_text_style_outline,
                        MarkupTextStyle.TRANSPARENT to R.string.markup_text_style_transparent
                    ).forEach { (s, label) ->
                        val isSel = style == s
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) color else Color(0x33FFFFFF))
                                .clickable { style = s }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                stringResource(label),
                                color = if (isSel) Color.White else Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                IconButton(onClick = {
                    if (text.isNotBlank()) onConfirm(text.trim(), color, style, isBold, isItalic)
                    else onDismiss()
                }) {
                    Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save_label), tint = MarkupAccentGreen)
                }
            }

            // Central Text Input Area
            Column(
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        color = if (style == MarkupTextStyle.OUTLINE) color else Color.White,
                        fontSize = 30.sp,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(color),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .then(when (style) {
                                    MarkupTextStyle.BADGE -> Modifier.background(color.copy(alpha = 0.9f), RoundedCornerShape(12.dp)).padding(14.dp)
                                    MarkupTextStyle.FROSTED -> Modifier.background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(12.dp)).padding(14.dp)
                                    MarkupTextStyle.OUTLINE, MarkupTextStyle.TRANSPARENT -> Modifier.padding(14.dp)
                                }),
                            contentAlignment = Alignment.Center
                        ) {
                            if (text.isEmpty()) {
                                Text(
                                    stringResource(R.string.markup_text_prompt_hint),
                                    color = Color.White.copy(alpha = 0.45f),
                                    fontSize = 22.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Bold / Italic Toggles
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isBold) Color(0x66FFFFFF) else Color(0x22FFFFFF))
                            .clickable { isBold = !isBold }
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = Color.White)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isItalic) Color(0x66FFFFFF) else Color(0x22FFFFFF))
                            .clickable { isItalic = !isItalic }
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = Color.White)
                    }
                }
            }

            // Bottom Palette
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
            ) {
                AdvancedColorPicker(
                    selectedColor = color,
                    onSelectColor = { color = it }
                )
            }
        }
    }
}
