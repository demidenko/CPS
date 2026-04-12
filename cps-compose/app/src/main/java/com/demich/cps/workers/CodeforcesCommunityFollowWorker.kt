package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.community.follow.followRepository
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.features.codeforces.follow.database.CodeforcesUserBlog
import com.demich.cps.features.codeforces.follow.database.handle
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.userInfoOrNull
import com.demich.datastore_itemized.edit
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant


class CodeforcesCommunityFollowWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object : CPSPeriodicWorkProvider {
        override fun getWork(context: Context) = object : CPSPeriodicWork(name = "cf_follow", context = context) {
            override suspend fun isEnabled() = context.settingsCommunity.codeforcesFollowEnabled()
            override suspend fun requestBuilder() =
                CPSPeriodicWorkRequestBuilder<CodeforcesCommunityFollowWorker>(
                    repeatInterval = 4.hours,
                    batteryNotLow = true
                )
        }
    }

    override suspend fun runWork(): Result {
        val repository = context.followRepository

        //TODO: consider skip this if blogs.size is small
        //update userInfo to keep fresh lastOnlineTime
        val profiles = repository.updateUsers()

        val blogs = repository.blogs()

        val lastOnlineItem = hintsDataStore.followLastUserOnlineTime
        val blogsToUpdate = lastOnlineItem().let { last ->
            blogs.filter {
                // can't just check it.userLastOnlineTime because of possible ProfileResult.Failed in updateUsers
                val canSkip = profiles[it.handle]?.let { profile ->
                    profile is ProfileResult.Success && profile.userInfo.lastOnlineTime == last[it.id]
                } ?: false

                it.blogSize == null || !canSkip
            }
        }

        blogsToUpdate
            .forEachWithProgress { blog ->
                repository.getAndReloadBlogEntries(handle = blog.handle).getOrThrow()
                lastOnlineItem.edit { put(blog.id, blog.userLastOnlineTimeOrNull()) }
            }

        return Result.success()
    }
}

private fun CodeforcesUserBlog.userLastOnlineTimeOrNull(): Instant? =
    userProfile.userInfoOrNull()?.lastOnlineTime
