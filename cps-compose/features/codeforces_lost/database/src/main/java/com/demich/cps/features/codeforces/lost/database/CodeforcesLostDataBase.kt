package com.demich.cps.features.codeforces.lost.database

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.demich.cps.features.room.DeprecatedInstantSecondsConverter
import com.demich.cps.features.room.RoomJsonConverter
import com.demich.cps.features.room.instanceDelegate
import com.demich.cps.features.room.jsonRoom
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry


@Database(
    entities = [CodeforcesLostBlogEntry::class],
    version = 1
)
@TypeConverters(
    DeprecatedInstantSecondsConverter::class,
    CodeforcesBlogEntryConverter::class
)
internal abstract class CodeforcesLostDataBase: RoomDatabase() {
    abstract fun lostBlogEntriesDao(): CodeforcesLostDao
}

internal val Context.lostDataBase by instanceDelegate<CodeforcesLostDataBase>(
    name = "codeforces_lost_db"
)

internal class CodeforcesBlogEntryConverter: RoomJsonConverter<CodeforcesBlogEntry> {
    @TypeConverter
    override fun decode(str: String) = jsonRoom.decodeFromString<CodeforcesBlogEntry>(str)

    @TypeConverter
    override fun encode(value: CodeforcesBlogEntry) = jsonRoom.encodeToString(value)
}