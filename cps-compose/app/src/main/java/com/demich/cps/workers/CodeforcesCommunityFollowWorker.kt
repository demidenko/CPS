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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
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
                    repeatInterval = 6.hours,
                    batteryNotLow = true
                )
        }
    }

    //save handles between run after fast retry
    private val proceeded = mutableSetOf<String>()

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
                val handle = blog.handle
                if (handle !in proceeded) {
                    repository.getAndReloadBlogEntries(handle).getOrThrow()
                    lastOnlineItem.edit { put(blog.id, blog.userLastOnlineTimeOrNull()) }
                    proceeded.add(handle)
                }
            }

        work.enqueueInIfEarlier(
            duration = nextEnqueueIn(blogsCount = blogs.size, proceeded = proceeded.size).coerceAtLeast(2.hours)
        )

        return Result.success()
    }
}

private fun CodeforcesUserBlog.userLastOnlineTimeOrNull(): Instant? =
    userProfile.userInfoOrNull()?.lastOnlineTime

private fun nextEnqueueIn(
    blogsCount: Int,
    proceeded: Int
): Duration {
    return 30.minutes * proceeded
}