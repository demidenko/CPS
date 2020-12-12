package com.example.test3

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KProperty

class SettingsUI(context: Context) {
    private val dataStore = context.createDataStore(name = "settings_ui")

    private val KEY_USE_REAL_COLORS = preferencesKey<Boolean>("use_real_colors")

    private val flowForUseRealColors = dataStore.data.map {
        it[KEY_USE_REAL_COLORS] ?: false
    }

    suspend fun getUseRealColors() = flowForUseRealColors.first()

    suspend fun setUseRealColors(use: Boolean) {
        dataStore.edit {
            it[KEY_USE_REAL_COLORS] = use
        }
    }
}



var Context.useRealColors by UseRealColorsDelegate()

class UseRealColorsDelegate {

    private var _dataStore: SettingsUI? = null

    private fun getOrCreateDataStore(thisRef: Context): SettingsUI {
        return _dataStore ?: SettingsUI(thisRef).also {
            _dataStore = it
        }
    }

    operator fun getValue(thisRef: Context, property: KProperty<*>): Boolean {
        return runBlocking { getOrCreateDataStore(thisRef).getUseRealColors() }
    }

    operator fun setValue(thisRef: Context, property: KProperty<*>, value: Boolean) {
        runBlocking {
            getOrCreateDataStore(thisRef).setUseRealColors(value)
        }
    }
}