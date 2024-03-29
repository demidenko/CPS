package com.example.test3.workers

import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import androidx.core.os.bundleOf
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.test3.*
import com.example.test3.account_manager.AtCoderAccountManager
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.account_manager.STATUS
import com.example.test3.utils.*
import kotlinx.coroutines.*

class AccountsWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val notificationManager by lazy { context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager }

    private val codeforcesAccountManager by lazy { CodeforcesAccountManager(context) }
    private val atcoderAccountManager by lazy { AtCoderAccountManager(context) }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO){
        val jobs = mutableListOf<Job>()
        with(codeforcesAccountManager.getSettings()){
            if(observeRating()) jobs.add(launch { codeforcesRating() })
            if(observeContribution()) jobs.add(launch { codeforcesContribution() })
        }
        with(atcoderAccountManager.getSettings()){
            if(observeRating()) jobs.add(launch { atcoderRating() })
        }
        if(jobs.isEmpty()) WorkersCenter.stopWorker(context, WorkersNames.accounts_parsers)
        jobs.joinAll()
        return@withContext Result.success()
    }

    private suspend fun codeforcesRating() {
        val info = codeforcesAccountManager.getSavedInfo()
        if(info.status != STATUS.OK) return

        val response = CodeforcesAPI.getUserRatingChanges(info.handle) ?: return
        if(response.status != CodeforcesAPIStatus.OK) return

        val lastRatingChange = response.result?.lastOrNull() ?: return

        codeforcesAccountManager.applyRatingChange(lastRatingChange)
    }

    private suspend fun atcoderRating() {
        val info = atcoderAccountManager.getSavedInfo()
        if(info.status != STATUS.OK) return

        val ratingChanges = AtCoderAPI.getRatingChanges(info.handle) ?: return
        val lastRatingChange = ratingChanges.lastOrNull() ?: return
        val lastRatingChangeContestID = lastRatingChange.getContestID()

        val settings = atcoderAccountManager.getSettings()
        val prevRatingChangeContestID = settings.lastRatedContestID()

        if(prevRatingChangeContestID == lastRatingChangeContestID && info.rating == lastRatingChange.NewRating) return

        settings.lastRatedContestID(lastRatingChangeContestID)

        if(prevRatingChangeContestID != null) {
            atcoderAccountManager.notifyRatingChange(info.handle, lastRatingChange)
            val newInfo = atcoderAccountManager.loadInfo(info.handle)
            if(newInfo.status!=STATUS.FAILED){
                atcoderAccountManager.setSavedInfo(newInfo)
            }else{
                atcoderAccountManager.setSavedInfo(info.copy(rating = lastRatingChange.NewRating))
            }
        }
    }

    private suspend fun codeforcesContribution() {
        val info = codeforcesAccountManager.getSavedInfo()
        if(info.status != STATUS.OK) return

        val handle = info.handle
        val contribution = codeforcesAccountManager.loadInfo(handle).let {
            if(it.status != STATUS.OK) return
            it.contribution
        }

        if(info.contribution != contribution){
            codeforcesAccountManager.setSavedInfo(info.copy(contribution = contribution))

            val oldShowedContribution: Int = if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) info.contribution
            else {
                notificationManager.activeNotifications.find {
                    it.id == NotificationIDs.codeforces_contribution_changes
                }?.notification?.extras?.getInt(keyContribution, info.contribution) ?: info.contribution
            }

            notificationBuilder(context, NotificationChannels.codeforces_contribution_changes) {
                setSubText(handle)
                setContentTitle("Contribution change: ${signedToString(oldShowedContribution)} → ${signedToString(contribution)}")
                setSmallIcon(R.drawable.ic_person)
                setSilent(true)
                setAutoCancel(true)
                setShowWhen(false)
                setContentIntent(makePendingIntentOpenURL(info.link(), context))
                addExtras(bundleOf(keyContribution to oldShowedContribution))
            }.notifyBy(notificationManager, NotificationIDs.codeforces_contribution_changes)
        }
    }

    companion object {
        private const val keyContribution = "contribution"
    }

}