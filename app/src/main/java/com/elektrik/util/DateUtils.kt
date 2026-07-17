package com.sarikaya.santiye.gunlugu.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

object DateUtils {
    private val turkishLocale = Locale.Builder().setLanguage("tr").setRegion("TR").build()
    private val formatter = DateTimeFormatter.ofPattern("d MMMM EEEE", turkishLocale)

    fun getStartOfDayEpochMillis(date: LocalDate = LocalDate.now()): Long {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun formatDate(millis: Long): String {
        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
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
