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
import androidx.lifecycle.lifecycleScope
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.account_manager.*
import com.example.test3.utils.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds


abstract class AccountPanel<U: UserInfo>(
    protected val mainActivity: MainActivity,
    open val manager: AccountManager<U>
){

    private val layout = mainActivity.layoutInflater.inflate(R.layout.account_panel, null) as ConstraintLayout

    protected val textMain: TextView = layout.findViewById(R.id.account_panel_textMain)
    protected val textAdditional: TextView = layout.findViewById(R.id.account_panel_textAdditional)

    private val expandButton = layout.findViewById<ImageButton>(R.id.account_panel_expand_button).apply {
        setOnClickListener { callExpand() }
    }

    private val reloadButton = layout.findViewById<ImageButton>(R.id.account_panel_reload_button).apply {
        setOnClickListener { reload() }
    }

    companion object {
        private val startDelay = 3.seconds
        private val duration = 2.seconds
        private fun animateDelayedHide(button: ImageButton){
            button.clearAnimation()
            button.animate().setStartDelay(0).setDuration(0).alpha(1f).withEndAction {
                button.isVisible = true
                button.animate()
                    .setStartDelay(startDelay.inWholeMilliseconds)
                    .setDuration(duration.inWholeMilliseconds)
                    .alpha(0f)
                    .withEndAction {
                        button.isGone = true
                    }.start()
            }.start()
        }
    }

    fun createSmallView(): View {
        layout.setOnClickListener(object : View.OnClickListener{
            val buttons = listOf(reloadButton, expandButton)
            var lastClickTime = Instant.DISTANT_PAST
            override fun onClick(v: View) {
                buttons.forEach { button ->
                    if(button.isEnabled) animateDelayedHide(button)
                }
                val currentTime = getCurrentTime()
                if(currentTime - lastClickTime < 1.seconds / 3 && expandButton.isEnabled) {
                    callExpand()
                }
                lastClickTime = currentTime
            }
        })

        mainActivity.accountsFragment.launchAndRepeatWithViewLifecycle {
            launch { subscribeBlockedState() }
            launch { subscribeLoadingState() }
        }

        return layout
    }

    private suspend fun subscribeLoadingState() {
        mainActivity.accountsFragment.accountViewModel
            .getAccountLoadingStateFlow(manager.managerName)
            .collect { loadingState ->
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
                        reloadButton.setColorFilter(getColorFromResource(mainActivity, R.color.fail))
                    }
                }
            }
    }

    private suspend fun subscribeBlockedState() {
        mainActivity.accountsFragment.accountViewModel
            .getAccountSmallViewBlockedState(manager)
            .collect { blockedState ->
                when(blockedState){
                    BlockedState.BLOCKED -> {
                        expandButton.isEnabled = false
                        reloadButton.isEnabled = false
                    }
                    BlockedState.UNBLOCKED -> {
                        reloadButton.isEnabled = true
                        expandButton.isEnabled = true
                    }
                }
            }
    }

    suspend fun isEmpty() = manager.getSavedInfo().isEmpty()

    protected abstract fun show(info: U)

    suspend fun show(){
        if(isEmpty()){
            layout.isVisible = false
        }else {
            layout.isVisible = true
            show(manager.getSavedInfo())
        }
    }

    fun reload() = mainActivity.accountsFragment.accountViewModel.reload(manager)

    fun callExpand(){
        mainActivity.cpsFragmentManager.pushBack(
            AccountViewFragment<U>().apply {
                requireArguments().putString("manager", manager.managerName)
            }
        )
    }

    fun<U: UserInfo> showMainRated(handleView: TextView, ratingView: TextView, ratedAccountManager: RatedAccountManager<U>, info: U) {
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

    open suspend fun showBigView(fragment: AccountViewFragment<U>) {
        val view = fragment.requireView()

        val userInfoTextView = view.findViewById<TextView>(R.id.account_user_info)
        userInfoTextView.text = manager.getSavedInfo().toString()
    }

    open suspend fun createSettingsView(fragment: AccountSettingsFragment<U>){}

    suspend fun<U: UserInfo> AccountSettingsFragment<U>.appendSettingsSwitch(
        item: CPSDataStore.Item<Boolean>,
        title: String,
        description: String = "",
        onChecked: suspend () -> Unit
    ) {
        createAndAddSwitch(
            title = title,
            checked = item(),
            description = description
        ) { buttonView, isChecked ->
            lifecycleScope.launch {
                buttonView.isEnabled = false
                item(isChecked)
                if(isChecked) onChecked()
                buttonView.isEnabled = true
            }
        }
    }
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