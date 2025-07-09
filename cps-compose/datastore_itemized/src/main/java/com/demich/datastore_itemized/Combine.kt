package com.demich.datastore_itemized

import androidx.datastore.preferences.core.Preferences

internal fun <D: ItemizedDataStore, R> D.dataStoreValue(
    reader: PreferencesReader<R>
): DataStoreValue<R> =
    object : DataStoreValue<R>(
        dataStore = dataStore,
        reader = reader
    ) { }


fun <D: ItemizedDataStore, T, R> D.transform(
    item: DataStoreValue<T>,
    transform: (T) -> R
): DataStoreValue<R> = dataStoreValue(object : PreferencesReader<R> {
        override fun restore(prefs: Preferences): R =
            transform(item.reader.restore(prefs))

        override fun prefsEquivalent(old: Preferences, new: Preferences): Boolean =
            item.reader.prefsEquivalent(old, new)
    })

fun <D: ItemizedDataStore, T1, T2, R> D.combine(
    item1: DataStoreValue<T1>,
    item2: DataStoreValue<T2>,
    transform: (T1, T2) -> R
): DataStoreValue<R> = dataStoreValue(object : PreferencesReader<R> {
        override fun restore(prefs: Preferences): R =
            transform(
                item1.reader.restore(prefs),
                item2.reader.restore(prefs)
            )

        override fun prefsEquivalent(old: Preferences, new: Preferences): Boolean =
            item1.reader.prefsEquivalent(old, new) &&
            item2.reader.prefsEquivalent(old, new)
    })

fun <D: ItemizedDataStore, T1, T2, T3, R> D.combine(
    item1: DataStoreValue<T1>,
    item2: DataStoreValue<T2>,
    item3: DataStoreValue<T3>,
    transform: (T1, T2, T3) -> R
): DataStoreValue<R> = dataStoreValue(object : PreferencesReader<R> {
    override fun restore(prefs: Preferences): R =
        transform(
            item1.reader.restore(prefs),
            item2.reader.restore(prefs),
            item3.reader.restore(prefs)
        )

    override fun prefsEquivalent(old: Preferences, new: Preferences): Boolean =
        item1.reader.prefsEquivalent(old, new) &&
        item2.reader.prefsEquivalent(old, new) &&
        item3.reader.prefsEquivalent(old, new)
})

fun <D: ItemizedDataStore, T1, T2, T3, T4, R> D.combine(
    item1: DataStoreValue<T1>,
    item2: DataStoreValue<T2>,
    item3: DataStoreValue<T3>,
    item4: DataStoreValue<T4>,
    transform: (T1, T2, T3, T4) -> R
): DataStoreValue<R> = dataStoreValue(object : PreferencesReader<R> {
    override fun restore(prefs: Preferences): R =
        transform(
            item1.reader.restore(prefs),
            item2.reader.restore(prefs),
            item3.reader.restore(prefs),
            item4.reader.restore(prefs)
        )

    override fun prefsEquivalent(old: Preferences, new: Preferences): Boolean =
        item1.reader.prefsEquivalent(old, new) &&
        item2.reader.prefsEquivalent(old, new) &&
        item3.reader.prefsEquivalent(old, new) &&
        item4.reader.prefsEquivalent(old, new)
})