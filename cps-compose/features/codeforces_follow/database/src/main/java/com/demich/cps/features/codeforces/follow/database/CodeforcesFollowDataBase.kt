package com.demich.cps.features.codeforces.follow.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.demich.cps.features.room.InstanceProvider

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

    companion object: InstanceProvider<CodeforcesFollowDataBase>({
        Room.databaseBuilder(
            name = "codeforces_follow_db",
            klass = CodeforcesFollowDataBase::class.java,
            context = it
        )
    })
}