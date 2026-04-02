package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.platforms.api.codeforces.CodeforcesPageContentProvider
import com.demich.cps.platforms.api.codeforces.models.CodeforcesLocale
import org.jsoup.Jsoup
import kotlin.math.max

suspend fun CodeforcesPageContentProvider.getRecentFeed(locale: CodeforcesLocale): CodeforcesRecentFeed =
    CodeforcesUtils.extractRecentFeed(source = getRecentActionsPage(locale = locale))

private fun CodeforcesUtils.extractRecentFeed(source: String): CodeforcesRecentFeed {
    val document = Jsoup.parse(source)
    val comments = extractComments(document)
    //blog entry with low rating disappeared from blogEntries but has comments, need to merge
    val blogEntries = extractRecentBlogEntries(document).toMutableList()
    val blogEntriesIds = blogEntries.mapTo(mutableSetOf()) { it.id }
    val usedIds = mutableSetOf<Int>()
    var index = 0
    for (comment in comments) {
        val id = comment.blogEntryId
        if (id !in blogEntriesIds) {
            blogEntriesIds.add(id)
            if (index < blogEntries.size) {
                //mark low rated
                blogEntries.add(
                    index = index,
                    element = comment.toRecentFeedBlogEntry(isLowRated = true)
                )
            } else {
                //latest recent comments has no blog entries in recent action, so most likely not low rated
                check(index == blogEntries.size)
                blogEntries.add(comment.toRecentFeedBlogEntry(isLowRated = false))
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

private fun CodeforcesWebComment.toRecentFeedBlogEntry(isLowRated: Boolean) =
    CodeforcesRecentFeedBlogEntry(
        id = blogEntryId,
        title = blogEntryTitle,
        author = blogEntryAuthor,
        isLowRated = isLowRated
    )