package com.demich.cps.features.codeforces.follow.database

import androidx.room.TypeConverter
import com.demich.cps.accounts.userinfo.CodeforcesUserInfo
import com.demich.cps.features.room.RoomJsonConverter
import com.demich.cps.features.room.jsonRoom
import kotlinx.serialization.SerializationException


internal class IntsListConverter {
    @TypeConverter
    fun intsToString(ints: List<Int>?): String? {
        if (ints == null) return null
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

    @TypeConverter
    fun decodeToInts(s: String?): List<Int>? {
        if (s == null) return null
        return (s.indices step 4).map { i ->
            ((s[i+3].code*256 + s[i+2].code)*256 + s[i+1].code)*256 + s[i].code
        }
    }
}

internal class CodeforcesUserInfoConverter: RoomJsonConverter<CodeforcesUserInfo?> {
    @TypeConverter
    override fun encode(value: CodeforcesUserInfo?) = jsonRoom.encodeToString(value)

    @TypeConverter
    override fun decode(str: String): CodeforcesUserInfo? {
        return try {
            jsonRoom.decodeFromString(str)
        } catch (e: SerializationException) {
            null
        }
    }
}