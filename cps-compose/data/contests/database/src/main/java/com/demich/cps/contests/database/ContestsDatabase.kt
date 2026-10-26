package com.demich.cps.contests.database

import android.content.Context
import androidx.room3.AutoMigration
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RenameColumn
import androidx.room3.RoomDatabase
import androidx.room3.migration.AutoMigrationSpec
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.demich.cps.features.room.DurationSecondsConverter
import com.demich.cps.features.room.InstantSecondsConverter
import com.demich.cps.features.room.instanceDelegate


@Database(
    entities = [Contest::class],
    version = 4,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = ContestsDatabase.DurationRenameMigration::class),
        AutoMigration(from = 2, to = 3)
    ]
)
@ColumnTypeConverters(
    InstantSecondsConverter::class,
    DurationSecondsConverter::class
)
internal abstract class ContestsDatabase: RoomDatabase() {
    abstract fun contestsDao(): ContestsListDao

    @RenameColumn(tableName = "contests_list", fromColumnName = "durationSeconds", toColumnName = "duration")
    class DurationRenameMigration: AutoMigrationSpec
}

internal val Context.contestsDatabase by instanceDelegate<ContestsDatabase>(
    name = "contests_db",
    migrations = { listOf(migration_3_4_AddEndTimeColumn) }
)

private val migration_3_4_AddEndTimeColumn get() = object : Migration(3, 4) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE contests_list ADD COLUMN endTime INTEGER NOT NULL DEFAULT 0")
    }
}
