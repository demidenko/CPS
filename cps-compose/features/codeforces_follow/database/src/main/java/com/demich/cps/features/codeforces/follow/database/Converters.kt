package com.demich.cps.features.codeforces.follow.database

import androidx.room3.ColumnTypeConverter
import com.demich.cps.features.room.RoomJsonConverter
import com.demich.cps.profiles.userinfo.CodeforcesUserInfo
import kotlinx.serialization.SerializationException


internal class IntCollectionAsBytesConverter {
    @ColumnTypeConverter
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

    private fun String.intCollectionSize(): Int = length / 4

    private fun MutableCollection<Int>.extractInts(s: String) {
        s.indices.step(4).forEach { i ->
            val int = ((s[i+3].code*256 + s[i+2].code)*256 + s[i+1].code)*256 + s[i].code
            add(int)
        }
    }

    @ColumnTypeConverter
    fun decodeIntList(s: String): List<Int> =
        buildList(capacity = s.intCollectionSize()) { extractInts(s) }

    @ColumnTypeConverter
    fun decodeIntSet(s: String): Set<Int> =
        buildSet(capacity = s.intCollectionSize()) { extractInts(s) }
}

internal class CodeforcesUserInfoConverter: RoomJsonConverter<CodeforcesUserInfo?>() {
    @ColumnTypeConverter
    override fun encode(value: CodeforcesUserInfo?) = encodeToString(value)

    @ColumnTypeConverter
    override fun decode(str: String): CodeforcesUserInfo? {
        return try {
            decodeFromString(str)
        } catch (e: SerializationException) {
            null
        }
    }
}