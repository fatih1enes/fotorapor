package com.fatihenes.photoreport.core.common.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val turkishLocale = Locale.forLanguageTag("tr-TR")
    private val englishLocale = Locale.US

    private val trFormatter = DateTimeFormatter.ofPattern("d MMMM EEEE", turkishLocale)
    private val enFormatter = DateTimeFormatter.ofPattern("MMMM d, EEEE", englishLocale)

    private val trDateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM, HH:mm", turkishLocale)
    private val enDateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d, HH:mm", englishLocale)

    fun getStartOfDayEpochMillis(date: LocalDate = LocalDate.now()): Long {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun formatDate(millis: Long, language: String = "tr"): String {
        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        val formatter = if (language == "en") enFormatter else trFormatter
        return date.format(formatter)
    }

    fun formatDateTime(millis: Long, language: String = "tr"): String {
        val dateTime = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        val formatter = if (language == "en") enDateTimeFormatter else trDateTimeFormatter
        return dateTime.format(formatter)
    }

    fun getStartOfDay(cal: java.util.Calendar): Long {
        val c = cal.clone() as java.util.Calendar
        c.set(java.util.Calendar.HOUR_OF_DAY, 0)
        c.set(java.util.Calendar.MINUTE, 0)
        c.set(java.util.Calendar.SECOND, 0)
        c.set(java.util.Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
}
