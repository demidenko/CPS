package com.demich.datastore_itemized

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences

internal interface PreferencesSaver<T> {
    fun save(prefs: MutablePreferences, value: T)

    fun restore(prefs: Preferences): T

    fun removeFrom(prefs: MutablePreferences)

    fun prefsEquivalent(old: Preferences, new: Preferences): Boolean
}