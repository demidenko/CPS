package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.features.codeforces.lost.database.CodeforcesLostRepository
import com.demich.cps.features.codeforces.lost.database.codeforcesLostRepository
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesPageContentProvider
import com.demich.cps.platforms.api.codeforces.getRecentActions
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.api.codeforces.models.CodeforcesLocale
import com.demich.cps.platforms.clients.codeforces.CodeforcesClient
import com.demich.cps.platforms.utils.codeforces.CodeforcesColorTag
import com.demich.cps.platforms.utils.codeforces.CodeforcesRecentFeedBlogEntry
import com.demich.cps.platforms.utils.codeforces.CodeforcesUtils
import com.demich.cps.platforms.utils.codeforces.CodeforcesWebBlogEntry
import com.demich.cps.platforms.utils.codeforces.getProfiles
import com.demich.cps.platforms.utils.codeforces.toWebBlogEntry
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.datastore_itemized.DataStoreItem
import com.demich.datastore_itemized.fromSnapshot
import com.demich.datastore_itemized.value
import com.demich.kotlin_stdlib_boost.mapToSet
import com.demich.kotlin_stdlib_boost.partitionIndex
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant


class CodeforcesCommunityLostRecentWorker(
    context: Context,
    parameters: WorkerParameters
): CPSWorker(
    work = getWork(context),
    parameters = parameters
) {
    companion object : CPSPeriodicWorkProvider {
        override fun getWork(context: Context) = object : CPSPeriodicWork(name = "cf_lost", context = context) {
            override suspend fun isEnabled() = context.settingsCommunity.codeforcesLostEnabled()

            override suspend fun requestBuilder() =
                CPSPeriodicWorkRequestBuilder<CodeforcesCommunityLostRecentWorker>(
                    repeatInterval = 30.minutes
                )

            override fun flowOfInfo() =
                combine(
                    flow = context.codeforcesLostRepository.flowOfSuspects(),
                    flow2 = WorkersHintsDataStore(context).codeforcesLostHintNotNew.asFlow()
                ) { suspects, hint ->
                    mapOf(
                        "suspects" to suspects.size,
                        "hint" to hint?.run { "($blogEntryId, $creationTime)" }
                    )
                }
        }
    }

    override suspend fun runWork() {
        context.settingsCommunity.fromSnapshot {
            checkRecentActions(
                locale = codeforcesLocale.value,
                minRatingColorTag = codeforcesLostMinRatingTag.value,
                repository = context.codeforcesLostRepository
            )
        }
    }

    private fun isNew(blogCreationTime: Instant) = workerStartTime - blogCreationTime < 24.hours

    private suspend fun checkRecentActions(
        locale: CodeforcesLocale,
        minRatingColorTag: CodeforcesColorTag,
        repository: CodeforcesLostRepository
    ) {
        val recentBlogEntries = CodeforcesClient(locale = locale).getRecentBlogEntries()
        //TODO: use api.recentActions on fail but !![only for findSuspects step]!!

        //get current suspects with removing old ones
        val suspects = repository.getSuspectsRemoveInvalid {
            isNew(it.creationTime) && it.author.colorTag >= minRatingColorTag
        }

        //catch new suspects from recent actions
        findSuspects(
            blogEntries = recentBlogEntries.filter { blogEntry -> suspects.none { it.id == blogEntry.id } },
            minRatingColorTag = minRatingColorTag,
            isNew = ::isNew,
            api = CodeforcesClient(locale = locale),
            hintStorage = hintsDataStore.codeforcesLostHintNotNew.asHintStorage()
        ) {
            repository.insertSuspect(it)
        }

        repository.checkSuspects(
            recentBlogEntries = recentBlogEntries,
            suspects = suspects,
            currentTime = workerStartTime,
            staleAfter = 7.days
        )
    }
}

private suspend inline fun CodeforcesLostRepository.getSuspectsRemoveInvalid(
    isValid: (CodeforcesWebBlogEntry) -> Boolean
): List<CodeforcesWebBlogEntry> =
    suspects()
        .partition(isValid)
        .let { (valid, invalid) ->
            remove(invalid)
            valid
        }

private suspend fun CodeforcesLostRepository.checkSuspects(
    recentBlogEntries: List<CodeforcesRecentFeedBlogEntry>,
    suspects: List<CodeforcesWebBlogEntry>,
    currentTime: Instant,
    staleAfter: Duration
) {
    val recentIds = recentBlogEntries.mapToSet { it.id }

    //remove from lost
    remove(
        lostEntries().filter {
            currentTime - it.creationTime > staleAfter || it.id in recentIds
        }
    )

    //suspect become lost
    suspects.forEach { blogEntry ->
        if (blogEntry.id !in recentIds) {
            insertLost(blogEntry)
        }
    }
}

private suspend fun CodeforcesPageContentProvider.getRecentBlogEntries(): List<CodeforcesRecentFeedBlogEntry> {
    suspend fun extractFrom(page: suspend () -> String) =
        CodeforcesUtils.extractRecentBlogEntries(source = page())

    // "/groups" has less size than "/recent" and hopefully will be cached by cf
    return runCatching {
        extractFrom(::getGroupsPage)
    }.getOrElse {
        extractFrom(::getRecentActionsPage)
    }
}

@Serializable
data class CodeforcesLostHint(
    val blogEntryId: Int,
    val creationTime: Instant
)

private abstract class CodeforcesLostHintStorage {
    abstract suspend fun getHint(): CodeforcesLostHint?

    protected abstract suspend fun update(transform: (CodeforcesLostHint?) -> CodeforcesLostHint)

    abstract suspend fun reset()

    suspend fun update(blogEntryId: Int, time: Instant) {
        update {
            if (it == null || it.creationTime < time) CodeforcesLostHint(blogEntryId, time)
            else it
        }
    }
}

private fun DataStoreItem<CodeforcesLostHint?>.asHintStorage(): CodeforcesLostHintStorage =
    object : CodeforcesLostHintStorage() {
        override suspend fun getHint(): CodeforcesLostHint? {
            return this@asHintStorage.invoke()
        }

        override suspend fun update(transform: (CodeforcesLostHint?) -> CodeforcesLostHint) {
            this@asHintStorage.update(transform)
        }

        override suspend fun reset() {
            this@asHintStorage.setValue(null)
        }
    }

private fun CodeforcesApi.withBlogEntriesCache(
    onNewBlogEntry: suspend (CodeforcesBlogEntry) -> Unit
): CodeforcesApi {
    val origin = this
    return object : CodeforcesApi by origin {
        private val cache = mutableMapOf<Int, CodeforcesBlogEntry>()

        private suspend inline fun getOrPut(id: Int, blogEntry: () -> CodeforcesBlogEntry): CodeforcesBlogEntry =
            cache.getOrPut(key = id) {
                blogEntry().also { onNewBlogEntry(it) }
            }

        override suspend fun getBlogEntry(blogEntryId: Int) =
            getOrPut(id = blogEntryId) {
                origin.getBlogEntry(blogEntryId)
            }

        override suspend fun getRecentActions(maxCount: Int) =
            origin.getRecentActions(maxCount = maxCount).apply {
                forEach {
                    it.blogEntry?.let { blogEntry ->
                        getOrPut(id = blogEntry.id) { blogEntry }
                    }
                }
            }
    }
}

private fun Collection<CodeforcesRecentFeedBlogEntry>.filterIdGreaterThan(id: Int) = filter { it.id > id }

private suspend fun Collection<CodeforcesRecentFeedBlogEntry>.fixAndFilterColorTag(
    minRatingColorTag: CodeforcesColorTag,
    api: CodeforcesApi
) = api.fixHandleColors(this).filter { it.author.colorTag >= minRatingColorTag }

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
    minRatingColorTag: CodeforcesColorTag,
    crossinline isNew: (Instant) -> Boolean,
    hintStorage: CodeforcesLostHintStorage,
    api: CodeforcesApi,
    onSuspect: (CodeforcesWebBlogEntry) -> Unit
) {
    val api = api.withBlogEntriesCache { blogEntry ->
        val time = blogEntry.creationTime
        if (!isNew(time)) {
            //save hint
            hintStorage.update(blogEntry.id, time)
        }
    }

    val hint = hintStorage.run {
        val hint = getHint()
        //ensure hint in case isNew logic changes
        if (hint != null && isNew(hint.creationTime)) {
            reset()
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
        .fixAndFilterColorTag(minRatingColorTag = minRatingColorTag, api = api)
        .also {
            if (it.size > 1) api.getRecentActions()
        }
        .filterNewEntries(isFinalFilter = true) { isNew(api.getBlogEntry(it.id).creationTime) }
        .forEach {
            val blogEntry = api.getBlogEntry(it.id)
            onSuspect(
                blogEntry
                    .copy(rating = 0)
                    .toWebBlogEntry(colorTag = it.author.colorTag)
            )
        }
}

//Required against new year color chaos
private suspend fun CodeforcesApi.fixHandleColors(blogEntries: Collection<CodeforcesRecentFeedBlogEntry>): List<CodeforcesRecentFeedBlogEntry> {
    val profiles = getProfiles(handles = blogEntries.map { it.author.handle }, recoverHandle = false)
    return blogEntries.map { blogEntry ->
        val profile = profiles.getValue(blogEntry.author.handle)
        require(profile is ProfileResult.Success) { "fixHandleColors: profile result is not success" }
        if (blogEntry.author.colorTag == ADMIN) blogEntry
        else {
            val colorTag = CodeforcesColorTag.fromRating(profile.userInfo.rating)
            blogEntry.copy(author = blogEntry.author.copy(colorTag = colorTag))
        }
    }
}
