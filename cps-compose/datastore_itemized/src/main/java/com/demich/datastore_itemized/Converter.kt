package com.demich.datastore_itemized

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal abstract class Converter<T, S: Any>(
    val key: Preferences.Key<S>
) {
    protected abstract fun fromPrefs(s: S?): T
    protected abstract fun toPrefs(t: T & Any): S

    fun flowFrom(prefs: Flow<Preferences>): Flow<T> =
        prefs.map { it[key] }.distinctUntilChanged().map(::fromPrefs)

    fun getFrom(prefs: Preferences): T = fromPrefs(prefs[key])

    fun setTo(prefs: MutablePreferences, value: T) {
        if (value == null) prefs.remove(key)
        else prefs[key] = toPrefs(value)
    }

    internal fun mapGetter(transform: (T) -> T): Converter<T, S> =
        object : Converter<T, S>(key = key) {
            override fun fromPrefs(s: S?) = transform(this@Converter.fromPrefs(s))
            override fun toPrefs(t: T & Any) = this@Converter.toPrefs(t)
        }
}

internal class ValueWithDefault<T: Any>(
    key: Preferences.Key<T>,
    private val defaultValue: T
): Converter<T, T>(key) {
    override fun fromPrefs(s: T?): T = s ?: defaultValue
    override fun toPrefs(t: T): T = t
}

internal class ValueNullable<T: Any>(
    key: Preferences.Key<T>
): Converter<T?, T>(key) {
    override fun fromPrefs(s: T?): T? = s
    override fun toPrefs(t: T): T = t
}

internal class ValueConvertible<T, S: Any>(
    key: Preferences.Key<S>,
    private val defaultValue: () -> T,
    private val encode: (T) -> S,
    private val decode: (S) -> T
): Converter<T, S>(key) {
    override fun fromPrefs(s: S?): T = s?.runCatching(decode)?.getOrNull() ?: defaultValue()
    override fun toPrefs(t: T & Any): S = encode(t)
}