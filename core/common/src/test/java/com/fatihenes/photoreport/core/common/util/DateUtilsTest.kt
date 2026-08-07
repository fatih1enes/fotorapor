package com.fatihenes.photoreport.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

class DateUtilsTest {

    @Test
    fun getStartOfDayEpochMillis_returnsCorrectValue() {
        val date = LocalDate.of(2024, 5, 20)
        val expected = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val actual = DateUtils.getStartOfDayEpochMillis(date)
        assertEquals(expected, actual)
    }

    @Test
    fun formatDate_turkish_returnsCorrectString() {
        // 2024-05-20 is Monday (Pazartesi)
        val date = LocalDate.of(2024, 5, 20)
        val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val result = DateUtils.formatDate(millis, "tr")
        // Format: d MMMM EEEE -> 20 Mayıs Pazartesi
        assertEquals("20 Mayıs Pazartesi", result)
    }

    @Test
    fun formatDate_english_returnsCorrectString() {
        val date = LocalDate.of(2024, 5, 20)
        val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val result = DateUtils.formatDate(millis, "en")
        // Format: MMMM d, EEEE -> May 20, Monday
        assertEquals("May 20, Monday", result)
    }

    @Test
    fun formatDateTime_returnsCorrectString() {
        val millis = 1716195600000L // 2024-05-20 12:00:00 (approx)
        val resultTr = DateUtils.formatDateTime(millis, "tr")
        val resultEn = DateUtils.formatDateTime(millis, "en")

        // SimpleDateFormat output depends on local environment slightly but let's check basic structure
        // dd MMM, HH:mm
        assert(resultTr.contains("May")) // Mayıs in tr
        assert(resultEn.contains("May"))
    }

    @Test
    fun getStartOfDay_calendar_zeroesOutFields() {
        val cal = Calendar.getInstance()
        cal.set(2024, Calendar.MAY, 20, 15, 30, 45)
        cal.set(Calendar.MILLISECOND, 500)

        val resultMillis = DateUtils.getStartOfDay(cal)

        val resultCal = Calendar.getInstance()
        resultCal.timeInMillis = resultMillis

        assertEquals(2024, resultCal.get(Calendar.YEAR))
        assertEquals(Calendar.MAY, resultCal.get(Calendar.MONTH))
        assertEquals(20, resultCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(Calendar.MINUTE))
        assertEquals(0, resultCal.get(Calendar.SECOND))
        assertEquals(0, resultCal.get(Calendar.MILLISECOND))
    }
}
