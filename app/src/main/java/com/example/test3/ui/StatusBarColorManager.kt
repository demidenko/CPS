package com.example.test3.ui

import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.test3.MainActivity
import com.example.test3.account_manager.AccountManager
import com.example.test3.account_manager.RatedAccountManager
import kotlinx.coroutines.runBlocking

private const val NO_COLOR = Color.TRANSPARENT

class StatusBarColorManager(
    mainActivity: MainActivity,
    managers: List<AccountManager>
) {

    private class ColorInfo(
        val color: Int = NO_COLOR,
        val order: Double = -1e9
    )

    private val colors = mutableMapOf<String,ColorInfo>()
    private var current: String? = null

    private fun getListOfColors(): List<ColorInfo> {
        if(current != null) return listOfNotNull(colors[current])
        return colors.values.toList()
    }

    private var enabled: Boolean = true
    init {
        with(mainActivity){
            managers.forEach { manager ->
                manager.getDataStoreLive().observe(this){
                    runBlocking { updateBy(manager) }
                }
            }
            settingsUI.useStatusBarLiveData.observe(this){ use ->
                enabled = use
                recalculateStatusBarColor()
            }
            settingsUI.useRealColorsLiveData.observeUpdates(this){ use ->
                runBlocking {
                    managers.forEach { updateBy(it) }
                }
            }
        }
    }

    private val statusBarColor = MutableLiveData<Int>()
    fun getStatusBarColorLiveData(): LiveData<Int> = statusBarColor

    private fun recalculateStatusBarColor() {
        val color = if(enabled){
            getListOfColors().maxByOrNull { it.order }?.color ?: NO_COLOR
        } else NO_COLOR
        statusBarColor.value = color
    }

    fun setCurrent(manager: AccountManager?) {
        current = manager?.managerName
        recalculateStatusBarColor()
    }

    private suspend fun updateBy(manager: AccountManager) {
        val name = manager.managerName
        val info = manager.getSavedInfo()
        if(!info.isEmpty()) {
            colors[name] = ColorInfo()
            if(manager is RatedAccountManager){
                manager.getColor(info)?.let {
                    colors[name] = ColorInfo(it, manager.getOrder(info))
                }
            }
        } else {
            colors.remove(name)
        }
        recalculateStatusBarColor()
    }
}