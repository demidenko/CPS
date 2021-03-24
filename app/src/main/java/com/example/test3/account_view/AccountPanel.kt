package com.example.test3.account_view

import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.distinctUntilChanged
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.account_manager.*
import com.example.test3.getColorFromResource
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
        setOnClickListener { mainActivity.accountsFragment.accountViewModel.reload(manager) }
    }

    abstract val homeURL: String

    companion object {
        private val startDelayMillis = TimeUnit.SECONDS.toMillis(3)
        private val durationMillis = TimeUnit.SECONDS.toMillis(2)
        private fun animateDelayedHide(button: ImageButton){
            button.clearAnimation()
            button.animate().setStartDelay(0).setDuration(0).alpha(1f).withEndAction {
                button.isVisible = true
                button.animate().setStartDelay(startDelayMillis).setDuration(durationMillis).alpha(0f)
                    .withEndAction {
                        button.isGone = true
                    }.start()
            }.start()
        }
    }

    fun createSmallView(): View {
        layout.setOnClickListener(object : View.OnClickListener{
            val buttons = listOf(reloadButton, expandButton)
            var lastClickMillis: Long = 0
            override fun onClick(v: View) {
                buttons.forEach { button ->
                    if(button.isEnabled) animateDelayedHide(button)
                }
                val currentTimeMillis = System.currentTimeMillis()
                if(currentTimeMillis - lastClickMillis < 333 && expandButton.isEnabled){
                    callExpand()
                }
                lastClickMillis = currentTimeMillis
            }
        })

        val failColor = getColorFromResource(mainActivity, R.color.fail)
        mainActivity.accountsFragment.accountViewModel
            .getAccountLoadingStateLiveData(manager.managerName).distinctUntilChanged()
            .observe(mainActivity.accountsFragment){ loadingState ->
                loadingState ?: return@observe
                if(loadingState == LoadingState.LOADING) block() else unblock()
                when(loadingState) {
                    LoadingState.PENDING -> {
                        reloadButton.animate().setStartDelay(0).setDuration(1000).alpha(0f).withEndAction {
                            reloadButton.clearAnimation()
                            reloadButton.isGone = true
                        }.start()
                    }
                    LoadingState.LOADING -> {
                        expandButton.animate().setStartDelay(0).alpha(0f).setDuration(0).withEndAction {
                            expandButton.isGone = true
                        }.start()
                        reloadButton.animate().setStartDelay(0).alpha(1f).setDuration(0).withStartAction {
                            reloadButton.clearColorFilter()
                            reloadButton.isVisible = true
                        }.start()
                        reloadButton.startAnimation(rotateAnimation)
                    }
                    LoadingState.FAILED -> {
                        reloadButton.clearAnimation()
                        reloadButton.setColorFilter(failColor)
                    }
                }
            }

        return layout
    }

    suspend fun isEmpty() = manager.getSavedInfo().isEmpty()

    protected abstract fun show(info: UserInfo)

    suspend fun show(){
        if(isEmpty()){
            layout.isVisible = false
        }else {
            layout.isVisible = true
            show(manager.getSavedInfo())
        }
    }

    fun isBlocked(): Boolean = !reloadButton.isEnabled

    fun block(){
        expandButton.isEnabled = false
        reloadButton.isEnabled = false
        mainActivity.accountsFragment.sharedReloadButton.startReload(manager.managerName)
    }

    fun unblock(){
        mainActivity.accountsFragment.sharedReloadButton.stopReload(manager.managerName)
        reloadButton.isEnabled = true
        expandButton.isEnabled = true
    }

    fun reload() = reloadButton.callOnClick()

    fun callExpand(){
        mainActivity.cpsFragmentManager.pushBack(
            AccountViewFragment().apply {
                requireArguments().putString("manager", manager.managerName)
            }
        )
    }

    fun showMainRated(handleView: TextView, ratingView: TextView, ratedAccountManager: RatedAccountManager, info: UserInfo) {
        val color = ratedAccountManager.getColor(info) ?: mainActivity.defaultTextColor
        handleView.text = ratedAccountManager.makeSpan(info)
        ratingView.setTextColor(color)
        if(info.status == STATUS.OK){
            val rating = ratedAccountManager.getRating(info)
            ratingView.text = if(rating == NOT_RATED) "[not rated]" else "$rating"
        }else{
            ratingView.text = ""
        }
    }

    open val bigViewResource: Int = R.layout.fragment_account_view

    open suspend fun showBigView(fragment: AccountViewFragment) {
        val view = fragment.requireView()

        val userInfoTextView = view.findViewById<TextView>(R.id.account_user_info)
        userInfoTextView.text = manager.getSavedInfo().toString()
    }

    open suspend fun createSettingsView(fragment: AccountSettingsFragment){}

}

private val rotateAnimation = RotateAnimation(
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