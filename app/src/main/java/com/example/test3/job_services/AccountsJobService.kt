package com.example.test3.job_services

import android.app.NotificationManager
import android.os.Bundle
import com.example.test3.*
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.account_manager.STATUS
import com.example.test3.utils.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AccountsJobService : CoroutineJobService() {

    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }

    private val codeforcesAccountManager by lazy { CodeforcesAccountManager(this) }

    override suspend fun makeJobs(): ArrayList<Job> {
        val jobs = arrayListOf<Job>()
        with(codeforcesAccountManager.getSettings()){
            if(getObserveRating()) jobs.add(launch { codeforcesRating() })
            if(getObserveContribution()) jobs.add(launch { codeforcesContribution() })
        }
        if(jobs.isEmpty()) JobServicesCenter.stopJobService(this, JobServiceIDs.accounts_parsers)
        return jobs
    }

    private suspend fun codeforcesRating() {
        val info = codeforcesAccountManager.getSavedInfo() as CodeforcesAccountManager.CodeforcesUserInfo
        if(info.status != STATUS.OK) return

        val response = CodeforcesAPI.getUserRatingChanges(info.handle) ?: return
        if(response.status != CodeforcesAPIStatus.OK) return

        val lastRatingChange = response.result?.last() ?: return
        val prevRatingChangeContestID = codeforcesAccountManager.getSettings().getLastRatedContestID()

        if(prevRatingChangeContestID == lastRatingChange.contestId && info.rating == lastRatingChange.newRating) return

        codeforcesAccountManager.getSettings().setLastRatedContestID(lastRatingChange.contestId)

        if(prevRatingChangeContestID!=-1){
            CodeforcesUtils.notifyRatingChange(lastRatingChange, this, notificationManager, codeforcesAccountManager)
            val newInfo = codeforcesAccountManager.loadInfo(info.handle)
            if(newInfo.status!=STATUS.FAILED){
                codeforcesAccountManager.setSavedInfo(newInfo)
            }else{
                codeforcesAccountManager.setSavedInfo(info.copy(rating = lastRatingChange.newRating))
            }
        }
    }

    private suspend fun codeforcesContribution() {
        val info = codeforcesAccountManager.getSavedInfo() as CodeforcesAccountManager.CodeforcesUserInfo
        if(info.status != STATUS.OK) return

        val handle = info.handle
        val contribution = (codeforcesAccountManager.loadInfo(handle) as CodeforcesAccountManager.CodeforcesUserInfo).let {
            if(it.status != STATUS.OK) return
            it.contribution
        }

        if(info.contribution != contribution){
            codeforcesAccountManager.setSavedInfo(info.copy(contribution = contribution))

            val oldShowedContribution: Int = if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) info.contribution
            else {
                notificationManager.activeNotifications.find {
                    it.id == NotificationIDs.codeforces_contribution_changes
                }?.notification?.extras?.getInt("contribution", info.contribution) ?: info.contribution
            }

            val n = notificationBuilder(this, NotificationChannels.codeforces_contribution_changes).apply {
                setSubText(handle)
                setContentTitle("Contribution change: ${signedToString(oldShowedContribution)} → ${signedToString(contribution)}")
                setSmallIcon(R.drawable.ic_person)
                setNotificationSilent()
                setAutoCancel(true)
                setShowWhen(false)
                setContentIntent(makePendingIntentOpenURL(info.link(), this@AccountsJobService))
                extras = Bundle().apply {
                    putInt("contribution", oldShowedContribution)
                }
            }
            notificationManager.notify( NotificationIDs.codeforces_contribution_changes, n.build())
        }
    }

}