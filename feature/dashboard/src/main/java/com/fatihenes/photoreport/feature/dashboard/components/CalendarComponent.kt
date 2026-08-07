package com.fatihenes.photoreport.feature.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
import com.fatihenes.photoreport.core.ui.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun MonthlyCalendar(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    activityDots: Map<LocalDate, List<Color>> = emptyMap(),
    locale: Locale = Locale.getDefault()
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val weekdayNames = remember(locale, firstDayOfWeek) {
        (0 until 7).map { offset ->
            firstDayOfWeek.plus(offset.toLong()).getDisplayName(TextStyle.NARROW, locale)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(FotoRaporTokens.SpacingL)
            .graphicsLayer { clip = true } // Optimization
    ) {
        CalendarHeader(
            currentMonth = currentMonth,
            locale = locale,
            onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
            onNextMonth = { currentMonth = currentMonth.plusMonths(1) }
        )

        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingL))

        WeekdayLabels(weekdayNames)

        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingS))

        CalendarMonthGrid(
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            firstDayOfWeek = firstDayOfWeek,
            activityDots = activityDots,
            onDateSelected = onDateSelected
        )
    }
}

@Composable
private fun CalendarHeader(
    currentMonth: YearMonth,
    locale: Locale,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.back_label),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(FotoRaporTokens.IconSizeS)
            )
        }

        Text(
            text = buildString {
                append(currentMonth.month.getDisplayName(TextStyle.FULL, locale)
                    .replaceFirstChar { it.uppercase() })
                append(" ")
                append(currentMonth.year)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        IconButton(onClick = onNextMonth, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.acc_expand),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(FotoRaporTokens.IconSizeS)
            )
        }
    }
}

@Composable
private fun WeekdayLabels(weekdayNames: List<String>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        weekdayNames.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    firstDayOfWeek: java.time.DayOfWeek,
    activityDots: Map<LocalDate, List<Color>>,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = (currentMonth.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val totalCells = ((daysInMonth + firstDayOfMonth + 6) / 7) * 7

    for (row in 0 until totalCells / 7) {
        Row(modifier = Modifier.fillMaxWidth()) {
            for (col in 0 until 7) {
                val dayIndex = row * 7 + col - firstDayOfMonth + 1
                if (dayIndex in 1..daysInMonth) {
                    val date = currentMonth.atDay(dayIndex)
                    CalendarDateCell(
                        date = date,
                        dayIndex = dayIndex,
                        isSelected = date == selectedDate,
                        isToday = date == LocalDate.now(),
                        dots = activityDots[date] ?: emptyList(),
                        onDateSelected = onDateSelected
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RowScope.CalendarDateCell(
    date: LocalDate,
    dayIndex: Int,
    isSelected: Boolean,
    isToday: Boolean,
    dots: List<Color>,
    onDateSelected: (LocalDate) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(FotoRaporTokens.RadiusS))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDateSelected(date)
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DateCellBackground(dayIndex, isSelected, isToday)
            ActivityDotsRow(dots)
        }
    }
}

@Composable
private fun DateCellBackground(dayIndex: Int, isSelected: Boolean, isToday: Boolean) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(FotoRaporTokens.RadiusS)
            )
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(FotoRaporTokens.RadiusS)
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayIndex.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                isToday -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun ActivityDotsRow(dots: List<Color>) {
    Row(
        modifier = Modifier.padding(top = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        dots.take(3).forEach { color ->
            Box(modifier = Modifier.size(4.dp).background(color, CircleShape))
        }
    }
}
