package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.community.settings.CodeforcesLostHint
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.features.codeforces.lost.database.CodeforcesLostBlogEntry
import com.demich.cps.features.codeforces.lost.database.CodeforcesLostDao
import com.demich.cps.features.codeforces.lost.database.lostBlogEntriesDao
import com.demich.cps.platforms.api.codeforces.CodeforcesClient
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.api.codeforces.models.CodeforcesColorTag
import com.demich.cps.platforms.api.codeforces.models.CodeforcesLocale
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils
import com.demich.cps.platforms.utils.codeforces.getProfiles
import com.demich.datastore_itemized.DataStoreItem
import com.demich.kotlin_stdlib_boost.mapToSet
import com.demich.kotlin_stdlib_boost.partitionIndex
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


class CodeforcesCommunityLostRecentWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object {
        fun getWork(context: Context) = object : CPSPeriodicWork(name = "cf_lost", context = context) {
            override suspend fun isEnabled() = context.settingsCommunity.codeforcesLostEnabled()
            override suspend fun requestBuilder() =
                CPSPeriodicWorkRequestBuilder<CodeforcesCommunityLostRecentWorker>(
                    repeatInterval = 30.minutes
                )
        }
    }

    private fun isNew(blogCreationTime: Instant) = workerStartTime - blogCreationTime < 24.hours
    private fun isOldLost(blogCreationTime: Instant) = workerStartTime - blogCreationTime > 7.days

    private suspend fun CodeforcesLostDao.getSuspectsRemoveOld(minRatingColorTag: CodeforcesColorTag) =
        getSuspects().partition {
            isNew(it.blogEntry.creationTime) && it.blogEntry.authorColorTag >= minRatingColorTag
        }.let { (valid, invalid) ->
            remove(invalid)
            valid
        }

    private suspend fun getRecentBlogEntries(locale: CodeforcesLocale): List<CodeforcesBlogEntry> {
        suspend fun extractFrom(page: CodeforcesClient.BasePage) =
            CodeforcesUtils.extractRecentBlogEntries(
                source = CodeforcesClient.getPage(page = page, locale = locale)
            )

        // "/groups" has less size than "/recent" and hopefully will be cached by cf
        return runCatching {
            extractFrom(CodeforcesClient.BasePage.groups)
        }.getOrElse {
            extractFrom(CodeforcesClient.BasePage.recent)
        }
    }

    override suspend fun runWork(): Result {
        val settings = context.settingsCommunity

        val locale = settings.codeforcesLocale()
        val minRatingColorTag = settings.codeforcesLostMinRatingTag()

        val recentBlogEntries = getRecentBlogEntries(locale = locale)
        //TODO: use api.recentActions on fail but !![only for findSuspects step]!!

        val dao = context.lostBlogEntriesDao
        //get current suspects with removing old ones
        val suspects = dao.getSuspectsRemoveOld(minRatingColorTag)

        //catch new suspects from recent actions
        findSuspects(
            blogEntries = recentBlogEntries.filter { blogEntry -> suspects.none { it.id == blogEntry.id } },
            locale = locale,
            minRatingColorTag = minRatingColorTag,
            isNew = ::isNew,
            lastNotNewIdItem = settings.codeforcesLostHintNotNew
        ) {
            dao.insert(
                CodeforcesLostBlogEntry(
                    blogEntry = it,
                    isSuspect = true,
                    timeStamp = Instant.DISTANT_PAST
                )
            )
        }

        val recentIds = recentBlogEntries.mapToSet { it.id }

        //remove from lost
        dao.remove(
            dao.getLost().filter {
                isOldLost(it.blogEntry.creationTime) || it.blogEntry.id in recentIds
            }
        )

        //suspect become lost
        suspects.forEach { blogEntry ->
            if (blogEntry.id !in recentIds) {
                dao.insert(blogEntry.copy(
                    isSuspect = false,
                    timeStamp = workerStartTime
                ))
            }
        }

        return Result.success()
    }

}

private class CachedBlogEntryApi(
    val locale: CodeforcesLocale,
    val onUpdate: suspend (CodeforcesBlogEntry) -> Unit
) {
    private val cache = mutableMapOf<Int, Instant>()

    suspend inline fun getCreationTime(blogEntryId: Int): Instant =
        cache.getOrPut(blogEntryId) {
            CodeforcesClient.getBlogEntry(blogEntryId = blogEntryId, locale = locale)
                .also { onUpdate(it) }
                .creationTime
        }

    suspend fun useRecentActions() {
        CodeforcesClient.runCatching { getRecentActions(locale = locale) }
            .getOrElse { return }
            .forEach {
                it.blogEntry?.let { blogEntry ->
                    if (cache.put(key = blogEntry.id, value = blogEntry.creationTime) == null) {
                        onUpdate(blogEntry)
                    }
                }
            }
    }
}

private fun Collection<CodeforcesBlogEntry>.filterIdGreaterThan(id: Int) = filter { it.id > id }

private suspend fun Collection<CodeforcesBlogEntry>.fixAndFilterColorTag(minRatingColorTag: CodeforcesColorTag) =
    fixedHandleColors().filter { it.authorColorTag >= minRatingColorTag }

private inline fun Collection<CodeforcesBlogEntry>.filterNewEntries(
    isFinalFilter: Boolean = false,
    isNew: (CodeforcesBlogEntry) -> Boolean
) = sortedBy { it.id }.run {
    val indexOfFirstNew =
        if (isFinalFilter) {
            indexOfLast { !isNew(it) } + 1
        } else {
            partitionIndex { !isNew(it) }
        }
    subList(fromIndex = indexOfFirstNew, toIndex = size)
}

private suspend inline fun findSuspects(
    blogEntries: Collection<CodeforcesBlogEntry>,
    locale: CodeforcesLocale,
    minRatingColorTag: CodeforcesColorTag,
    crossinline isNew: (Instant) -> Boolean,
    lastNotNewIdItem: DataStoreItem<CodeforcesLostHint?>,
    onSuspect: (CodeforcesBlogEntry) -> Unit
) {
    val cachedApi = CachedBlogEntryApi(locale = locale) { blogEntry ->
        val time = blogEntry.creationTime
        if (!isNew(time)) {
            //save hint
            lastNotNewIdItem.update {
                if (it == null || it.creationTime < time) CodeforcesLostHint(blogEntry.id, time)
                else it
            }
        }
    }

    val hint = lastNotNewIdItem.let { item ->
        val hint = item()
        //ensure hint in case isNew logic changes
        if (hint != null && isNew(hint.creationTime)) {
            item.invoke(newValue = null)
            null
        } else {
            hint
        }
    }

    /*
    These 3 filters can be in arbitrary order but
    .filterIdGreaterThen should be before .filterNewEntries
    so there is three ways:
    #1 .fixAndFilterColorTag .filterIdGreaterThen .filterNewEntries
    #2 .filterIdGreaterThen .fixAndFilterColorTag .filterNewEntries
    #3 .filterIdGreaterThen .filterNewEntries .fixAndFilterColorTag
    additionally #2 is not worse than #1
    So as result choose #2 or #3
     */
    blogEntries
        // TODO: `.invoke()` instead of `()` https://youtrack.jetbrains.com/issue/KT-74111/
        .filterIdGreaterThan(hint?.blogEntryId ?: Int.MIN_VALUE)
        .fixAndFilterColorTag(minRatingColorTag)
        .also {
            if (it.size > 1) cachedApi.useRecentActions()
        }
        .filterNewEntries(isFinalFilter = true) { isNew(cachedApi.getCreationTime(blogEntryId = it.id)) }
        .forEach {
            val blogEntry = it.copy(
                creationTime = cachedApi.getCreationTime(blogEntryId = it.id),
                rating = 0,
                commentsCount = null
            )
            onSuspect(blogEntry)
        }
}

//Required against new year color chaos
private suspend fun Collection<CodeforcesBlogEntry>.fixedHandleColors(): List<CodeforcesBlogEntry> {
    val authors = CodeforcesClient.getProfiles(handles = map { it.authorHandle }, doRedirect = false)
    return map { blogEntry ->
        val result = authors.getValue(blogEntry.authorHandle)
        require(result is ProfileResult.Success)
        if (blogEntry.authorColorTag == CodeforcesColorTag.ADMIN) blogEntry
        else blogEntry.copy(authorColorTag = CodeforcesUtils.colorTagFrom(result.userInfo.rating))
    }
}
