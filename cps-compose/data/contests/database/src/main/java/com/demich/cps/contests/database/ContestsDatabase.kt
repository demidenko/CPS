package com.demich.cps.contests.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.demich.cps.features.room.InstanceProvider
import com.demich.cps.features.room.InstantSecondsConverter


@Database(
    entities = [Contest::class],
    version = 1
)
@TypeConverters(
    InstantSecondsConverter::class
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
}