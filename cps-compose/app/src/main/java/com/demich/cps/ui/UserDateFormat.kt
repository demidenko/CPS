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

    private val dM = LocalDate.Format {
        day()
        char(delimiter)
        monthNumber()
    }

    private val dME = LocalDate.Format {
        dayOfWeek(names = dayOfWeekShortNames)
        chars(" ")
        date(dM)
    }

    private val dMY = LocalDate.Format {
        date(dM)
        char(delimiter)
        year()
    }

    private val dMYE = LocalDate.Format {
        dayOfWeek(names = dayOfWeekShortNames)
        chars(" ")
        date(dMY)
    }

    fun dateFormat(
        showYear: Boolean,
        showDayOfWeek: Boolean
    ) =
        if (showYear) {
            if (showDayOfWeek) dMYE else dMY
        } else {
            if (showDayOfWeek) dME else dM
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
    toLocalDateTime(timeZone = timeZone).date
        .format(CPSDateFormats.dateFormat(showYear = true, showDayOfWeek = false))