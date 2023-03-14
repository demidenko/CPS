package com.demich.cps.features.codeforces.lost.database

import android.content.Context
import androidx.room.*
import com.demich.cps.data.api.CodeforcesBlogEntry
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


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

    companion object {
        private var instance: CodeforcesLostDataBase? = null
        fun getInstance(context: Context): CodeforcesLostDataBase {
            return instance ?: Room.databaseBuilder(
                name = "codeforces_lost_db",
                klass = CodeforcesLostDataBase::class.java,
                context = context
            ).build().also { instance = it }
        }
    }
}

internal class InstantSecondsConverter {
    @TypeConverter
    fun instantToSeconds(time: Instant): Long = time.epochSeconds

    @TypeConverter
    fun secondsToInstant(seconds: Long): Instant = Instant.fromEpochSeconds(seconds)
}


private val jsonRoom = Json {
    encodeDefaults = true
    allowStructuredMapKeys = true
}

internal class CodeforcesBlogEntryConverter {
    @TypeConverter
    fun toString(blogEntry: CodeforcesBlogEntry): String {
        return jsonRoom.encodeToString(blogEntry)
    }

    @TypeConverter
    fun toBlogEntry(str: String): CodeforcesBlogEntry {
        return jsonRoom.decodeFromString(str)
    }
}