package com.demich.cps.features.codeforces.follow.database

import androidx.room.*
import com.demich.cps.features.room.InstanceProvider

@Database(
    entities = [CodeforcesUserBlog::class],
    version = 1
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