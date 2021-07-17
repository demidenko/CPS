package com.example.test3.ui

import android.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.test3.MainActivity
import com.example.test3.account_manager.AccountManager
import com.example.test3.account_manager.RatedAccountManager
import com.example.test3.account_manager.UserInfo
import com.example.test3.utils.ignoreFirst
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val NO_COLOR = Color.TRANSPARENT

class StatusBarColorManager(
    mainActivity: MainActivity,
    managers: List<AccountManager<out UserInfo>>
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

    private val statusBarColor = MutableStateFlow<Int>(NO_COLOR)
    fun getStatusBarColorFlow() = statusBarColor.asStateFlow()

    private var enabled: Boolean = true
    init {
        checkManagers(managers)
        with(mainActivity) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    managers.forEach {
                        it.flowOfInfo().onEach { (manager, info) ->
                            updateBy(manager)
                        }.launchIn(this)
                    }
                    settingsUI.useStatusBar.flow.onEach { use ->
                        enabled = use
                        recalculateStatusBarColor()
                    }.launchIn(this)
                    settingsUI.userRealColors.flow.ignoreFirst().onEach { use ->
                        managers.forEach { updateBy(it) }
                    }.launchIn(this)
                }
            }
        }
    }

    private fun recalculateStatusBarColor() {
        val color = if(enabled){
            getListOfColors().maxByOrNull { it.order }?.color ?: NO_COLOR
        } else NO_COLOR
        statusBarColor.value = color
    }

    fun setCurrent(manager: AccountManager<*>?) {
        current = manager?.managerName
        recalculateStatusBarColor()
    }

    private suspend fun<U: UserInfo> updateBy(manager: AccountManager<U>) = updateBy(manager, manager.getSavedInfo())
    private fun<U: UserInfo> updateBy(manager: AccountManager<U>, info: U) {
        val name = manager.managerName
        if(!info.isEmpty()) {
            colors[name] =
                (manager as? RatedAccountManager)?.getColor(info)?.let {
                    ColorInfo(it, manager.getOrder(info))
                } ?: ColorInfo()
        } else {
            colors.remove(name)
        }
        recalculateStatusBarColor()
    }


    companion object {
        private fun checkManagers(managers: List<AccountManager<out UserInfo>>) {
            //check data file names
            if(managers.map { it.managerName }.distinct().size != managers.size)
                throw Exception("Not different managers names")

            //check ranks of colors
            managers.filterIsInstance<RatedAccountManager<*>>().apply {
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
}