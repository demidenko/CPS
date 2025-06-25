package com.demich.cps.contests.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.demich.cps.features.room.DurationSecondsConverter
import com.demich.cps.features.room.InstanceProvider
import com.demich.cps.features.room.InstantSecondsConverter


@Database(
    entities = [Contest::class],
    version = 4,
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
        Room.databaseBuilder<ContestsDatabase>(
            name = "contests_db",
            context = it
        ).addMigrations(migration_3_4_AddEndTimeColumn)
    })

    @RenameColumn(tableName = "contests_list", fromColumnName = "durationSeconds", toColumnName = "duration")
    class DurationRenameMigration: AutoMigrationSpec

}

private val migration_3_4_AddEndTimeColumn get() = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE contests_list ADD COLUMN endTime INTEGER NOT NULL DEFAULT 0")
    }
}
