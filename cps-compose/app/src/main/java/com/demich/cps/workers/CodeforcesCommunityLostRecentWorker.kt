package com.demich.cps.workers

import android.content.Context
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.features.codeforces.lost.database.CodeforcesLostBlogEntry
import com.demich.cps.features.codeforces.lost.database.lostBlogEntriesDao
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.platforms.api.CodeforcesApi
import com.demich.cps.platforms.api.CodeforcesBlogEntry
import com.demich.cps.platforms.api.CodeforcesColorTag
import com.demich.cps.platforms.api.CodeforcesLocale
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils
import com.demich.datastore_itemized.DataStoreItem
import com.demich.kotlin_stdlib_boost.mapToSet
import com.demich.kotlin_stdlib_boost.partitionPoint
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
            override val requestBuilder: PeriodicWorkRequest.Builder
                get() = CPSPeriodicWorkRequestBuilder<CodeforcesCommunityLostRecentWorker>(
                    repeatInterval = 30.minutes
                )
        }

        /*suspend fun updateInfo(context: Context, progress: MutableStateFlow<Pair<Int,Int>?>) {
            val blogsDao = getLostBlogsDao(context)

            val blogEntries = blogsDao.getLost().sortedByDescending { it.id }

            progress.value = 0 to blogEntries.size
            var done = 0

            val users = CodeforcesUtils.getUsersInfo(blogEntries.map { it.authorHandle })
            val locale = NewsFragment.getCodeforcesContentLanguage(context)

            blogEntries.forEach { originalBlogEntry ->
                var blogEntry = originalBlogEntry.copy()

                //updates author's handle color
                users[blogEntry.authorHandle]?.takeIf { it.status==STATUS.OK }?.let { user ->
                    blogEntry = blogEntry.copy(
                        authorColorTag = CodeforcesUtils.getTagByRating(user.rating)
                    )
                }

                CodeforcesAPI.getBlogEntry(blogEntry.id,locale)?.let { response ->
                    if(response.status == CodeforcesAPIStatus.FAILED && response.isBlogNotFound(blogEntry.id)){
                        //remove deleted
                        blogsDao.remove(blogEntry)
                    } else {
                        //update title and author's handle
                        if(response.status == CodeforcesAPIStatus.OK) response.result?.let { freshBlogEntry ->
                            val title = freshBlogEntry.title.removeSurrounding("<p>", "</p>")
                            blogEntry = blogEntry.copy(
                                authorHandle = freshBlogEntry.authorHandle,
                                title = fromHTML(title).toString()
                            )
                        }
                    }
                }

                if(blogEntry != originalBlogEntry) blogsDao.update(blogEntry)

                progress.value = ++done to blogEntries.size
            }

            progress.value = null
        }*/
    }

    private fun isNew(blogCreationTime: Instant) = workerStartTime - blogCreationTime < 24.hours
    private fun isOldLost(blogCreationTime: Instant) = workerStartTime - blogCreationTime > 7.days

    override suspend fun runWork(): Result {
        val settings = context.settingsCommunity
        val dao = context.lostBlogEntriesDao

        val locale = settings.codeforcesLocale()
        val minRatingColorTag = settings.codeforcesLostMinRatingTag()

        //get current suspects with removing old ones
        val suspects: List<CodeforcesLostBlogEntry>
        dao.getSuspects().partition {
            isNew(it.blogEntry.creationTime) && it.blogEntry.authorColorTag >= minRatingColorTag
        }.let { (valid, invalid) ->
            suspects = valid
            dao.remove(invalid)
        }

        val recentBlogEntries =
            CodeforcesApi.runCatching { getPageSource(path = "/recent-actions", locale = locale) }
            .getOrElse { return Result.retry() }
            .let { CodeforcesUtils.extractRecentBlogEntries(it) }

        //catch new suspects from recent actions
        findSuspects(
            blogEntries = recentBlogEntries.filter { blogEntry -> suspects.none { it.id == blogEntry.id } },
            locale = locale,
            minRatingColorTag = minRatingColorTag,
            isNew = ::isNew,
            lastNotNewIdItem = settings.codeforcesLostHintNotNew,
            onApiFailure = { return Result.retry() }
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

    suspend inline fun getCreationTime(blogEntryId: Int, onApiFailure: () -> Nothing): Instant =
        cache.getOrPut(blogEntryId) {
            CodeforcesApi.runCatching { getBlogEntry(blogEntryId = blogEntryId, locale = locale) }
                .getOrElse { onApiFailure() }
                .also { onUpdate(it) }
                .creationTime
        }
}

private fun Collection<CodeforcesBlogEntry>.filterIdGreaterThen(id: Int) = filter { it.id > id }

private suspend fun Collection<CodeforcesBlogEntry>.fixAndFilterColorTag(minRatingColorTag: CodeforcesColorTag) =
    fixedHandleColors().filter { it.authorColorTag >= minRatingColorTag }

private inline fun Collection<CodeforcesBlogEntry>.filterNewEntries(isNew: (CodeforcesBlogEntry) -> Boolean) =
    sortedBy { it.id }.run {
        val indexOfFirstNew = partitionPoint { !isNew(it) }
        subList(fromIndex = indexOfFirstNew, toIndex = size)
    }

private suspend inline fun findSuspects(
    blogEntries: Collection<CodeforcesBlogEntry>,
    locale: CodeforcesLocale,
    minRatingColorTag: CodeforcesColorTag,
    noinline isNew: (Instant) -> Boolean,
    lastNotNewIdItem: DataStoreItem<Pair<Int, Instant>?>,
    onApiFailure: () -> Nothing,
    onSuspect: (CodeforcesBlogEntry) -> Unit
) {
    //ensure hint in case isNew logic changes
    lastNotNewIdItem.update {
        if (it == null || !isNew(it.second)) it else null
    }

    val cachedApi = CachedBlogEntryApi(locale = locale) { blogEntry ->
        val time = blogEntry.creationTime
        if (!isNew(time)) {
            //save hint
            lastNotNewIdItem.update {
                if (it == null || it.second < time) Pair(blogEntry.id, time)
                else it
            }
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
        .filterIdGreaterThen(lastNotNewIdItem()?.first ?: Int.MIN_VALUE)
        .fixAndFilterColorTag(minRatingColorTag)
        .filterNewEntries { isNew(cachedApi.getCreationTime(blogEntryId = it.id, onApiFailure = onApiFailure)) }
        .forEach {
            val blogEntry = it.copy(
                creationTime = cachedApi.getCreationTime(blogEntryId = it.id, onApiFailure = onApiFailure),
                rating = 0,
                commentsCount = 0
            )
            onSuspect(blogEntry)
        }
}

//Required against new year color chaos
private suspend fun Collection<CodeforcesBlogEntry>.fixedHandleColors(): List<CodeforcesBlogEntry> {
    val authors = CodeforcesUtils.getUsersInfo(handles = map { it.authorHandle }, doRedirect = false)
    return map { blogEntry ->
        val userInfo = authors.getValue(blogEntry.authorHandle)
        require(userInfo.status == STATUS.OK)
        if (blogEntry.authorColorTag == CodeforcesColorTag.ADMIN) blogEntry
        else blogEntry.copy(authorColorTag = CodeforcesColorTag.fromRating(userInfo.rating))
    }
}
