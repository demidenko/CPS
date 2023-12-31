package com.demich.cps.workers

import android.content.Context
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkerParameters
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.features.codeforces.lost.database.CodeforcesLostBlogEntry
import com.demich.cps.features.codeforces.lost.database.lostBlogEntriesDao
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.platforms.api.CodeforcesApi
import com.demich.cps.platforms.api.CodeforcesBlogEntry
import com.demich.cps.platforms.api.CodeforcesColorTag
import com.demich.cps.platforms.api.CodeforcesLocale
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils
import com.demich.cps.utils.mapToSet
import com.demich.cps.utils.partitionPoint
import com.demich.datastore_itemized.DataStoreItem
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


class CodeforcesNewsLostRecentWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object {
        fun getWork(context: Context) = object : CPSWork(name = "cf_lost", context = context) {
            override suspend fun isEnabled() = context.settingsNews.codeforcesLostEnabled()
            override val requestBuilder: PeriodicWorkRequest.Builder
                get() = CPSPeriodicWorkRequestBuilder<CodeforcesNewsLostRecentWorker>(
                    repeatInterval = 45.minutes
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
        val settings = context.settingsNews
        val locale = settings.codeforcesLocale()

        val recentBlogEntries = extractRecentBlogEntries(
            source = CodeforcesApi.runCatching {
                getPageSource(path = "/recent-actions", locale = locale)
            }.getOrElse { return Result.retry() }
        )

        val dao = context.lostBlogEntriesDao
        val minRatingColorTag = settings.codeforcesLostMinRatingTag()

        //get current suspects with removing old ones
        val suspects: List<CodeforcesLostBlogEntry>
        dao.getSuspects().partition {
            isNew(it.blogEntry.creationTime) && it.blogEntry.authorColorTag >= minRatingColorTag
        }.let { (valid, invalid) ->
            suspects = valid
            dao.remove(invalid)
        }

        //catch new suspects from recent actions
        findSuspects(
            blogEntries = recentBlogEntries
                .filter { it.authorColorTag >= minRatingColorTag }
                .filter { blogEntry -> suspects.none { it.id == blogEntry.id } },
            locale = locale,
            isNew = ::isNew,
            hintItem = settings.codeforcesLostHintNotNew,
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

private class CachedBlogEntryApi(val locale: CodeforcesLocale) {
    private val cache = mutableMapOf<Int, Instant>()

    suspend inline fun getCreationTime(blogEntryId: Int, onApiFailure: () -> Nothing): Instant =
        cache.getOrPut(blogEntryId) {
            CodeforcesApi.runCatching { getBlogEntry(blogEntryId = blogEntryId, locale = locale) }
                .getOrElse { onApiFailure() }
                .creationTime
        }

    fun getLastNotNew(isNew: (Instant) -> Boolean) =
        cache.asSequence()
            .filter { !isNew(it.value) }
            .maxByOrNull { it.value }
}

private suspend inline fun findSuspects(
    blogEntries: Collection<CodeforcesBlogEntry>,
    locale: CodeforcesLocale,
    noinline isNew: (Instant) -> Boolean,
    hintItem: DataStoreItem<Pair<Int, Instant>?>,
    onApiFailure: () -> Nothing,
    onSuspect: (CodeforcesBlogEntry) -> Unit
) {
    //reset just in case isNew window change
    hintItem.update {
        if (it != null && isNew(it.second)) null
        else it
    }

    val notNewBlogEntryId = hintItem()?.first ?: Int.MIN_VALUE
    val cachedApi = CachedBlogEntryApi(locale = locale)

    blogEntries.filter { it.id > notNewBlogEntryId }.sortedBy { it.id }.apply {
        val indexOfFirstNew = partitionPoint {
            !isNew(cachedApi.getCreationTime(blogEntryId = it.id, onApiFailure = onApiFailure))
        }
        subList(fromIndex = indexOfFirstNew, toIndex = size).forEach {
            val blogEntry = it.copy(
                creationTime = cachedApi.getCreationTime(blogEntryId = it.id, onApiFailure = onApiFailure),
                rating = 0,
                commentsCount = 0
            )
            onSuspect(blogEntry)
        }
    }

    //save hint
    cachedApi.getLastNotNew(isNew)?.let { entry ->
        hintItem.update {
            if (it == null || it.second < entry.value) entry.toPair()
            else it
        }
    }
}

//Required against new year color chaos
private suspend fun List<CodeforcesBlogEntry>.fixedHandleColors(): List<CodeforcesBlogEntry> {
    val authors = CodeforcesUtils.getUsersInfo(handles = map { it.authorHandle }, doRedirect = false)
    return map { blogEntry ->
        val userInfo = authors.getValue(blogEntry.authorHandle)
        require(userInfo.status == STATUS.OK)
        if (blogEntry.authorColorTag == CodeforcesColorTag.ADMIN) blogEntry
        else blogEntry.copy(authorColorTag = CodeforcesColorTag.fromRating(userInfo.rating))
    }
}

private fun isNewYearChaos(source: String): Boolean {
    //TODO: check is new year chaos
    return true
}

private suspend fun extractRecentBlogEntries(source: String): List<CodeforcesBlogEntry> {
    return CodeforcesUtils.extractRecentBlogEntries(source).let {
        if (isNewYearChaos(source)) it.fixedHandleColors()
        else it
    }
}
