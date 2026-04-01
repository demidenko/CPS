package com.demich.cps.features.codeforces.follow.database

import androidx.room.TypeConverter
import com.demich.cps.features.room.RoomJsonConverter
import com.demich.cps.profiles.userinfo.CodeforcesUserInfo
import kotlinx.serialization.SerializationException


internal class IntCollectionAsBytesConverter {
    @TypeConverter
    fun intsToString(ints: Collection<Int>): String {
        return buildString {
            ints.forEach { num ->
                var x = num
                repeat(4) {
                    append((x%256).toChar())
                    x/=256
                }
            }
        }
    }

    private inline fun String.extractInts(block: (Int) -> Unit) {
        val s = this
        indices.step(4).forEach { i ->
            val int = ((s[i+3].code*256 + s[i+2].code)*256 + s[i+1].code)*256 + s[i].code
            block(int)
        }
    }

    @TypeConverter
    fun decodeToList(s: String): List<Int> =
        buildList { s.extractInts(::add) }

    @TypeConverter
    fun decodeToSet(s: String): Set<Int> =
        buildSet { s.extractInts(::add) }
}

internal class CodeforcesUserInfoConverter: RoomJsonConverter<CodeforcesUserInfo?>() {
    @TypeConverter
    override fun encode(value: CodeforcesUserInfo?) = encodeToString(value)

    @TypeConverter
    override fun decode(str: String): CodeforcesUserInfo? {
        return try {
            decodeFromString(str)
        } catch (e: SerializationException) {
            null
        }
    }
}