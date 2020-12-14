package com.example.test3.account_view

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import androidx.lifecycle.lifecycleScope
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.account_manager.NOT_RATED
import com.example.test3.account_manager.STATUS
import com.example.test3.account_manager.UserInfo
import com.example.test3.getColorFromResource
import com.example.test3.job_services.JobServicesCenter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CodeforcesAccountPanel(
    mainActivity: MainActivity,
    override val manager: CodeforcesAccountManager
): AccountPanel(mainActivity, manager) {

    override fun show(info: UserInfo) { info as CodeforcesAccountManager.CodeforcesUserInfo
        val color = manager.getColor(info) ?: mainActivity.defaultTextColor
        textMain.text = manager.makeSpan(info)
        textAdditional.text = ""
        textAdditional.setTextColor(color)
        if(info.status == STATUS.OK){
            textAdditional.text = if(info.rating == NOT_RATED) "[not rated]" else "${info.rating}"
        }
    }

    override val bigViewResource = R.layout.fragment_account_view_codeforces

    override suspend fun showBigView(fragment: AccountViewFragment) {
        val view = fragment.requireView()

        val info = manager.getSavedInfo() as CodeforcesAccountManager.CodeforcesUserInfo
        val color = manager.getColor(info) ?: mainActivity.defaultTextColor

        val handleView = view.findViewById<TextView>(R.id.account_view_handle)
        val ratingView = view.findViewById<TextView>(R.id.account_view_rating)
        val contributionView = view.findViewById<TextView>(R.id.account_view_cf_contribution)

        handleView.apply {
            text = manager.makeSpan(info)
        }

        ratingView.apply {
            setTextColor(color)
            if(info.status == STATUS.OK){
                text = if(info.rating == NOT_RATED) "[not rated]" else "${info.rating}"
            } else {
                text = ""
            }
        }

        contributionView.apply {
            val contributionViewTitle = view.findViewById<TextView>(R.id.account_view_cf_contribution_title)
            if(info.contribution == 0){
                visibility = View.GONE
                contributionViewTitle.visibility = View.GONE
            }else {
                visibility = View.VISIBLE
                contributionViewTitle.visibility = View.VISIBLE
                if (info.contribution > 0) {
                    text = "+${info.contribution}"
                    setTextColor(getColorFromResource(mainActivity, R.color.blog_rating_positive))
                } else {
                    text = "${info.contribution}"
                    setTextColor(getColorFromResource(mainActivity, R.color.blog_rating_positive))
                }
            }
        }
    }

    companion object {
        fun getDataStore(context: Context) = CodeforcesSettingsDataStore(context)
    }

    override suspend fun createSettingsView(fragment: AccountSettingsFragment) {

        getDataStore(mainActivity).apply {
            fragment.createAndAddSwitch(
                "Observe rating changes",
                getObserveRating()
            ){ buttonView, isChecked ->
                fragment.lifecycleScope.launch {
                    buttonView.isEnabled = false
                    getDataStore(mainActivity).setObserveRating(isChecked)
                    if (isChecked) {
                        JobServicesCenter.startAccountsJobService(mainActivity)
                    }
                    buttonView.isEnabled = true
                }
            }

            fragment.createAndAddSwitch(
                "Observe contribution changes",
                getObserveContribution()
            ){ buttonView, isChecked ->
                fragment.lifecycleScope.launch {
                    buttonView.isEnabled = false
                    getDataStore(mainActivity).setObserveContribution(isChecked)
                    if (isChecked) {
                        JobServicesCenter.startAccountsJobService(mainActivity)
                    }
                    buttonView.isEnabled = true
                }
            }
        }

    }

    override suspend fun resetRelatedData() {
        with(getDataStore(mainActivity)){
            setLastRatedContestID(-1)
        }
    }

    class CodeforcesSettingsDataStore(context: Context) {
        private val dataStore = context.createDataStore(name = "settings_account_codeforces")
        companion object {
            private val KEY_OBS_RATING = preferencesKey<Boolean>("settings_account_codeforces_rating")
            private val KEY_LAST_RATED_CONTEST = preferencesKey<Int>("settings_account_codeforces_last_rated_contest")
            private val KEY_OBS_CONTRIBUTION = preferencesKey<Boolean>("settings_account_codeforces_contribution")
        }

        suspend fun getObserveRating() = dataStore.data.first()[KEY_OBS_RATING] ?: false
        suspend fun setObserveRating(flag: Boolean){
            dataStore.edit { it[KEY_OBS_RATING] = flag }
        }

        suspend fun getLastRatedContestID() = dataStore.data.first()[KEY_LAST_RATED_CONTEST] ?: -1
        suspend fun setLastRatedContestID(contestID: Int){
            dataStore.edit { it[KEY_LAST_RATED_CONTEST] = contestID }
        }

        suspend fun getObserveContribution() = dataStore.data.first()[KEY_OBS_CONTRIBUTION] ?: false
        suspend fun setObserveContribution(flag: Boolean){
            dataStore.edit { it[KEY_OBS_CONTRIBUTION] = flag }
        }
    }

}