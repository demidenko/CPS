package com.demich.datastore_itemized

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences

internal interface PreferencesSaver<T>: PreferencesReader<T> {
    fun save(prefs: MutablePreferences, value: T)

    fun removeFrom(prefs: MutablePreferences)
}

internal interface PreferencesReader<out T> {
    fun restore(prefs: Preferences): T

    fun prefsEquivalent(old: Preferences, new: Preferences): Boolean
}