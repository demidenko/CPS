package com.demich.cps.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.clients.codeforces.CodeforcesClient
import com.demich.cps.platforms.codeforces.lost.CodeforcesLostBlogEntry
import com.demich.cps.platforms.codeforces.lost.CodeforcesLostBlogEntryFresh
import com.demich.cps.platforms.codeforces.lost.CodeforcesLostBlogEntrySuspect
import com.demich.cps.platforms.codeforces.lost.CodeforcesLostEntry
import com.demich.cps.platforms.codeforces.lost.CodeforcesLostHint
import com.demich.cps.platforms.codeforces.lost.CodeforcesLostHintStorage
import com.demich.cps.platforms.codeforces.lost.CodeforcesLostStorage
import com.demich.cps.platforms.codeforces.lost.updateEntries
import com.demich.cps.platforms.utils.codeforces.CodeforcesColorTag
import com.demich.cps.utils.jsonCPS
import com.demich.datastore_itemized.DataStoreItem
import com.demich.datastore_itemized.ItemizedDataStore
import com.demich.datastore_itemized.combine
import com.demich.datastore_itemized.dataStoreWrapper
import com.demich.datastore_itemized.edit
import com.demich.datastore_itemized.value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
                    flow = CodeforcesLostDataStore(context).flowOfEntries(),
                    flow2 = WorkersHintsDataStore(context).codeforcesLostHintNotNew.asFlow()
                ) { entries, hint ->
                    mapOf(
                        "hint" to hint?.run { "($blogEntryId, $creationTime)" },
                        "counters" to entries.counters()
                    )
                }
        }
    }

    override suspend fun runWork() {
        val lostStorage: CodeforcesLostStorage = CodeforcesLostDataStore(context)

        val client = CodeforcesClient(
            locale = context.settingsCommunity.codeforcesLocale()
        )

        val currentTime = workerStartTime
        lostStorage.updateEntries(
            api = client,
            pageContentProvider = client,
            hintStorage = hintsDataStore.codeforcesLostHintNotNew.asHintStorage(),
            isFresh = { currentTime - it < 24.hours },
            isStale = { currentTime - it > 7.days },
            trustColorTags = false
        )
    }
}

@Serializable
private data class Suspect(
    @SerialName("id") val blogEntryId: Int,
    @SerialName("tag") val authorColorTag: CodeforcesColorTag?
)

private fun Suspect.toPublic() = CodeforcesLostBlogEntrySuspect(blogEntryId = blogEntryId, authorColorTag = authorColorTag)
private fun CodeforcesLostBlogEntrySuspect.toPrivate() = Suspect(blogEntryId = blogEntryId, authorColorTag = authorColorTag)

@Serializable
private data class Fresh(
    @SerialName("entry") val blogEntry: CodeforcesBlogEntry,
    @SerialName("tag") val authorColorTag: CodeforcesColorTag?
)

private fun Fresh.toPublic() = CodeforcesLostBlogEntryFresh(blogEntry = blogEntry, authorColorTag = authorColorTag)
private fun CodeforcesLostBlogEntryFresh.toPrivate() = Fresh(blogEntry = blogEntry, authorColorTag = authorColorTag)

@Serializable
private data class Lost(
    @SerialName("entry")val blogEntry: CodeforcesBlogEntry,
    @SerialName("tag") val authorColorTag: CodeforcesColorTag?,
    @SerialName("stamp") val timeStamp: Instant
)

private fun Lost.toPublic() = CodeforcesLostBlogEntry(blogEntry = blogEntry, authorColorTag = authorColorTag, timeStamp = timeStamp)
private fun CodeforcesLostBlogEntry.toPrivate() = Lost(blogEntry = blogEntry, authorColorTag = authorColorTag, timeStamp = timeStamp)

class CodeforcesLostDataStore(context: Context):
    ItemizedDataStore(context.cf_lost_dataStore),
    CodeforcesLostStorage
{
    companion object {
        private val Context.cf_lost_dataStore by dataStoreWrapper(name = "cf_lost")
    }

    private val suspects = jsonCPS.itemList<Suspect>(name = "suspects")
    private val fresh = jsonCPS.itemList<Fresh>(name = "fresh")
    private val lost = jsonCPS.itemList<Lost>(name = "lost")

    private val all = combine {
        buildMap<Int, CodeforcesLostEntry> {
            suspects.value.forEach { put(it.blogEntryId, it.toPublic()) }
            fresh.value.forEach { put(it.blogEntry.id, it.toPublic()) }
            lost.value.forEach { put(it.blogEntry.id, it.toPublic()) }
        }
    }

    override suspend fun getEntries() = all()

    override suspend fun update(transform: (Map<Int, CodeforcesLostEntry>) -> Map<Int, CodeforcesLostEntry>) {
        edit {
            val result = transform(all.value).values
            suspects.value = result.mapNotNull { (it as? CodeforcesLostBlogEntrySuspect)?.toPrivate() }
            fresh.value = result.mapNotNull { (it as? CodeforcesLostBlogEntryFresh)?.toPrivate() }
            lost.value = result.mapNotNull { (it as? CodeforcesLostBlogEntry)?.toPrivate() }
        }
    }

    fun flowOfLostEntries(): Flow<List<CodeforcesLostBlogEntry>> =
        lost.asFlow().map { it.map { it.toPublic() } }

    fun flowOfEntries(): Flow<Collection<CodeforcesLostEntry>> = all.asFlow().map { it.values }
}

private fun Collection<CodeforcesLostEntry>.counters(): String {
    val suspects = count { it is CodeforcesLostBlogEntrySuspect }
    val fresh = count { it is CodeforcesLostBlogEntryFresh }
    val lost = count { it is CodeforcesLostBlogEntry }
    return "$suspects / $fresh / $lost"
}

private fun DataStoreItem<CodeforcesLostHint?>.asHintStorage(): CodeforcesLostHintStorage =
    object : CodeforcesLostHintStorage {
        override suspend fun getValue(): CodeforcesLostHint? {
            return this@asHintStorage.invoke()
        }

        override suspend fun update(transform: (CodeforcesLostHint?) -> CodeforcesLostHint?) {
            this@asHintStorage.update(transform)
        }
    }

