package com.demich.cps.features.codeforces.lost.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.demich.cps.features.room.InstanceProvider
import com.demich.cps.features.room.InstantSecondsConverter
import com.demich.cps.features.room.RoomJsonConverter
import com.demich.cps.features.room.jsonRoom
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry


@Database(
    entities = [CodeforcesLostBlogEntry::class],
    version = 1
)
@TypeConverters(
    InstantSecondsConverter::class,
    CodeforcesBlogEntryConverter::class
)
internal abstract class CodeforcesLostDataBase: RoomDatabase() {
    abstract fun lostBlogEntriesDao(): CodeforcesLostDao

    companion object: InstanceProvider<CodeforcesLostDataBase>({
        Room.databaseBuilder<CodeforcesLostDataBase>(
            name = "codeforces_lost_db",
            context = it
        )
    })
}

internal class CodeforcesBlogEntryConverter: RoomJsonConverter<CodeforcesBlogEntry> {
    @TypeConverter
    override fun decode(str: String) = jsonRoom.decodeFromString<CodeforcesBlogEntry>(str)

    @TypeConverter
    override fun encode(value: CodeforcesBlogEntry) = jsonRoom.encodeToString(value)
}