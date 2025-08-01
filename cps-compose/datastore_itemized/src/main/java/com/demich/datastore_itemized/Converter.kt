package com.demich.datastore_itemized

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences

internal abstract class Converter<T, S: Any>(
    private val key: Preferences.Key<S>
): PreferencesSaver<T> {
    protected abstract fun fromPrefs(s: S?): T
    protected abstract fun toPrefs(t: T): S?

    override fun restore(prefs: Preferences): T = fromPrefs(prefs[key])

    override fun save(prefs: MutablePreferences, value: T) {
        toPrefs(value)
            ?.let { prefs[key] = it }
            ?: prefs.remove(key)
    }

    override fun removeFrom(prefs: MutablePreferences) {
        prefs.remove(key)
    }

    override fun prefsEquivalent(old: Preferences, new: Preferences) =
        old[key] == new[key]
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
    override fun toPrefs(t: T?): T? = t
}

internal class ValueConvertible<T, S: Any>(
    key: Preferences.Key<S>,
    private val defaultValue: () -> T,
    private val encode: (T) -> S,
    private val decode: (S) -> T
): Converter<T, S>(key) {
    override fun fromPrefs(s: S?): T {
        if (s == null) return defaultValue()
        return runCatching { decode(s) }.getOrElse { defaultValue() }
    }

    override fun toPrefs(t: T): S = encode(t)
}