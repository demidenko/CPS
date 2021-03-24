package com.example.test3.ui

import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.test3.MainActivity
import com.example.test3.account_manager.AccountManager
import com.example.test3.account_manager.RatedAccountManager
import com.example.test3.account_manager.UserInfo
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
        checkManagers(managers)
        with(mainActivity){
            managers.forEach {
                it.getInfoLiveData().observe(this){ (manager, info) ->
                    updateBy(manager, info)
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

    private suspend fun updateBy(manager: AccountManager) = updateBy(manager, manager.getSavedInfo())
    private fun updateBy(manager: AccountManager, info: UserInfo) {
        val name = manager.managerName
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

    private fun checkManagers(managers: List<AccountManager>) {
        //check data file names
        if(managers.map { it.managerName }.distinct().size != managers.size)
            throw Exception("Not different managers names")

        //check ranks of colors
        managers.filterIsInstance<RatedAccountManager>().apply {
            distinctBy { ratedManager ->
                val colors = ratedManager.rankedHandleColorsList
                colors.forEachIndexed { index, handleColor ->
                    ratedManager.getColor(handleColor)
                    if(index > 0 && handleColor < colors[index-1])
                        throw Exception("${ratedManager.managerName}: color list is not sorted")
                }
                colors.size
            }.apply {
                if(size != 1) throw Exception("different sizes for color lists")
            }
        }
    }
}