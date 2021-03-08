package com.example.test3.ui

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import com.example.test3.MainActivity
import com.example.test3.utils.SettingsDataStore
import kotlinx.coroutines.flow.distinctUntilChanged
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


class SettingsUI(context: Context): SettingsDataStore(context, "settings_ui") {

    private val KEY_USE_REAL_COLORS = booleanPreferencesKey("use_real_colors")
    private val KEY_USE_STATUS_BAR = booleanPreferencesKey("use_status_bar")
    private val KEY_DARK_LIGHT_MODE = stringPreferencesKey("dark_light_mode")

    private val useRealColorsFlow = dataStore.data.map { it[KEY_USE_REAL_COLORS] ?: false }
    val useRealColorsLiveData = useRealColorsFlow.distinctUntilChanged().asLiveData()
    suspend fun getUseRealColors() = useRealColorsFlow.first()
    suspend fun setUseRealColors(use: Boolean) {
        dataStore.edit { it[KEY_USE_REAL_COLORS] = use }
    }

    private val useStatusBar = dataStore.data.map { it[KEY_USE_STATUS_BAR] ?: true }
    val useStatusBarLiveData = useStatusBar.distinctUntilChanged().asLiveData()
    suspend fun getUseStatusBar() = useStatusBar.first()
    suspend fun setUseStatusBar(use: Boolean) {
        dataStore.edit { it[KEY_USE_STATUS_BAR] = use }
    }

    enum class DarkLightMode { DARK, LIGHT, /*AUTO*/ }
    private val darkLightMode = dataStore.data.map { it[KEY_DARK_LIGHT_MODE]?.let { DarkLightMode.valueOf(it) } ?: DarkLightMode.DARK }
    val darkLightModeLiveData = darkLightMode.distinctUntilChanged().asLiveData()
    suspend fun getDarkLightMode() = darkLightMode.first()
    suspend fun setDarkLightMode(mode: DarkLightMode) {
        dataStore.edit { it[KEY_DARK_LIGHT_MODE] = mode.name }
    }
}

val Context.settingsUI by SettingsDelegate{ SettingsUI(it) }
fun Context.getUseRealColors() = runBlocking { settingsUI.getUseRealColors() }
suspend fun Context.setUseRealColors(use: Boolean) = settingsUI.setUseRealColors(use)


class SettingsDelegate<T: SettingsDataStore>(
    val create: (Context)->T
) {
    private var _dataStore: T? = null

    operator fun getValue(thisRef: Context, property: KProperty<*>): T {
        return _dataStore ?: create(thisRef).also {
            _dataStore = it
        }
    }
}

class PanelSettingsUI(val mainActivity: MainActivity){

}