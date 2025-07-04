package com.demich.cps.features.room

import androidx.room.TypeConverter
import kotlin.time.Instant


class InstantSecondsConverter {
    @TypeConverter
    fun instantToSeconds(time: Instant): Long = time.epochSeconds

    @TypeConverter
    fun secondsToInstant(seconds: Long): Instant = Instant.fromEpochSeconds(seconds)
}
