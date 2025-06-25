package com.demich.cps.features.codeforces.follow.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.demich.cps.features.room.instanceDelegate

@Database(
    entities = [CodeforcesUserBlog::class],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
@TypeConverters(
    IntsListConverter::class,
    CodeforcesUserInfoConverter::class
)
internal abstract class CodeforcesFollowDataBase: RoomDatabase() {
    abstract fun followListDao(): CodeforcesFollowDao
}

internal val Context.followDataBase by
    instanceDelegate<CodeforcesFollowDataBase>(name = "codeforces_follow_db")