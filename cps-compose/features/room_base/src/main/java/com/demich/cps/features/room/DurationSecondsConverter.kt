package com.demich.cps.features.room

import androidx.room3.ColumnTypeConverter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class DurationSecondsConverter {
    @ColumnTypeConverter
    fun durationToSeconds(duration: Duration): Long = duration.inWholeSeconds

    @ColumnTypeConverter
    fun secondsToDuration(seconds: Long): Duration = seconds.seconds
}