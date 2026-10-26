package com.demich.cps.features.codeforces.follow.database

import android.content.Context
import androidx.room3.AutoMigration
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RenameColumn
import androidx.room3.RoomDatabase
import androidx.room3.migration.AutoMigrationSpec
import com.demich.cps.features.room.instanceDelegate

@Database(
    entities = [CodeforcesUserBlogEntity::class],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = CodeforcesFollowDataBase.BlogEntriesIdsRenameMigration::class)
    ]
)
@ColumnTypeConverters(
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