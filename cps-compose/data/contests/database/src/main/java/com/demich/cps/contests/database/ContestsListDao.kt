package com.demich.cps.contests.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

val Context.contestsListDao: ContestsListDao
    get() = ContestsDatabase.getInstance(this).contestsDao()

internal const val contestsTableName = "contests_list"

@Dao
abstract class ContestsListDao {
    @Upsert
    protected abstract suspend fun insert(contests: List<Contest>)

    @Delete
    protected abstract  suspend fun remove(contests: List<Contest>)

    //@Query("delete from $contestsTableName where platform = :platform")
    //abstract suspend fun remove(platform: Contest.Platform)

    @Transaction
    open suspend fun replace(platform: Contest.Platform, contests: List<Contest>) {
        require(contests.all { it.platform == platform })
        val ids = contests.mapTo(mutableSetOf()) { it.id }
        remove(getContests(platform).filter { it.id !in ids })
        insert(contests)
    }

    @Query("select * from $contestsTableName")
    abstract fun flowOfContests(): Flow<List<Contest>>

    @Query("select * from $contestsTableName where platform = :platform")
    abstract suspend fun getContests(platform: Contest.Platform): List<Contest>

    @Query("select * from $contestsTableName where platform = :platform and endTime > :currentTime")
    abstract suspend fun getContestsNotFinished(platform: Contest.Platform, currentTime: Instant): List<Contest>

}