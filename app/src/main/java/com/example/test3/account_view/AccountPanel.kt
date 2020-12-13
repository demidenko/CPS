package com.example.test3.account_view

import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.account_manager.AccountManager
import com.example.test3.account_manager.STATUS
import com.example.test3.account_manager.UserInfo
import com.example.test3.getColorFromResource
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


abstract class AccountPanel(
    protected val mainActivity: MainActivity,
    open val manager: AccountManager
){
    private val layout = mainActivity.layoutInflater.inflate(R.layout.account_panel, null) as ConstraintLayout

    protected val textMain: TextView = layout.findViewById(R.id.account_panel_textMain)
    protected val textAdditional: TextView = layout.findViewById(R.id.account_panel_textAdditional)

    private val expandButton = layout.findViewById<ImageButton>(R.id.account_panel_expand_button).apply {
        setOnClickListener { callExpand() }
    }

    private val reloadButton = layout.findViewById<ImageButton>(R.id.account_panel_reload_button).apply {
        setOnClickListener {
            mainActivity.accountsFragment.lifecycleScope.launch { reload() }
        }
    }


    fun createSmallView(): View {
        layout.setOnClickListener {
            val startDelay = TimeUnit.SECONDS.toMillis(3)
            val duration = TimeUnit.SECONDS.toMillis(2)
            listOf(reloadButton, expandButton).forEach { button ->
                if(button.isEnabled) {
                    button.clearAnimation()
                    button.animate().setStartDelay(0).setDuration(0).alpha(1f).withEndAction {
                        button.visibility = View.VISIBLE
                        button.animate().setStartDelay(startDelay).setDuration(duration).alpha(0f)
                            .withEndAction {
                                button.visibility = View.GONE
                            }.start()
                    }.start()
                }
            }
        }

        return layout
    }

    suspend fun isEmpty() = manager.getSavedInfo().userID.isBlank()

    protected abstract fun show(info: UserInfo)

    suspend fun show(){
        if(isEmpty()){
            layout.visibility = View.GONE
        }else {
            layout.visibility = View.VISIBLE
            show(manager.getSavedInfo())
        }
        mainActivity.accountsFragment.updateUI()
    }

    fun isBlocked(): Boolean = !reloadButton.isEnabled

    fun block(){
        expandButton.isEnabled = false
        reloadButton.isEnabled = false
        mainActivity.accountsFragment.sharedReloadButton.startReload(manager.PREFERENCES_FILE_NAME)
    }

    fun unblock(){
        mainActivity.accountsFragment.sharedReloadButton.stopReload(manager.PREFERENCES_FILE_NAME)
        reloadButton.isEnabled = true
        expandButton.isEnabled = true
    }

    suspend fun reload(){
        if(isEmpty()) return

        block()

        expandButton.animate().setStartDelay(0).alpha(0f).setDuration(0).withEndAction {
            expandButton.visibility = View.GONE
        }.start()

        reloadButton.animate().setStartDelay(0).alpha(1f).setDuration(0).withStartAction {
            reloadButton.setColorFilter(mainActivity.defaultTextColor)
            reloadButton.visibility = View.VISIBLE
        }.start()
        reloadButton.startAnimation(rotateAnimation)


        val savedInfo = manager.getSavedInfo()
        val info = manager.loadInfo(savedInfo.userID)

        if(info.status != STATUS.FAILED){
            if(info!=savedInfo) manager.setSavedInfo(info)
            reloadButton.animate().setStartDelay(0).setDuration(1000).alpha(0f).withEndAction {
                reloadButton.clearAnimation()
                reloadButton.visibility = View.GONE
            }.start()
        }else{
            reloadButton.clearAnimation()
            reloadButton.setColorFilter(getColorFromResource(mainActivity, R.color.reload_fail))
        }

        unblock()
    }

    fun callExpand(){
        mainActivity.supportFragmentManager.beginTransaction()
            .hide(mainActivity.accountsFragment)
            .add(android.R.id.content, AccountViewFragment().apply {
                arguments = Bundle().apply { putString("manager", manager.PREFERENCES_FILE_NAME) }
            }, AccountViewFragment.tag)
            .addToBackStack(null)
            .commit()
    }

    open val bigViewResource: Int = R.layout.fragment_account_view

    open suspend fun showBigView(fragment: AccountViewFragment) {
        val view = fragment.requireView()

        val userInfoTextView = view.findViewById<TextView>(R.id.account_user_info)
        userInfoTextView.text = manager.getSavedInfo().toString()
    }

    open suspend fun createSettingsView(fragment: AccountSettingsFragment){

    }
}

val rotateAnimation = RotateAnimation(
    0f,
    360f,
    Animation.RELATIVE_TO_SELF,
    0.5f,
    Animation.RELATIVE_TO_SELF,
    0.5f
).apply {
    duration = 1000
    repeatCount = Animation.INFINITE
    repeatMode = Animation.INFINITE
    interpolator = LinearInterpolator()
}