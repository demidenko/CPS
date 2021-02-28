package com.example.test3.account_view

import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.account_manager.AtCoderAccountManager
import com.example.test3.account_manager.UserInfo
import com.example.test3.workers.WorkersCenter
import kotlinx.coroutines.launch

class AtCoderAccountPanel(
    mainActivity: MainActivity,
    override val manager: AtCoderAccountManager
): AccountPanel(mainActivity, manager)  {

    override val homeURL = "https://atcoder.jp"

    override fun show(info: UserInfo) {
        showMainRated(textMain, textAdditional, manager, info)
    }

    override val bigViewResource = R.layout.fragment_account_view_atcoder

    override suspend fun showBigView(fragment: AccountViewFragment) {
        val view = fragment.requireView()

        val info = manager.getSavedInfo() as AtCoderAccountManager.AtCoderUserInfo

        val handleView = view.findViewById<TextView>(R.id.account_view_handle)
        val ratingView = view.findViewById<TextView>(R.id.account_view_rating)

        showMainRated(handleView, ratingView, manager, info)

        RatingGraphView.showInAccountViewFragment(fragment, manager)
    }

    override suspend fun createSettingsView(fragment: AccountSettingsFragment) {
        manager.apply {
            fragment.createAndAddSwitch(
                "Rating changes observer",
                getSettings().getObserveRating()
            ){ buttonView, isChecked ->
                fragment.lifecycleScope.launch {
                    buttonView.isEnabled = false
                    getSettings().setObserveRating(isChecked)
                    if (isChecked) {
                        WorkersCenter.startAccountsWorker(mainActivity)
                    }
                    buttonView.isEnabled = true
                }
            }
        }
    }

}