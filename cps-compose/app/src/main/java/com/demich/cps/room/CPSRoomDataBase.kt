package com.demich.cps.room

import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import com.demich.cps.contests.Contest
import com.demich.cps.features.room.InstanceProvider
import com.demich.cps.features.room.InstantSecondsConverter

@Database(
    entities = [Contest::class],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = RoomSingleton.DeleteLostMigration::class),
        AutoMigration(from = 2, to = 3, spec = RoomSingleton.DeleteFollowMigration::class)
    ]
)
@TypeConverters(
    InstantSecondsConverter::class
)
internal abstract class RoomSingleton: RoomDatabase() {
    abstract fun contestsListDao(): ContestsListDao

    companion object: InstanceProvider<RoomSingleton>({
        Room.databaseBuilder(
            name = "CPSdb",
            klass = RoomSingleton::class.java,
            context = it
        )
    })

    @DeleteTable(tableName = "cf_lost_blog_entries")
    class DeleteLostMigration : AutoMigrationSpec

    @DeleteTable(tableName = "cf_follow_list")
    class DeleteFollowMigration : AutoMigrationSpec
}

