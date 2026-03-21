package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.R
import com.demich.cps.notifications.getActiveNotification
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.platforms.clients.AtCoderClient
import com.demich.cps.platforms.clients.codeforces.CodeforcesClient
import com.demich.cps.platforms.utils.codeforces.getProfile
import com.demich.cps.profiles.managers.AtCoderProfileManager
import com.demich.cps.profiles.managers.CodeforcesProfileManager
import com.demich.cps.profiles.managers.applyRatingChange
import com.demich.cps.profiles.toRatingChange
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.userInfoOrNull
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
        joinAllWithProgress {
            with(CodeforcesProfileManager()) {
                settingsStorage(context).fromSnapshot {
                    if (observeRating.value) add { checkRating(context = context) }
                    if (observeContribution.value) add { checkContribution(context = context) }
                }
            }

            with(AtCoderProfileManager()) {
                settingsStorage(context).fromSnapshot {
                    if (observeRating.value) add { checkRating(context = context) }
                }
            }
        }

        return Result.success()
    }
}

private suspend fun CodeforcesProfileManager.checkRating(context: Context) {
    val userInfo = profileStorage(context).profile()
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

private suspend fun CodeforcesProfileManager.checkContribution(context: Context) {
    val storage = profileStorage(context)
    val userInfo = storage.profile()
        ?.userInfoOrNull() ?: return

    val handle = userInfo.handle
    val newContribution = CodeforcesClient.getProfile(handle = handle, recoverHandle = false)
        .userInfoOrNull()?.contribution ?: return

    if (newContribution == userInfo.contribution) return

    storage.setProfile(ProfileResult(userInfo.copy(contribution = newContribution)))

    val oldContribution = getNotifiedCodeforcesContribution(context) ?: userInfo.contribution

    notificationChannels.codeforces.contribution_changes.notify(context) {
        subText = handle
        contentTitle = "Contribution change: ${oldContribution.toSignedString()} → ${newContribution.toSignedString()}"
        smallIcon = R.drawable.ic_person
        silent = true
        autoCancel = true
        time = null
        url = userInfo.userPageUrl
        addExtras(KEY_CF_CONTRIBUTION, oldContribution)
    }
}

private suspend fun AtCoderProfileManager.checkRating(context: Context) {
    val storage = profileStorage(context)
    val userInfo = storage.profile()
        ?.userInfoOrNull() ?: return

    val lastRatingChange = AtCoderClient.runCatching {
        getRatingChanges(handle = userInfo.handle)
    }.getOrNull()?.lastOrNull() ?: return

    storage.applyRatingChange(ratingChange = lastRatingChange.toRatingChange(handle = userInfo.handle))
}