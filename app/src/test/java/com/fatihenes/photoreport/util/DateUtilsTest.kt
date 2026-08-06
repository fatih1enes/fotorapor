package com.fatihenes.photoreport.util

import com.fatihenes.photoreport.core.common.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

class DateUtilsTest {

    @Test
    fun testGetStartOfDayEpochMillis() {
        val date = LocalDate.of(2023, 10, 15)
        val millis = DateUtils.getStartOfDayEpochMillis(date)
        val expectedMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expectedMillis, millis)
    }

    @Test
    fun testFormatDate() {
        val date = LocalDate.of(2023, 10, 15)
        val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val formattedDate = DateUtils.formatDate(millis, "tr")
        // October 15, 2023, was a Sunday
        assertEquals("15 Ekim Pazar", formattedDate)
    }

    @Test
    fun testFormatDateEnglish() {
        val date = LocalDate.of(2023, 10, 15)
        val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val formattedDate = DateUtils.formatDate(millis, "en")
        assertEquals("October 15, Sunday", formattedDate)
    }

    @Test
    fun testGetStartOfDayWithCalendar() {
        val cal = Calendar.getInstance()
        cal.set(2023, Calendar.OCTOBER, 15, 14, 30, 45)
        cal.set(Calendar.MILLISECOND, 123)

        val startOfDayMillis = DateUtils.getStartOfDay(cal)

        val startCal = Calendar.getInstance()
        startCal.timeInMillis = startOfDayMillis

        assertEquals(2023, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, startCal.get(Calendar.MONTH))
        assertEquals(15, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startCal.get(Calendar.MINUTE))
        assertEquals(0, startCal.get(Calendar.SECOND))
        assertEquals(0, startCal.get(Calendar.MILLISECOND))
    }
}
