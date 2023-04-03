package com.demich.cps.features.room

import androidx.room.TypeConverter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class DurationSecondsConverter {
    @TypeConverter
    fun durationToSeconds(duration: Duration): Long = duration.inWholeSeconds

    @TypeConverter
    fun secondsToDuration(seconds: Long): Duration = seconds.seconds
}