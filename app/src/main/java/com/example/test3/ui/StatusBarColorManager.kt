package com.example.test3.ui

import android.graphics.Color
import com.example.test3.MainActivity
import com.example.test3.account_manager.AccountManager
import com.example.test3.account_manager.RatedAccountManager
import kotlinx.coroutines.runBlocking

class StatusBarColorManager(
    val mainActivity: MainActivity,
    managers: List<AccountManager>
) {

    class ColorInfo(
        val color: Int = Color.TRANSPARENT,
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

    private fun recalculateStatusBarColor() {
        val colorInfo = if(enabled){
            getListOfColors().maxByOrNull { it.order } ?: ColorInfo()
        } else ColorInfo()
        mainActivity.window.statusBarColor = colorInfo.color
    }

    fun setCurrent(manager: AccountManager?) {
        current = manager?.managerName
        recalculateStatusBarColor()
    }

    private suspend fun updateBy(manager: AccountManager) {
        val name = manager.managerName
        if(!manager.isEmpty()) {
            colors[name] = ColorInfo()
            if(manager is RatedAccountManager){
                val info = manager.getSavedInfo()
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