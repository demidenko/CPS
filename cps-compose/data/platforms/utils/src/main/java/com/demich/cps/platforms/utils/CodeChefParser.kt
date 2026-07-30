package com.demich.cps.platforms.utils

import com.demich.cps.platforms.api.codechef.CodeChefRatingChange
import com.demich.cps.profiles.userinfo.CodeChefUserInfo
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlin.time.Instant

class CodeChefParser {
    fun extractUserInfo(source: String, handle: String): CodeChefUserInfo =
        source.parseDocument().run {
            val rating = selectFirst("div.widget-rating")
                ?.selectFirst("div.rating-header > div.rating-number")?.text()?.toInt()
            /*val rating = expectFirst("div.rating-ranks")
                .select("a")
                .takeIf { it.any { it.text() != "Inactive" } }
                ?.let { selectFirst("div.rating-header > div.rating-number")?.text()?.toInt() }
             */
            val userName = expectFirst("section.user-details")
                .selectFirst("span.m-username--link")
                ?.text() ?: handle
            return CodeChefUserInfo(
                handle = userName,
                rating = rating
            )
        }
}

fun CodeChefRatingChange.date(): Instant =
    LocalDateTime.Format {
        date(LocalDate.Formats.ISO)
        char(' ')
        time(LocalTime.Formats.ISO)
    }.parse(end_date).toInstant(TimeZone.of("IST"))