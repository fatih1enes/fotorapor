package com.fatihenes.photoreport.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

object DateUtils {
    private val turkishLocale = Locale("tr", "TR")
    private val englishLocale = Locale.US
    
    private val trFormatter = DateTimeFormatter.ofPattern("d MMMM EEEE", turkishLocale)
    private val enFormatter = DateTimeFormatter.ofPattern("MMMM d, EEEE", englishLocale)

    fun getStartOfDayEpochMillis(date: LocalDate = LocalDate.now()): Long {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun formatDate(millis: Long, language: String = "tr"): String {
        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        val formatter = if (language == "en") enFormatter else trFormatter
        return date.format(formatter)
    }

    /**
     * Converts a Calendar instance to the start-of-day epoch millis.
     * Zeroes out hour, minute, second, and millisecond fields.
     */
    fun getStartOfDay(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
}
