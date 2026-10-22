package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.community.follow.followRepository
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.platforms.codeforces.follow.storage.CodeforcesUserBlogInfo
import com.demich.cps.platforms.codeforces.follow.storage.handle
import com.demich.cps.profiles.userinfo.CodeforcesUserInfo
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.userInfoOrNull
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.edit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant


class CodeforcesFollowWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object : CPSPeriodicWorkProvider {
        override val workName get() = "cf_follow"

        override fun getWork(context: Context) = object : CPSPeriodicWork(name = workName, context = context) {
            override suspend fun requestBuilder() =
                if (context.settingsCommunity.codeforcesFollowEnabled()) {
                    CPSPeriodicWorkRequestBuilder<CodeforcesFollowWorker>(
                        repeatInterval = 4.hours,
                        batteryNotLow = true
                    )
                } else null
        }
    }

    override suspend fun runWork() {
        val repository = context.followRepository

        // update userInfo to keep fresh lastOnlineTime
        val profiles = repository.updateProfiles()

        val lastOnlineItem = CodeforcesFollowWorkerStorage(context).usersLastOnlineTime

        val skipper = SkippingHeuristic(
            profiles = profiles,
            lastOnline = lastOnlineItem(),
            currentTime = workerStartTime,
            safeToSkipDuration = 24.hours
        )

        val blogsToUpdate = repository.blogs().filter {
            it.blogSize == null || !skipper.canSkip(it)
        }

        blogsToUpdate
            .sortedByDescending { it.userLastOnlineTimeOrNull() ?: Instant.DISTANT_PAST }
            .forEachWithProgress { blog ->
                repository.getAndReloadBlogEntries(handle = blog.handle).getOrThrow()
                lastOnlineItem.edit { set(key = blog.id, value = blog.userLastOnlineTimeOrNull()) }
            }
    }
}

private fun CodeforcesUserBlogInfo.userLastOnlineTimeOrNull(): Instant? =
    userProfile.userInfoOrNull()?.lastOnlineTime

private class CodeforcesFollowWorkerStorage(context: Context): ItemizedDataStore(context.dataStore) {
    companion object {
        private val Context.dataStore by workerDataStoreDelegate(CodeforcesFollowWorker)
    }

    //TODO: clean up unused ids sometimes
    val usersLastOnlineTime = jsonCPS.itemMap<Long, Instant?>(name = "users_last_online")
}

private class SkippingHeuristic(
    val profiles: Map<String, ProfileResult<CodeforcesUserInfo>>,
    val lastOnline: Map<Long, Instant?>,
    val currentTime: Instant,
    val safeToSkipDuration: Duration
) {
    fun canSkip(blog: CodeforcesUserBlogInfo): Boolean {
        val profile = profiles[blog.handle]

        // can't just check blog.userLastOnlineTime because of possible ProfileResult.Failed in updateUsers
        if (profile is ProfileResult.Success) {
            val lastOnlineTime = lastOnline[blog.id]
            if (profile.userInfo.lastOnlineTime == lastOnlineTime) {
                return currentTime - lastOnlineTime >= safeToSkipDuration
            }
        }

        return false
    }
}