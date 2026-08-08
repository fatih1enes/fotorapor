package com.fatihenes.photoreport.feature.project.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporMotion
import com.fatihenes.photoreport.core.designsystem.theme.FotoRaporTokens
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters

@Composable
fun WeekCalendar(
    projectColor: Color,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedDay by remember { mutableStateOf(LocalDate.now()) }
    val currentWeekDays = remember { calculateCurrentWeekDays() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = FotoRaporTokens.SpacingL),
        shape = RoundedCornerShape(FotoRaporTokens.RadiusL),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FotoRaporTokens.SpacingM),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            currentWeekDays.forEach { date ->
                CalendarDayItem(
                    date = date,
                    isSelected = date == selectedDay,
                    projectColor = projectColor,
                    onClick = {
                        selectedDay = date
                        onDateSelected(date)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun calculateCurrentWeekDays(): List<LocalDate> {
    val today = LocalDate.now()
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0..6).map { monday.plusDays(it.toLong()) }
}

@Suppress("FunctionName")
@Composable
private fun CalendarDayItem(
    date: LocalDate,
    isSelected: Boolean,
    projectColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isToday = remember(date) { date == LocalDate.now() }
    val currentLocale = androidx.compose.ui.text.intl.Locale.current.platformLocale

    val dayName = remember(date, currentLocale) {
        date.dayOfWeek.getDisplayName(TextStyle.SHORT, currentLocale).uppercase()
    }

    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) projectColor.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(FotoRaporMotion.DurationShort),
        label = "week_day_bg"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(FotoRaporTokens.RadiusS))
            .background(animatedBgColor)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(vertical = FotoRaporTokens.SpacingS)
    ) {
        Text(
            text = dayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = if (isSelected) projectColor else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(FotoRaporTokens.SpacingXS))
        DayNumberBadge(
            dayOfMonth = date.dayOfMonth,
            isSelected = isSelected,
            isToday = isToday,
            projectColor = projectColor
        )
    }
}

@Suppress("FunctionName")
@Composable
private fun DayNumberBadge(
    dayOfMonth: Int,
    isSelected: Boolean,
    isToday: Boolean,
    projectColor: Color
) {
    val targetBackgroundColor = when {
        isSelected -> projectColor
        isToday -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> Color.Transparent
    }

    val targetTextColor = when {
        isSelected -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    val backgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(FotoRaporMotion.DurationShort),
        label = "day_badge_bg"
    )

    val textColor by animateColorAsState(
        targetValue = targetTextColor,
        animationSpec = tween(FotoRaporMotion.DurationShort),
        label = "day_badge_text"
    )

    val fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium

    Box(
        modifier = Modifier
            .size(32.dp)
            .background(backgroundColor, RoundedCornerShape(FotoRaporTokens.RadiusXS)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = fontWeight,
            color = textColor,
        )
    }
}

