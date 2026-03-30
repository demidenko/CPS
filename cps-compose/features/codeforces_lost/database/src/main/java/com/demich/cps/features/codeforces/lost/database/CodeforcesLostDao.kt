package com.demich.cps.features.codeforces.lost.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.demich.cps.platforms.utils.codeforces.CodeforcesWebBlogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Instant

val Context.codeforcesLostRepository: CodeforcesLostRepository
    get() = lostDataBase.lostBlogEntriesDao()

interface CodeforcesLostRepository {
    suspend fun insertSuspect(blogEntry: CodeforcesWebBlogEntry)

    suspend fun insertLost(blogEntry: CodeforcesWebBlogEntry)

    suspend fun remove(blogEntries: Collection<CodeforcesWebBlogEntry>)

    fun flowOfLost(): Flow<List<CodeforcesWebBlogEntry>>

    suspend fun lostEntries(): List<CodeforcesWebBlogEntry>

    suspend fun suspects(): List<CodeforcesWebBlogEntry>
}

internal const val cfLostTableName = "BlogEntries"
private const val selectSuspect = "SELECT * FROM $cfLostTableName where isSuspect"

@Dao
internal interface CodeforcesLostDao: CodeforcesLostRepository {
    @Upsert
    suspend fun insert(blogEntry: CodeforcesLostBlogEntry)

    override suspend fun insertLost(blogEntry: CodeforcesWebBlogEntry) {
        insert(CodeforcesLostBlogEntry(
            blogEntry = blogEntry.toDeprecatedBlogEntry(),
            isSuspect = false,
            timeStamp = Clock.System.now(),
        ))
    }

    override suspend fun insertSuspect(blogEntry: CodeforcesWebBlogEntry) {
        insert(CodeforcesLostBlogEntry(
            blogEntry = blogEntry.toDeprecatedBlogEntry(),
            isSuspect = true,
            timeStamp = Instant.DISTANT_PAST,
        ))
    }

    @Delete(entity = CodeforcesLostBlogEntry::class)
    suspend fun removeByIds(ids: List<IdHolder>)

    @Query("$selectSuspect = 0")
    suspend fun daoGetLost(): List<CodeforcesLostBlogEntry>

    @Query("$selectSuspect = 0")
    fun daoFlowOfLost(): Flow<List<CodeforcesLostBlogEntry>>

    @Query("$selectSuspect = 1")
    suspend fun daoGetSuspects(): List<CodeforcesLostBlogEntry>

    override suspend fun remove(blogEntries: Collection<CodeforcesWebBlogEntry>) {
        removeByIds(blogEntries.map { IdHolder(it.id) })
    }

    override fun flowOfLost(): Flow<List<CodeforcesWebBlogEntry>> =
        daoFlowOfLost().map { list ->
            list.sortedByDescending { it.timeStamp }
                .map { it.blogEntry.toWebBlogEntry() }
        }

    override suspend fun lostEntries(): List<CodeforcesWebBlogEntry> =
        daoGetLost().map { it.blogEntry.toWebBlogEntry() }

    override suspend fun suspects(): List<CodeforcesWebBlogEntry> =
        daoGetSuspects().map { it.blogEntry.toWebBlogEntry() }
}

internal class IdHolder(val id: Int)