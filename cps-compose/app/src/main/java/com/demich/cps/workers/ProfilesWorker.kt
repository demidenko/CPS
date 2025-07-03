package com.demich.cps.workers

import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import androidx.core.os.bundleOf
import androidx.work.WorkerParameters
import com.demich.cps.R
import com.demich.cps.accounts.managers.AtCoderAccountManager
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.accounts.managers.toRatingChange
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.userInfoOrNull
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.platforms.clients.AtCoderClient
import com.demich.cps.platforms.clients.codeforces.CodeforcesClient
import com.demich.cps.platforms.utils.codeforces.getProfile
import com.demich.cps.utils.toSignedString
import kotlin.time.Duration.Companion.minutes

class ProfilesWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object : CPSPeriodicWorkProvider {
        override fun getWork(context: Context) = object : CPSPeriodicWork(name = "profiles", context = context) {
            override suspend fun isEnabled() = true //TODO something proper
            override suspend fun requestBuilder() =
                CPSPeriodicWorkRequestBuilder<ProfilesWorker>(
                    repeatInterval = 15.minutes
                )
        }
    }

    private val codeforcesAccountManager by lazy { CodeforcesAccountManager() }
    private val atcoderAccountManager by lazy { AtCoderAccountManager() }

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
        val userInfo = codeforcesAccountManager.dataStore(context).getProfile()
            ?.userInfoOrNull() ?: return

        val lastRatingChange = CodeforcesClient.runCatching {
            getUserRatingChanges(handle = userInfo.handle)
        }.getOrNull()?.lastOrNull() ?: return

        codeforcesAccountManager.applyRatingChange(lastRatingChange, context)
    }

    private suspend fun codeforcesContribution() {
        val dataStore = codeforcesAccountManager.dataStore(context)
        val userInfo = dataStore.getProfile()
            ?.userInfoOrNull() ?: return

        val handle = userInfo.handle
        val newContribution = CodeforcesClient.getProfile(handle = handle, recoverHandle = false)
            .userInfoOrNull()?.contribution ?: return

        if (newContribution == userInfo.contribution) return

        dataStore.setProfile(ProfileResult.Success(userInfo.copy(contribution = newContribution)))

        val oldContribution = getNotifiedCodeforcesContribution() ?: userInfo.contribution

        notificationChannels.codeforces.contribution_changes.notify(context) {
            subText = handle
            contentTitle = "Contribution change: ${oldContribution.toSignedString()} → ${newContribution.toSignedString()}"
            smallIcon = R.drawable.ic_person
            silent = true
            autoCancel = true
            time = null
            url = userInfo.userPageUrl
            addExtras(bundleOf(KEY_CF_CONTRIBUTION to oldContribution))
        }
    }

    private suspend fun atcoderRating() {
        val dataStore = atcoderAccountManager.dataStore(context)
        val userInfo = dataStore.getProfile()
            ?.userInfoOrNull() ?: return

        val lastRatingChange = AtCoderClient.runCatching {
            getRatingChanges(handle = userInfo.handle)
        }.getOrNull()?.lastOrNull() ?: return

        dataStore.applyRatingChange(ratingChange = lastRatingChange.toRatingChange(handle = userInfo.handle))
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
