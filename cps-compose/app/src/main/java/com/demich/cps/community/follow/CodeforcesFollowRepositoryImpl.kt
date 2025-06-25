package com.demich.cps.community.follow

import android.content.Context
import com.demich.cps.R
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.features.codeforces.follow.database.CodeforcesFollowRepository
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.platforms.api.codeforces.CodeforcesUrls
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.clients.codeforces.CodeforcesClient
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils

val Context.followRepository: CodeforcesFollowRepository
    get() = CodeforcesFollowRepositoryImpl(context = this)

private class CodeforcesFollowRepositoryImpl(val context: Context):
    CodeforcesFollowRepository(api = CodeforcesClient, context = context) {

    override suspend fun getLocale() = context.settingsCommunity.codeforcesLocale()

    override fun notifyNewBlogEntry(blogEntry: CodeforcesBlogEntry) =
        notificationChannels.codeforces.new_blog_entry(blogEntry.id).notify(context) {
            subText = "New codeforces blog entry"
            contentTitle = blogEntry.authorHandle
            bigContent = CodeforcesUtils.extractTitle(blogEntry)
            smallIcon = R.drawable.ic_new_post
            autoCancel = true
            time = blogEntry.creationTime
            url = CodeforcesUrls.blogEntry(blogEntry.id)
        }
}