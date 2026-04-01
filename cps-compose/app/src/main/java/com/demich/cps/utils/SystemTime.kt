package com.demich.cps.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

fun getSystemTime(): Instant =
    Clock.System.now()

fun Instant.toSystemDateTime(): LocalDateTime =
    toLocalDateTime(timeZone = TimeZone.currentSystemDefault())

fun Instant.toSystemLocalDate(): LocalDate = toSystemDateTime().date