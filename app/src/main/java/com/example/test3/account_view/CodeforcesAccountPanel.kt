package com.example.test3.account_view

import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.account_manager.NOT_RATED
import com.example.test3.account_manager.STATUS
import com.example.test3.account_manager.UserInfo
import com.example.test3.getColorFromResource
import com.example.test3.job_services.JobServicesCenter
import com.example.test3.utils.signedToString
import kotlinx.coroutines.launch

class CodeforcesAccountPanel(
    mainActivity: MainActivity,
    override val manager: CodeforcesAccountManager
): AccountPanel(mainActivity, manager) {

    override fun show(info: UserInfo) {
        showMainRated(textMain, textAdditional, manager, info)
    }

    override val bigViewResource = R.layout.fragment_account_view_codeforces

    override suspend fun showBigView(fragment: AccountViewFragment) {
        val view = fragment.requireView()

        val info = manager.getSavedInfo() as CodeforcesAccountManager.CodeforcesUserInfo

        val handleView = view.findViewById<TextView>(R.id.account_view_handle)
        val ratingView = view.findViewById<TextView>(R.id.account_view_rating)
        val contributionView = view.findViewById<TextView>(R.id.account_view_cf_contribution)
        val ratingGraphView = view.findViewById<RatingGraphView>(R.id.account_view_rating_graph)

        showMainRated(handleView, ratingView, manager, info)

        contributionView.apply {
            val contributionViewTitle = view.findViewById<TextView>(R.id.account_view_cf_contribution_title)
            if(info.contribution == 0){
                visibility = View.GONE
                contributionViewTitle.visibility = View.GONE
            }else {
                visibility = View.VISIBLE
                contributionViewTitle.visibility = View.VISIBLE
                text = signedToString(info.contribution)
                if (info.contribution > 0) {
                    setTextColor(getColorFromResource(mainActivity, R.color.blog_rating_positive))
                } else {
                    setTextColor(getColorFromResource(mainActivity, R.color.blog_rating_negative))
                }
            }
        }

        fragment.lifecycleScope.launch {
            ratingGraphView.setManager(manager)
            if(info.status!=STATUS.OK || info.rating== NOT_RATED) return@launch
            val history = manager.getRatingHistory(info) ?: return@launch
            ratingGraphView.apply {
                setHistory(history)
                visibility = View.VISIBLE
            }
        }
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
                        JobServicesCenter.startAccountsJobService(mainActivity)
                    }
                    buttonView.isEnabled = true
                }
            }

            fragment.createAndAddSwitch(
                "Contribution changes observer",
                getSettings().getObserveContribution()
            ){ buttonView, isChecked ->
                fragment.lifecycleScope.launch {
                    buttonView.isEnabled = false
                    getSettings().setObserveContribution(isChecked)
                    if (isChecked) {
                        JobServicesCenter.startAccountsJobService(mainActivity)
                    }
                    buttonView.isEnabled = true
                }
            }

            fragment.createAndAddSwitch(
                "Contest watcher",
                getSettings().getContestWatchEnabled(),
                fragment.getString(R.string.cf_contest_watcher_description)
            ){ buttonView, isChecked ->
                fragment.lifecycleScope.launch {
                    buttonView.isEnabled = false
                    getSettings().setContestWatchEnabled(isChecked)
                    if (isChecked) {
                        JobServicesCenter.startCodeforcesContestWatchStarterJobService(mainActivity)
                    }
                    buttonView.isEnabled = true
                }
            }
        }

    }

}