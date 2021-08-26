package com.example.test3.account_view

import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.lifecycle.lifecycleScope
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.account_manager.CodeforcesUserInfo
import com.example.test3.utils.CodeforcesUtils
import com.example.test3.workers.WorkersCenter
import kotlinx.coroutines.launch

class CodeforcesAccountPanel(
    mainActivity: MainActivity,
    override val manager: CodeforcesAccountManager
): AccountPanel<CodeforcesUserInfo>(mainActivity, manager) {

    override fun show(info: CodeforcesUserInfo) {
        showMainRated(textMain, textAdditional, manager, info)
    }

    override val bigViewResource = R.layout.fragment_account_view_codeforces

    override suspend fun showBigView(fragment: AccountViewFragment<CodeforcesUserInfo>) {
        val view = fragment.requireView()

        val info = manager.getSavedInfo()

        val handleView = view.findViewById<TextView>(R.id.account_view_handle)
        val ratingView = view.findViewById<TextView>(R.id.account_view_rating)

        showMainRated(handleView, ratingView, manager, info)

        CodeforcesUtils.setVotedView(
            rating = info.contribution,
            ratingTextView = view.findViewById<TextView>(R.id.account_view_cf_contribution_value),
            ratingGroupView = view.findViewById<Group>(R.id.account_view_cf_contribution),
            context = mainActivity
        )

        RatingGraphView.showInAccountViewFragment(fragment, manager)

    }

    override suspend fun createSettingsView(fragment: AccountSettingsFragment<CodeforcesUserInfo>) {
        manager.apply {
            fragment.createAndAddSwitch(
                "Rating changes observer",
                getSettings().observeRating()
            ){ buttonView, isChecked ->
                fragment.lifecycleScope.launch {
                    buttonView.isEnabled = false
                    getSettings().observeRating(isChecked)
                    if (isChecked) {
                        WorkersCenter.startAccountsWorker(mainActivity)
                    }
                    buttonView.isEnabled = true
                }
            }

            fragment.createAndAddSwitch(
                "Contribution changes observer",
                getSettings().observeContribution()
            ){ buttonView, isChecked ->
                fragment.lifecycleScope.launch {
                    buttonView.isEnabled = false
                    getSettings().observeContribution(isChecked)
                    if (isChecked) {
                        WorkersCenter.startAccountsWorker(mainActivity)
                    }
                    buttonView.isEnabled = true
                }
            }

            fragment.createAndAddSwitch(
                "Contest watcher",
                getSettings().contestWatchEnabled(),
                fragment.getString(R.string.cf_contest_watcher_description)
            ){ buttonView, isChecked ->
                fragment.lifecycleScope.launch {
                    buttonView.isEnabled = false
                    getSettings().contestWatchEnabled(isChecked)
                    if (isChecked) {
                        WorkersCenter.startCodeforcesContestWatchLauncherWorker(mainActivity)
                    }
                    buttonView.isEnabled = true
                }
            }

            fragment.createAndAddSwitch(
                "Upsolving suggestions",
                getSettings().upsolvingSuggestionsEnabled()
            ){ buttonView, isChecked ->
                fragment.lifecycleScope.launch {
                    buttonView.isEnabled = false
                    getSettings().upsolvingSuggestionsEnabled(isChecked)
                    if (isChecked) {
                        WorkersCenter.startCodeforcesUpsolveSuggestionsWorker(mainActivity)
                    }
                    buttonView.isEnabled = true
                }
            }
        }

    }

}