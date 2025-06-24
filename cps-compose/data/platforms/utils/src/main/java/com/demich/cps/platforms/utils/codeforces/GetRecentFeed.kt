package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.CodeforcesPageContentProvider
import com.demich.cps.platforms.api.codeforces.models.CodeforcesLocale
import kotlin.math.max

suspend fun CodeforcesPageContentProvider.getRecentFeed(locale: CodeforcesLocale): CodeforcesRecentFeed {
    val source = getRecentActionsPage(locale = locale)
    val comments = CodeforcesUtils.extractComments(source)
    //blog entry with low rating disappeared from blogEntries but has comments, need to merge
    val blogEntries = CodeforcesUtils.extractRecentBlogEntries(source).toMutableList()
    val blogEntriesIds = blogEntries.mapTo(mutableSetOf()) { it.id }
    val usedIds = mutableSetOf<Int>()
    var index = 0
    for (comment in comments) {
        val blogEntry = requireNotNull(comment.blogEntry).let {
            CodeforcesRecentFeedBlogEntry(
                id = it.id,
                title = it.title,
                author = it.author,
                isLowRated = false
            )
        }
        val id = blogEntry.id
        if (id !in blogEntriesIds) {
            blogEntriesIds.add(id)
            if (index < blogEntries.size) {
                //mark low rated
                blogEntries.add(
                    index = index,
                    element = blogEntry.copy(isLowRated = true)
                )
            } else {
                //latest recent comments has no blog entries in recent action, so most likely not low rated
                check(index == blogEntries.size)
                blogEntries.add(blogEntry)
            }
        }
        if (id !in usedIds) {
            usedIds.add(id)
            val curIndex = blogEntries.indexOfFirst { it.id == id }
            index = max(index, curIndex + 1)
        }
    }
    return CodeforcesRecentFeed(blogEntries, comments)
}