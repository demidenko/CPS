package com.demich.cps.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

fun getSystemTime(): Instant =
    Clock.System.now()

fun getSystemTimeZone(): TimeZone =
    TimeZone.currentSystemDefault()

fun Instant.toSystemDateTime(): LocalDateTime =
    toLocalDateTime(timeZone = getSystemTimeZone())

fun Instant.toSystemLocalDate(): LocalDate = toSystemDateTime().date

inline fun <R> contextSystemTimeZone(block: context(TimeZone) () -> R): R =
    context(getSystemTimeZone(), block = block)