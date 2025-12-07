package com.demich.cps.workers

import android.content.Context
import androidx.core.os.bundleOf
import androidx.work.WorkerParameters
import com.demich.cps.R
import com.demich.cps.accounts.managers.AtCoderAccountManager
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.accounts.managers.toRatingChange
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.userInfoOrNull
import com.demich.cps.notifications.getActiveNotification
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.platforms.clients.AtCoderClient
import com.demich.cps.platforms.clients.codeforces.CodeforcesClient
import com.demich.cps.platforms.utils.codeforces.getProfile
import com.demich.cps.utils.toSignedString
import com.demich.datastore_itemized.fromSnapshot
import com.demich.datastore_itemized.value
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

    override suspend fun runWork(): Result {
        val jobs = buildList<suspend () -> Unit> {
            with(CodeforcesAccountManager()) {
                getSettings(context).fromSnapshot {
                    if (observeRating.value) add { checkRating(context = context) }
                    if (observeContribution.value) add { checkContribution(context = context) }
                }
            }

            with(AtCoderAccountManager()) {
                getSettings(context).fromSnapshot {
                    if (observeRating.value) add { checkRating(context = context) }
                }
            }
        }

        jobs.joinAllWithProgress()

        return Result.success()
    }
}

private suspend fun CodeforcesAccountManager.checkRating(context: Context) {
    val userInfo = dataStore(context).profile()
        ?.userInfoOrNull() ?: return

    val lastRatingChange = CodeforcesClient.runCatching {
        getUserRatingChanges(handle = userInfo.handle)
    }.getOrNull()?.lastOrNull() ?: return

    applyRatingChange(lastRatingChange, context)
}

private const val KEY_CF_CONTRIBUTION = "cf_contribution"

private fun getNotifiedCodeforcesContribution(context: Context): Int? =
    notificationChannels.codeforces.contribution_changes.getActiveNotification(context)
        ?.extras?.getInt(KEY_CF_CONTRIBUTION)

private suspend fun CodeforcesAccountManager.checkContribution(context: Context) {
    val dataStore = dataStore(context)
    val userInfo = dataStore.profile()
        ?.userInfoOrNull() ?: return

    val handle = userInfo.handle
    val newContribution = CodeforcesClient.getProfile(handle = handle, recoverHandle = false)
        .userInfoOrNull()?.contribution ?: return

    if (newContribution == userInfo.contribution) return

    dataStore.setProfile(ProfileResult(userInfo.copy(contribution = newContribution)))

    val oldContribution = getNotifiedCodeforcesContribution(context) ?: userInfo.contribution

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

private suspend fun AtCoderAccountManager.checkRating(context: Context) {
    val dataStore = dataStore(context)
    val userInfo = dataStore.profile()
        ?.userInfoOrNull() ?: return

    val lastRatingChange = AtCoderClient.runCatching {
        getRatingChanges(handle = userInfo.handle)
    }.getOrNull()?.lastOrNull() ?: return

    dataStore.applyRatingChange(ratingChange = lastRatingChange.toRatingChange(handle = userInfo.handle))
}