package com.demich.cps.features.codeforces.lost.database

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.demich.cps.features.room.InstantSecondsConverter
import com.demich.cps.features.room.RoomJsonConverter
import com.demich.cps.features.room.instanceDelegate


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
}

internal val Context.lostDataBase by instanceDelegate<CodeforcesLostDataBase>(
    name = "codeforces_lost_db"
)

internal class CodeforcesBlogEntryConverter: RoomJsonConverter<DeprecatedCodeforcesBlogEntry>() {
    @TypeConverter
    override fun decode(str: String): DeprecatedCodeforcesBlogEntry = decodeFromString(str)

    @TypeConverter
    override fun encode(value: DeprecatedCodeforcesBlogEntry) = encodeToString(value)
}