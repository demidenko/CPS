package com.example.test3.account_view

import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.account_manager.AtCoderAccountManager
import com.example.test3.workers.WorkersCenter
import kotlinx.coroutines.launch

class AtCoderAccountPanel(
    mainActivity: MainActivity,
    override val manager: AtCoderAccountManager
): AccountPanel<AtCoderAccountManager.AtCoderUserInfo>(mainActivity, manager)  {

    override fun show(info: AtCoderAccountManager.AtCoderUserInfo) {
        showMainRated(textMain, textAdditional, manager, info)
    }

    override val bigViewResource = R.layout.fragment_account_view_atcoder

    override suspend fun showBigView(fragment: AccountViewFragment<AtCoderAccountManager.AtCoderUserInfo>) {
        val view = fragment.requireView()

        val info = manager.getSavedInfo()

        val handleView = view.findViewById<TextView>(R.id.account_view_handle)
        val ratingView = view.findViewById<TextView>(R.id.account_view_rating)

        showMainRated(handleView, ratingView, manager, info)

        RatingGraphView.showInAccountViewFragment(fragment, manager)
    }

    override suspend fun createSettingsView(fragment: AccountSettingsFragment<AtCoderAccountManager.AtCoderUserInfo>) {
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