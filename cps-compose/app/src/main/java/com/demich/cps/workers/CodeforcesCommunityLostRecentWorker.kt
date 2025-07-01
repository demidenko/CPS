package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.features.codeforces.lost.database.CodeforcesLostBlogEntry
import com.demich.cps.features.codeforces.lost.database.CodeforcesLostDao
import com.demich.cps.features.codeforces.lost.database.lostBlogEntriesDao
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.api.codeforces.models.CodeforcesColorTag
import com.demich.cps.platforms.api.codeforces.models.CodeforcesLocale
import com.demich.cps.platforms.clients.codeforces.CodeforcesClient
import com.demich.cps.platforms.utils.codeforces.CodeforcesRecentFeedBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils
import com.demich.cps.platforms.utils.codeforces.getProfiles
import com.demich.datastore_itemized.DataStoreItem
import com.demich.kotlin_stdlib_boost.mapToSet
import com.demich.kotlin_stdlib_boost.partitionIndex
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
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

    private suspend fun getRecentBlogEntries(locale: CodeforcesLocale): List<CodeforcesRecentFeedBlogEntry> {
        suspend fun extractFrom(page: suspend () -> String) =
            CodeforcesUtils.extractRecentBlogEntries(source = page())

        // "/groups" has less size than "/recent" and hopefully will be cached by cf
        return runCatching {
            extractFrom { CodeforcesClient.getGroupsPage(locale) }
        }.getOrElse {
            extractFrom { CodeforcesClient.getRecentActionsPage(locale) }
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
            api = CodeforcesClient,
            lastNotNewIdItem = hintsDataStore.codeforcesLostHintNotNew
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

@Serializable
data class CodeforcesLostHint(
    val blogEntryId: Int,
    val creationTime: Instant
)

private class CachedBlogEntriesCodeforcesApi(
    private val originApi: CodeforcesApi,
    val onNewBlogEntry: suspend (CodeforcesBlogEntry) -> Unit
): CodeforcesApi by originApi {
    private val cache = mutableMapOf<Int, CodeforcesBlogEntry>()

    override suspend fun getBlogEntry(blogEntryId: Int, locale: CodeforcesLocale) =
        cache.getOrPut(key = blogEntryId) {
            originApi.getBlogEntry(blogEntryId, locale)
                .also { onNewBlogEntry(it) }
        }

    override suspend fun getRecentActions(locale: CodeforcesLocale, maxCount: Int) =
        originApi.getRecentActions(locale = locale, maxCount = maxCount).apply {
            forEach {
                it.blogEntry?.let { blogEntry ->
                    if (cache.put(key = blogEntry.id, value = blogEntry) == null) {
                        onNewBlogEntry(blogEntry)
                    }
                }
            }
        }
}

private fun Collection<CodeforcesRecentFeedBlogEntry>.filterIdGreaterThan(id: Int) = filter { it.id > id }

private suspend fun Collection<CodeforcesRecentFeedBlogEntry>.fixAndFilterColorTag(minRatingColorTag: CodeforcesColorTag) =
    fixedHandleColors().filter { it.author.colorTag >= minRatingColorTag }

private inline fun Collection<CodeforcesRecentFeedBlogEntry>.filterNewEntries(
    isFinalFilter: Boolean = false,
    isNew: (CodeforcesRecentFeedBlogEntry) -> Boolean
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
    blogEntries: Collection<CodeforcesRecentFeedBlogEntry>,
    locale: CodeforcesLocale,
    minRatingColorTag: CodeforcesColorTag,
    crossinline isNew: (Instant) -> Boolean,
    lastNotNewIdItem: DataStoreItem<CodeforcesLostHint?>,
    api: CodeforcesApi,
    onSuspect: (CodeforcesBlogEntry) -> Unit
) {
    val cachedApi = CachedBlogEntriesCodeforcesApi(api) { blogEntry ->
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
        // TODO: `.invoke()` instead of `()` https://youtrack.jetbrains.com/issue/KT-74111/
        val hint = item.invoke()
        //ensure hint in case isNew logic changes
        if (hint != null && isNew(hint.creationTime)) {
            item.setValue(null)
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
        .filterIdGreaterThan(hint?.blogEntryId ?: Int.MIN_VALUE)
        .fixAndFilterColorTag(minRatingColorTag)
        .also {
            if (it.size > 1) cachedApi.getRecentActions(locale = locale)
        }
        .filterNewEntries(isFinalFilter = true) { isNew(cachedApi.getBlogEntry(it.id, locale).creationTime) }
        .forEach {
            val blogEntry = cachedApi.getBlogEntry(it.id, locale)
            onSuspect(
                blogEntry.copy(
                    authorColorTag = it.author.colorTag,
                    title = CodeforcesUtils.extractTitle(blogEntry),
                    rating = 0,
                    commentsCount = null
                )
            )
        }
}

//Required against new year color chaos
private suspend fun Collection<CodeforcesRecentFeedBlogEntry>.fixedHandleColors(): List<CodeforcesRecentFeedBlogEntry> {
    val profiles = CodeforcesClient.getProfiles(handles = map { it.author.handle }, recoverHandle = false)
    return map { blogEntry ->
        val profile = profiles.getValue(blogEntry.author.handle)
        require(profile is ProfileResult.Success) { "fixedHandleColors: profile result is not success" }
        if (blogEntry.author.colorTag == CodeforcesColorTag.ADMIN) blogEntry
        else {
            val colorTag = CodeforcesUtils.colorTagFrom(profile.userInfo.rating)
            blogEntry.copy(author = blogEntry.author.copy(colorTag = colorTag))
        }
    }
}
