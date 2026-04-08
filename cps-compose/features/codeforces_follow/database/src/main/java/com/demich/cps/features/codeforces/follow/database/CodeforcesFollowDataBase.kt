package com.demich.cps.features.codeforces.follow.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import com.demich.cps.features.room.instanceDelegate

@Database(
    entities = [CodeforcesUserBlogEntity::class],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = CodeforcesFollowDataBase.BlogEntriesIdsRenameMigration::class)
    ]
)
@TypeConverters(
    IntCollectionAsBytesConverter::class,
    CodeforcesUserInfoConverter::class
)
internal abstract class CodeforcesFollowDataBase: RoomDatabase() {
    abstract fun followListDao(): CodeforcesFollowDao


    @RenameColumn(tableName = "FollowList", fromColumnName = "blogEntries", toColumnName = "savedIds")
    class BlogEntriesIdsRenameMigration: AutoMigrationSpec
}

internal val Context.followDataBase by instanceDelegate<CodeforcesFollowDataBase>(
    name = "codeforces_follow_db"
)