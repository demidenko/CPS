package com.demich.cps.contests.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

val Context.contestsListDao: ContestsListDao
    get() = contestsDatabase.contestsDao()

internal const val contestsTableName = "contests_list"

private const val SQLITE_MAX_VARIABLE_NUMBER = 1000 //actually is 32k https://www.sqlite.org/limits.html#9

@Dao
abstract class ContestsListDao {
    @Upsert
    protected abstract suspend fun insert(contests: List<Contest>)

    @Delete
    protected abstract suspend fun remove(contests: List<Contest>)

    //@Query("delete from $contestsTableName where platform = :platform")
    //abstract suspend fun remove(platform: Contest.Platform)

    @Query("delete from $contestsTableName where platform = :platform and id not in (:ids)")
    protected abstract suspend fun __queryRemoveNotIn(platform: Contest.Platform, ids: Set<String>)

    private suspend fun removeNotIn(platform: Contest.Platform, ids: Set<String>) {
        if (ids.size < SQLITE_MAX_VARIABLE_NUMBER) {
            __queryRemoveNotIn(platform, ids)
        } else {
            remove(getContests(platform).filter { it.id !in ids })
        }
    }

    @Transaction
    open suspend fun replace(platform: Contest.Platform, contests: List<Contest>) {
        require(contests.all { it.platform == platform })
        val ids = contests.mapTo(mutableSetOf()) { it.id }
        removeNotIn(platform, ids)
        insert(contests)
    }

    @Query("select * from $contestsTableName")
    abstract fun flowOfContests(): Flow<List<Contest>>

    @Query("select * from $contestsTableName where platform = :platform")
    abstract suspend fun getContests(platform: Contest.Platform): List<Contest>

    @Query("select * from $contestsTableName where platform = :platform and endTime > :currentTime")
    abstract suspend fun getContestsNotFinished(platform: Contest.Platform, currentTime: Instant): List<Contest>

}