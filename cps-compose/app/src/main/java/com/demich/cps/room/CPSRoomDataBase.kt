package com.demich.cps.room

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.demich.cps.contests.Contest
import com.demich.cps.features.room.InstanceProvider
import com.demich.cps.features.room.InstantSecondsConverter

@Database(
    entities = [
        Contest::class,
        CodeforcesUserBlog::class,
        CodeforcesLostBlogEntry::class,
    ],
    version = 1
)
@TypeConverters(
    IntsListConverter::class,
    CodeforcesUserInfoConverter::class,
    InstantSecondsConverter::class
)
abstract class RoomSingleton: RoomDatabase() {
    abstract fun followListDao(): FollowListDao
    abstract fun contestsListDao(): ContestsListDao

    companion object: InstanceProvider<RoomSingleton>({
        Room.databaseBuilder(
            name = "CPSdb",
            klass = RoomSingleton::class.java,
            context = it
        )
    })
}

