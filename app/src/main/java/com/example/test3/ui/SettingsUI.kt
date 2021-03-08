package com.example.test3.ui

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.getColorFromResource
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


class SettingsUI(private val context: Context): SettingsDataStore(context, "settings_ui") {

    private val KEY_USE_REAL_COLORS = booleanPreferencesKey("use_real_colors")
    private val KEY_USE_STATUS_BAR = booleanPreferencesKey("use_status_bar")
    private val KEY_UI_MODE = stringPreferencesKey("ui_mode")

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


    private val uiModeFlow = dataStore.data.map { it[KEY_UI_MODE]?.let { UIMode.valueOf(it) } ?: getSystemUIMode(context) }
    suspend fun getUIMode() = uiModeFlow.first()
    suspend fun setUIMode(mode: UIMode) {
        dataStore.edit { it[KEY_UI_MODE] = mode.name }
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