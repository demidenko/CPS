package com.demich.cps.workers

import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import androidx.core.os.bundleOf
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.demich.cps.R
import com.demich.cps.accounts.managers.AtCoderAccountManager
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.notifications.attachUrl
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.platforms.api.AtCoderApi
import com.demich.cps.platforms.api.CodeforcesApi
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils
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

    private val codeforcesAccountManager by lazy { CodeforcesAccountManager(context) }
    private val atcoderAccountManager by lazy { AtCoderAccountManager(context) }

    override suspend fun runWork(): Result {
        val jobs = buildList {
            with(codeforcesAccountManager.getSettings(context)) {
                if (observeRating()) add(::codeforcesRating)
                if (observeContribution()) add(::codeforcesContribution)
            }
            with(atcoderAccountManager.getSettings(context)) {
                if (observeRating()) add(::atcoderRating)
            }
        }

        jobs.joinAllWithProgress()

        return Result.success()
    }

    private suspend fun codeforcesRating() {
        val userInfo = codeforcesAccountManager.dataStore(context).getSavedInfo() ?: return
        if (userInfo.status != STATUS.OK) return

        val lastRatingChange = CodeforcesApi.runCatching {
            getUserRatingChanges(handle = userInfo.handle)
        }.getOrNull()?.lastOrNull() ?: return

        codeforcesAccountManager.applyRatingChange(lastRatingChange, context)
    }

    private suspend fun codeforcesContribution() {
        val dataStore = codeforcesAccountManager.dataStore(context)
        val userInfo = dataStore.getSavedInfo() ?: return
        if (userInfo.status != STATUS.OK) return

        val handle = userInfo.handle
        val newContribution = CodeforcesUtils.getUserInfo(handle = handle, doRedirect = false)
            .takeIf { it.status == STATUS.OK }
            ?.contribution ?: return

        if (newContribution == userInfo.contribution) return

        dataStore.setSavedInfo(userInfo.copy(contribution = newContribution))

        val oldContribution = getNotifiedCodeforcesContribution() ?: userInfo.contribution

        notificationChannels.codeforces.contribution_changes.notify(context) {
            setSubText(handle)
            setContentTitle("Contribution change: ${oldContribution.toSignedString()} → ${newContribution.toSignedString()}")
            setSmallIcon(R.drawable.ic_person)
            setSilent(true)
            setAutoCancel(true)
            setShowWhen(false)
            attachUrl(url = userInfo.userPageUrl, context = context)
            addExtras(bundleOf(KEY_CF_CONTRIBUTION to oldContribution))
        }
    }

    private suspend fun atcoderRating() {
        val dataStore = atcoderAccountManager.dataStore(context)
        val userInfo = dataStore.getSavedInfo() ?: return
        if (userInfo.status != STATUS.OK) return

        val lastRatingChange = AtCoderApi.runCatching {
            getRatingChanges(handle = userInfo.handle)
        }.getOrNull()?.lastOrNull() ?: return

        val lastRatingChangeContestId = lastRatingChange.getContestId()

        val prevRatingChangeContestId = dataStore.lastRatedContestId()

        if (prevRatingChangeContestId == lastRatingChangeContestId && userInfo.rating == lastRatingChange.NewRating) return

        dataStore.lastRatedContestId(lastRatingChangeContestId)

        if (prevRatingChangeContestId != null) {
            atcoderAccountManager.notifyRatingChange(userInfo.handle, lastRatingChange, context)
            val newInfo = atcoderAccountManager.loadInfo(userInfo.handle)
            if (newInfo.status != STATUS.FAILED) {
                dataStore.setSavedInfo(newInfo)
            } else {
                dataStore.setSavedInfo(userInfo.copy(rating = lastRatingChange.NewRating))
            }
        }
    }

    private fun getNotifiedCodeforcesContribution(): Int? {
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = notificationChannels.codeforces.contribution_changes.notificationId
        return notificationManager.activeNotifications
            .find { it.id == notificationId }
            ?.notification?.extras?.getInt(KEY_CF_CONTRIBUTION)
    }
}

private const val KEY_CF_CONTRIBUTION = "cf_contribution"
