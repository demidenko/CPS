package com.demich.cps.features.room

import androidx.room3.ColumnTypeConverter
import kotlin.time.Instant


class InstantSecondsConverter {
    @ColumnTypeConverter
    fun instantToSeconds(time: Instant): Long = time.epochSeconds

    @ColumnTypeConverter
    fun secondsToInstant(seconds: Long): Instant = Instant.fromEpochSeconds(seconds)
}
