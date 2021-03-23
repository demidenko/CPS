package com.example.test3.ui

import android.graphics.Color
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.test3.AccountsFragment
import com.example.test3.R
import com.example.test3.account_manager.AccountManager
import com.example.test3.account_manager.RatedAccountManager

class StatusBarColorManager(
    private val accountsFragment: AccountsFragment
) {

    data class ColorInfo(
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
        with(accountsFragment.mainActivity){
            settingsUI.useStatusBarLiveData.observe(this){ use ->
                enabled = use
                updateStatusBar()
            }
        }
    }

    private fun updateStatusBar() {

        val (best, allEmpty) =
            getListOfColors().maxByOrNull { it.order }
                ?.let { it to false }
                ?: ColorInfo() to true

        with(accountsFragment){
            view?.findViewById<TextView>(R.id.accounts_welcome_text)?.isVisible = allEmpty
            mainActivity.window.statusBarColor = (if(enabled) best else ColorInfo()).color
        }
    }

    fun setCurrent(manager: AccountManager?) {
        current = manager?.managerName
        updateStatusBar()
    }

    suspend fun updateBy(manager: AccountManager) {
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
        updateStatusBar()
    }
}