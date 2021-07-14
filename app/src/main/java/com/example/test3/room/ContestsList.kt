package com.example.test3.room

import android.content.Context
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.test3.contests.Contest
import kotlinx.coroutines.flow.Flow

fun getContestsListDao(context: Context) = RoomSingleton.getInstance(context).contestsListDao()

const val contestsListTableName = "contests_list"

@Dao
interface ContestsListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contests: List<Contest>)

    @Query("select * from $contestsListTableName")
    fun flowOfContests(): Flow<List<Contest>>

    @Query("select * from $contestsListTableName where platform = :platform")
    suspend fun getContests(platform: Contest.Platform): List<Contest>
}
