package com.example.test3.room

import android.content.Context
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.test3.contests.Contest
import kotlinx.datetime.Instant

@Database(
    entities = [
        LostBlogEntry::class,
        CodeforcesUserBlog::class,
        Contest::class
    ],
    version = 5,
    autoMigrations = [
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5, spec = RoomSingleton.RenameTimeAutoMigration::class),
    ]
)
@TypeConverters(
    IntsListConverter::class,
    CodeforcesUserInfoConverter::class,
    InstantSecondsConverter::class
)
abstract class RoomSingleton: RoomDatabase(){
    abstract fun lostBlogsDao(): LostBlogsDao
    abstract fun followListDao(): FollowListDao
    abstract fun contestsListDao(): ContestsListDao

    companion object {
        private var instance: RoomSingleton? = null
        fun getInstance(context: Context): RoomSingleton {
            return instance
                ?: Room.databaseBuilder(context, RoomSingleton::class.java, "CPSdb")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
        }

        private val MIGRATION_1_2 get() = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE `cf_follow_list` (`id` INTEGER PRIMARY KEY autoincrement NOT NULL, `handle` TEXT NOT NULL, `blogs` TEXT)")
            }
        }

        private val MIGRATION_2_3 get() = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `cf_follow_list` ADD COLUMN userInfo TEXT NOT NULL DEFAULT ''")
            }
        }
    }

    @RenameColumn(tableName = lostBlogsTableName, fromColumnName = "creationTimeSeconds", toColumnName = "creationTime")
    @RenameColumn(tableName = contestsListTableName, fromColumnName = "startTimeSeconds", toColumnName = "startTime")
    class RenameTimeAutoMigration: AutoMigrationSpec
}

class InstantSecondsConverter {
    @TypeConverter
    fun instantToSeconds(time: Instant): Long = time.epochSeconds

    @TypeConverter
    fun secondsToInstant(seconds: Long): Instant = Instant.fromEpochSeconds(seconds)
}