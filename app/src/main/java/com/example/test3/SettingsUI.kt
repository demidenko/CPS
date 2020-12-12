package com.example.test3

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KProperty


fun<T> LiveData<T>.observeUpdates(owner: LifecycleOwner, _onChanged: (T)->Unit){
    observe(owner, object : Observer<T> {
        var ignore = true
        override fun onChanged(t: T) {
            if(!ignore) _onChanged(t)
            else ignore = false
        }
    })
}


class SettingsUI(context: Context) {
    private val dataStore = context.createDataStore(name = "settings_ui")

    private val KEY_USE_REAL_COLORS = preferencesKey<Boolean>("use_real_colors")

    private val flowForUseRealColors = dataStore.data.map {
        it[KEY_USE_REAL_COLORS] ?: false
    }

    val userRealColorsLiveData = flowForUseRealColors.asLiveData()

    suspend fun getUseRealColors() = flowForUseRealColors.first()

    suspend fun setUseRealColors(use: Boolean) {
        dataStore.edit {
            it[KEY_USE_REAL_COLORS] = use
        }
    }
}



val Context.settingsUI by SettingsUIDelegate()

fun Context.getUseRealColors() = runBlocking { settingsUI.getUseRealColors() }

suspend fun Context.setUseRealColors(use: Boolean) = settingsUI.setUseRealColors(use)

class SettingsUIDelegate {

    private var _dataStore: SettingsUI? = null

    operator fun getValue(thisRef: Context, property: KProperty<*>): SettingsUI {
        return _dataStore ?: SettingsUI(thisRef).also {
            _dataStore = it
        }
    }
}