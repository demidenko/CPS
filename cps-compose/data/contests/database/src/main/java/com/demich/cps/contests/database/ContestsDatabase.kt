package com.demich.cps.contests.database

import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import com.demich.cps.features.room.DurationSecondsConverter
import com.demich.cps.features.room.InstanceProvider
import com.demich.cps.features.room.InstantSecondsConverter


@Database(
    entities = [Contest::class],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = ContestsDatabase.DurationRenameMigration::class),
        AutoMigration(from = 2, to = 3)
    ]
)
@TypeConverters(
    InstantSecondsConverter::class,
    DurationSecondsConverter::class
)
internal abstract class ContestsDatabase: RoomDatabase() {
    abstract fun contestsDao(): ContestsListDao

    companion object: InstanceProvider<ContestsDatabase>({
        Room.databaseBuilder(
            name = "contests_db",
            klass = ContestsDatabase::class.java,
            context = it
        )
    })

    @RenameColumn(tableName = contestsTableName, fromColumnName = "durationSeconds", toColumnName = "duration")
    class DurationRenameMigration: AutoMigrationSpec
}