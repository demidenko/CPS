package com.demich.cps.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.datetime.TimeZone


val LocalTimeZone = staticCompositionLocalOf<TimeZone> {
    throw IllegalAccessException("timezone not provided")
}

@Composable
@ReadOnlyComposable
inline fun <T> contextLocalTimeZone(block: context(TimeZone) () -> T): T =
    context(with = LocalTimeZone.current, block = block)

@Composable
fun ProvideSystemTimeZone(content: @Composable () -> Unit) {
    val systemTimeZone = remember { getSystemTimeZone() }
    CompositionLocalProvider(
        LocalTimeZone provides systemTimeZone,
        content = content
    )
}