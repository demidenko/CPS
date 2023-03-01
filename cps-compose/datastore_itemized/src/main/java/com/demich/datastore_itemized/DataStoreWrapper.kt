package com.demich.datastore_itemized

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class DataStoreWrapper(
    internal val dataStore: DataStore<Preferences>
)

fun dataStoreWrapper(
    name: String
) = object : ReadOnlyProperty<Context, DataStoreWrapper> {
    val delegate = preferencesDataStore(name)
    override fun getValue(thisRef: Context, property: KProperty<*>): DataStoreWrapper {
        return DataStoreWrapper(delegate.getValue(thisRef, property))
    }
}