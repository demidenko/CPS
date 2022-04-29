package com.demich.cps.room

import android.content.Context
import androidx.room.*
import com.demich.cps.contests.Contest
import kotlinx.coroutines.flow.Flow

val Context.contestsListDao: ContestsListDao
    get() = RoomSingleton.getInstance(this).contestsListDao()

@Dao
interface ContestsListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contests: List<Contest>)

    @Delete
    suspend fun remove(contests: List<Contest>)

    @Query("delete from $contestsListTableName where platform = :platform")
    suspend fun remove(platform: Contest.Platform)

    suspend fun replace(platform: Contest.Platform, contests: List<Contest>) {
        require(contests.all { it.platform == platform })
        val ids = contests.map { it.id }.toSet()
        getContests(platform)
            .filter { it.id !in ids }
            .takeIf { it.isNotEmpty() }
            ?.let { remove(it) }
        insert(contests)
    }

    @Query("select * from $contestsListTableName")
    fun flowOfContests(): Flow<List<Contest>>

    @Query("select * from $contestsListTableName where platform = :platform")
    suspend fun getContests(platform: Contest.Platform): List<Contest>

    companion object {
        const val contestsListTableName = "contests_list"
    }
}
