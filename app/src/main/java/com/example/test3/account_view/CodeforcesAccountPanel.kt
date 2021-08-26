package com.example.test3.account_view

import android.widget.TextView
import androidx.constraintlayout.widget.Group
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.account_manager.CodeforcesUserInfo
import com.example.test3.utils.CodeforcesUtils
import com.example.test3.workers.WorkersCenter

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
        with(manager.getSettings()) {
            fragment.appendSettingsSwitch(
                observeRating,
                title = "Rating changes observer"
            ) { WorkersCenter.startAccountsWorker(mainActivity) }
            fragment.appendSettingsSwitch(
                observeContribution,
                title = "Contribution changes observer"
            ) { WorkersCenter.startAccountsWorker(mainActivity) }
            fragment.appendSettingsSwitch(
                contestWatchEnabled,
                title = "Contest watcher",
                description = fragment.getString(R.string.cf_contest_watcher_description)
            ) { WorkersCenter.startCodeforcesContestWatchLauncherWorker(mainActivity) }
            fragment.appendSettingsSwitch(
                upsolvingSuggestionsEnabled,
                title = "Upsolving suggestions"
            ) { WorkersCenter.startCodeforcesUpsolveSuggestionsWorker(mainActivity) }
        }
    }

}