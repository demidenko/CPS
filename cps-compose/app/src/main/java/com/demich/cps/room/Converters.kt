package com.demich.cps.room

import androidx.room.TypeConverter
import com.demich.cps.accounts.managers.CodeforcesUserInfo
import com.demich.cps.accounts.managers.STATUS
import com.demich.cps.utils.jsonCPS
import kotlinx.datetime.Instant
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class InstantSecondsConverter {
    @TypeConverter
    fun instantToSeconds(time: Instant): Long = time.epochSeconds

    @TypeConverter
    fun secondsToInstant(seconds: Long): Instant = Instant.fromEpochSeconds(seconds)
}

class IntsListConverter {
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

class CodeforcesUserInfoConverter {
    @TypeConverter
    fun userInfoToString(info: CodeforcesUserInfo): String {
        return jsonCPS.encodeToString(info)
    }

    @TypeConverter
    fun stringToUserInfo(str: String): CodeforcesUserInfo {
        return try {
            jsonCPS.decodeFromString(str)
        } catch (e: SerializationException) {
            CodeforcesUserInfo(
                status = STATUS.FAILED,
                handle = ""
            )
        }
    }
}