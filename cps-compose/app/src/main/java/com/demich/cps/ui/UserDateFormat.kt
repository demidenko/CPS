package com.demich.cps.ui

import com.demich.cps.utils.RUSSIAN_ABBREVIATED
import com.demich.cps.utils.isRuSystemLanguage
import com.demich.cps.utils.toSystemDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.char
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
        dayOfWeek(names =
            if (isRuSystemLanguage()) DayOfWeekNames.RUSSIAN_ABBREVIATED
            else DayOfWeekNames.ENGLISH_ABBREVIATED
        )
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

fun Instant.formatFullDateTime(
    showYear: Boolean = true,
    showDayOfWeek: Boolean = false
): String = toSystemDateTime().formatFullDateTime(showYear = showYear, showDayOfWeek = showDayOfWeek)

fun Instant.formatFullDate(): String =
    toSystemDateTime().date.format(CPSDateFormats.ddMMYYYY)