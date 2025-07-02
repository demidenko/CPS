package com.demich.datastore_itemized

import androidx.datastore.preferences.core.Preferences

fun <D: ItemizedDataStore, T, R> D.transform(
    item: DataStoreValue<T>,
    transform: (T) -> R
): DataStoreValue<R> = object : DataStoreValue<R>(
    dataStore = dataStore,
    reader = object : PreferencesReader<R> {
        override fun restore(prefs: Preferences): R =
            transform(item.reader.restore(prefs))

        override fun prefsEquivalent(old: Preferences, new: Preferences): Boolean =
            item.reader.prefsEquivalent(old, new)
    }
) { }

fun <D: ItemizedDataStore, T1, T2, R> D.combine(
    item1: DataStoreValue<T1>,
    item2: DataStoreValue<T2>,
    transform: (T1, T2) -> R
): DataStoreValue<R> = object : DataStoreValue<R>(
    dataStore = dataStore,
    reader = object : PreferencesReader<R> {
        override fun restore(prefs: Preferences): R =
            transform(
                item1.reader.restore(prefs),
                item2.reader.restore(prefs)
            )

        override fun prefsEquivalent(old: Preferences, new: Preferences): Boolean =
               item1.reader.prefsEquivalent(old, new)
            && item2.reader.prefsEquivalent(old, new)
    }
) { }