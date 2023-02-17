package com.demich.cps.workers

import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import androidx.core.os.bundleOf
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.demich.cps.*
import com.demich.cps.accounts.managers.AtCoderAccountManager
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.accounts.managers.STATUS
import com.demich.cps.utils.AtCoderApi
import com.demich.cps.utils.codeforces.CodeforcesApi
import com.demich.cps.utils.toSignedString
import kotlin.time.Duration.Companion.minutes

class AccountsWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object {
        fun getWork(context: Context) = object : CPSWork(name = "accounts", context = context) {
            override suspend fun isEnabled() = true //TODO something proper
            override val requestBuilder: PeriodicWorkRequest.Builder
                get() = CPSPeriodicWorkRequestBuilder<AccountsWorker>(
                    repeatInterval = 15.minutes
                )
        }
    }


    private val notificationManager by lazy { context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
    private val codeforcesAccountManager by lazy { CodeforcesAccountManager(context) }
    private val atcoderAccountManager by lazy { AtCoderAccountManager(context) }

    override suspend fun runWork(): Result {
        val jobs = buildList {
            with(codeforcesAccountManager.getSettings()) {
                if (observeRating()) add(::codeforcesRating)
                if (observeContribution()) add(::codeforcesContribution)
            }
            with(atcoderAccountManager.getSettings()) {
                if (observeRating()) add(::atcoderRating)
            }
        }

        jobs.joinAllWithProgress()

        return Result.success()
    }

    private suspend fun codeforcesRating() {
        val userInfo = codeforcesAccountManager.getSavedInfo()
        if (userInfo.status != STATUS.OK) return

        val lastRatingChange = CodeforcesApi.runCatching {
            getUserRatingChanges(handle = userInfo.handle)
        }.getOrNull()?.lastOrNull() ?: return

        codeforcesAccountManager.applyRatingChange(lastRatingChange)
    }

    private suspend fun codeforcesContribution() {
        val userInfo = codeforcesAccountManager.getSavedInfo()
        if (userInfo.status != STATUS.OK) return

        val handle = userInfo.handle
        val newContribution = codeforcesAccountManager.loadInfo(handle)
            .takeIf { it.status == STATUS.OK }
            ?.contribution ?: return

        if (newContribution == userInfo.contribution) return

        codeforcesAccountManager.setSavedInfo(userInfo.copy(contribution = newContribution))

        val oldContribution = getNotifiedCodeforcesContribution() ?: userInfo.contribution

        notificationBuilder(context, NotificationChannels.codeforces.contribution_changes) {
            setSubText(handle)
            setContentTitle("Contribution change: ${oldContribution.toSignedString()} → ${newContribution.toSignedString()}")
            setSmallIcon(R.drawable.ic_person)
            setSilent(true)
            setAutoCancel(true)
            setShowWhen(false)
            attachUrl(url = userInfo.link(), context = context)
            addExtras(bundleOf(KEY_CF_CONTRIBUTION to oldContribution))
        }.notifyBy(notificationManager, NotificationIds.codeforces_contribution_changes)
    }

    private suspend fun atcoderRating() {
        val userInfo = atcoderAccountManager.getSavedInfo()
        if (userInfo.status != STATUS.OK) return

        val lastRatingChange = AtCoderApi.runCatching {
            getRatingChanges(handle = userInfo.handle)
        }.getOrNull()?.lastOrNull() ?: return

        val lastRatingChangeContestId = lastRatingChange.getContestId()

        val settings = atcoderAccountManager.getSettings()
        val prevRatingChangeContestId = settings.lastRatedContestId()

        if (prevRatingChangeContestId == lastRatingChangeContestId && userInfo.rating == lastRatingChange.NewRating) return

        settings.lastRatedContestId(lastRatingChangeContestId)

        if (prevRatingChangeContestId != null) {
            atcoderAccountManager.notifyRatingChange(userInfo.handle, lastRatingChange)
            val newInfo = atcoderAccountManager.loadInfo(userInfo.handle)
            if (newInfo.status != STATUS.FAILED) {
                atcoderAccountManager.setSavedInfo(newInfo)
            } else {
                atcoderAccountManager.setSavedInfo(userInfo.copy(rating = lastRatingChange.NewRating))
            }
        }
    }

    private val KEY_CF_CONTRIBUTION = "cf_contribution"
    private fun getNotifiedCodeforcesContribution(): Int? {
        return notificationManager.activeNotifications.find {
            it.id == NotificationIds.codeforces_contribution_changes
        }?.notification?.extras?.getInt(KEY_CF_CONTRIBUTION)
    }
}