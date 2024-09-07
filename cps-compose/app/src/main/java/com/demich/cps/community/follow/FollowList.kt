package com.demich.cps.community.follow

import android.content.Context
import com.demich.cps.R
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.features.codeforces.follow.database.CodeforcesFollowList
import com.demich.cps.notifications.attachUrl
import com.demich.cps.notifications.notificationChannels
import com.demich.cps.notifications.setBigContent
import com.demich.cps.notifications.setWhen
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils

val Context.followListDao: FollowList
    get() = FollowList(context = this)

class FollowList(context: Context): CodeforcesFollowList(context) {

    override suspend fun getLocale() = context.settingsCommunity.codeforcesLocale()

    override fun notifyNewBlogEntry(blogEntry: CodeforcesBlogEntry) =
        notificationChannels.codeforces.new_blog_entry(blogEntry.id).notify(context) {
            setSubText("New codeforces blog entry")
            setContentTitle(blogEntry.authorHandle)
            setBigContent(CodeforcesUtils.extractTitle(blogEntry))
            setSmallIcon(R.drawable.ic_new_post)
            setAutoCancel(true)
            setWhen(blogEntry.creationTime)
            attachUrl(url = CodeforcesApi.urls.blogEntry(blogEntry.id), context = context)
        }
}