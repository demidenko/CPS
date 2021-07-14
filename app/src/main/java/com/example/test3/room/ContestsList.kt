package com.example.test3.room

import android.content.Context
import androidx.room.*
import com.example.test3.contests.Contest
import kotlinx.coroutines.flow.Flow

fun getContestsListDao(context: Context) = RoomSingleton.getInstance(context).contestsListDao()

const val contestsListTableName = "contests_list"

@Dao
interface ContestsListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contests: List<Contest>)

    @Delete
    suspend fun remove(contests: List<Contest>)

    suspend fun replace(platform: Contest.Platform, contests: List<Contest>) {
        require(contests.all { it.platform == platform })
        val ids = contests.mapTo(mutableSetOf()){ it.id }
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
}
