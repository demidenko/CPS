package com.example.test3.ui

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.utils.CPSDataStore
import com.example.test3.utils.getColorFromResource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KProperty


class SettingsUI(private val context: Context): CPSDataStore(context.settingsUI_dataStore) {
    companion object {
        private val Context.settingsUI_dataStore by preferencesDataStore("settings_ui")
    }

    val userRealColors = Item(booleanPreferencesKey("use_real_colors"), false)
    val useStatusBar = Item(booleanPreferencesKey("use_status_bar"), true)
    private val KEY_UI_MODE = stringPreferencesKey("ui_mode")

    private val uiModeFlow = dataStore.data.map { it[KEY_UI_MODE]?.let { UIMode.valueOf(it) } ?: getSystemUIMode(context) }
    suspend fun getUIMode() = uiModeFlow.first()
    suspend fun setUIMode(mode: UIMode) {
        dataStore.edit { it[KEY_UI_MODE] = mode.name }
    }
}

val Context.settingsUI by CPSDataStoreDelegate{ SettingsUI(it) }
fun Context.getUseRealColors() = runBlocking { settingsUI.userRealColors() }
suspend fun Context.setUseRealColors(use: Boolean) = settingsUI.userRealColors(use)


class CPSDataStoreDelegate<T: CPSDataStore>(
    val create: (Context)->T
) {
    private var _dataStore: T? = null

    operator fun getValue(thisRef: Context, property: KProperty<*>): T {
        return _dataStore ?: create(thisRef).also {
            _dataStore = it
        }
    }
}

enum class UIMode { DARK, LIGHT, /*SYSTEM*/ }
fun MainActivity.setupUIMode() {
    when(getUIMode()){
        UIMode.DARK -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            setDarkModeBars()
        }
        UIMode.LIGHT -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            setLightModeBars()
        }
    }
}

fun MainActivity.getUIMode(): UIMode = runBlocking{ settingsUI.getUIMode() }

fun MainActivity.setUIMode(mode: UIMode) {
    runBlocking { settingsUI.setUIMode(mode) }
    when(mode){
        UIMode.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        UIMode.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}

private fun getSystemUIMode(context: Context): UIMode {
    return when(context.resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK)) {
        Configuration.UI_MODE_NIGHT_NO -> UIMode.LIGHT
        else -> UIMode.DARK
    }
}

private fun MainActivity.setLightModeBars() {
    window.navigationBarColor = getColorFromResource(this, R.color.navigation_background)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        window.insetsController?.setSystemBarsAppearance(0.inv(), WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
        window.insetsController?.setSystemBarsAppearance(0.inv(), WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
    } else {
        var flags = window.decorView.systemUiVisibility
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }
        window.decorView.systemUiVisibility = flags
    }
}

private fun MainActivity.setDarkModeBars() {
    window.navigationBarColor = getColorFromResource(this, R.color.navigation_background)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        window.insetsController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
        window.insetsController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
    } else {
        var flags = window.decorView.systemUiVisibility
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                flags = flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
        }
        window.decorView.systemUiVisibility = flags
    }
}