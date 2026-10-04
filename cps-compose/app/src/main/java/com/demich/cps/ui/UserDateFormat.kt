package com.demich.cps.ui

import android.icu.text.DateFormatSymbols
import android.icu.util.Calendar
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/*
TODO:
    custom date format
    - order: MM-DD-YYYY, YYYY-MM-DD, ...
    - delimiter: '.', '/', '-', ...
    - month format: 02, Feb, Фев
    - year: YY or YYYY (contests: do not show if year is current)
    - show day of week (contests option only?)

    chose from world popular formats
    additionally, full constructor

    time format: always HH:MM
    am/pm????
 */


object CPSDateFormats {
    private val dayOfWeekShortNames = DateFormatSymbols().shortWeekdays.let { array ->
        DayOfWeekNames(
            monday = array[Calendar.MONDAY],
            tuesday = array[Calendar.TUESDAY],
            wednesday = array[Calendar.WEDNESDAY],
            thursday = array[Calendar.THURSDAY],
            friday = array[Calendar.FRIDAY],
            saturday = array[Calendar.SATURDAY],
            sunday = array[Calendar.SUNDAY]
        )
    }

    private const val delimiter = '.'

    val HHMM = LocalTime.Format {
        hour()
        char(':')
        minute()
    }

    val ddMM = LocalDate.Format {
        day()
        char(delimiter)
        monthNumber()
    }

    val ddMME = LocalDate.Format {
        date(ddMM)
        char(' ')
        dayOfWeek(names = dayOfWeekShortNames)
    }

    val ddMMYYYY = LocalDate.Format {
        date(ddMM)
        char(delimiter)
        year()
    }

    val ddMMEYYYY = LocalDate.Format {
        date(ddMME)
        char(delimiter)
        year()
    }

    fun dateFormat(
        showYear: Boolean,
        showDayOfWeek: Boolean
    ) =
        if (showYear) {
            if (showDayOfWeek) ddMMEYYYY else ddMMYYYY
        } else {
            if (showDayOfWeek) ddMME else ddMM
        }
}

fun LocalDateTime.formatFullDateTime(
    showYear: Boolean = true,
    showDayOfWeek: Boolean = false
): String {
    val date = date.format(CPSDateFormats.dateFormat(showYear = showYear, showDayOfWeek = showDayOfWeek))
    val time = time.format(CPSDateFormats.HHMM)
    return "$date $time"
}

context(timeZone: TimeZone)
fun Instant.formatFullDateTime(
    showYear: Boolean = true,
    showDayOfWeek: Boolean = false
): String =
    toLocalDateTime(timeZone = timeZone)
        .formatFullDateTime(showYear = showYear, showDayOfWeek = showDayOfWeek)

context(timeZone: TimeZone)
fun Instant.formatFullDate(): String =
    toLocalDateTime(timeZone = timeZone).date.format(CPSDateFormats.ddMMYYYY)