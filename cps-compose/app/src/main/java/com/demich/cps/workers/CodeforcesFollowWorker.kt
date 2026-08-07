package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.community.follow.followRepository
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.features.codeforces.follow.database.CodeforcesUserBlog
import com.demich.cps.features.codeforces.follow.database.handle
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.userInfoOrNull
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.edit
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
            override suspend fun isEnabled() = context.settingsCommunity.codeforcesFollowEnabled()
            override suspend fun requestBuilder() =
                CPSPeriodicWorkRequestBuilder<CodeforcesFollowWorker>(
                    repeatInterval = 4.hours,
                    batteryNotLow = true
                )
        }
    }

    override suspend fun runWork() {
        val repository = context.followRepository

        //TODO: consider skip this if blogs.size is small
        //update userInfo to keep fresh lastOnlineTime
        val profiles = repository.updateProfiles()

        val blogs = repository.blogs()

        val lastOnlineItem = CodeforcesFollowWorkerStorage(context).usersLastOnlineTime
        val blogsToUpdate = lastOnlineItem().let { last ->
            blogs.filterNot {
                // can't just check it.userLastOnlineTime because of possible ProfileResult.Failed in updateUsers
                val canSkip = profiles[it.handle].let { profile ->
                    profile is ProfileResult.Success && profile.userInfo.lastOnlineTime == last[it.id]
                }
                canSkip && it.blogSize != null
            }
        }

        blogsToUpdate
            .sortedByDescending { it.userLastOnlineTimeOrNull() ?: Instant.DISTANT_PAST }
            .forEachWithProgress { blog ->
                repository.getAndReloadBlogEntries(handle = blog.handle).getOrThrow()
                lastOnlineItem.edit { set(key = blog.id, value = blog.userLastOnlineTimeOrNull()) }
            }
    }
}

private fun CodeforcesUserBlog.userLastOnlineTimeOrNull(): Instant? =
    userProfile.userInfoOrNull()?.lastOnlineTime

private class CodeforcesFollowWorkerStorage(context: Context): ItemizedDataStore(context.dataStore) {
    companion object {
        private val Context.dataStore by workerDataStoreDelegate(CodeforcesFollowWorker)
    }

    //TODO: clean up unused ids sometimes
    val usersLastOnlineTime = jsonCPS.itemMap<Long, Instant?>(name = "users_last_online")
}