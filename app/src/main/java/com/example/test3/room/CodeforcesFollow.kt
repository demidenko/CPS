package com.example.test3.room

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

const val followListTableName = "cf_follow_list"

@Dao
interface FollowListDao {

}

@Entity(tableName = followListTableName)
class UserBlogs(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val handle: String,
    val blogs: List<Int>?
)

class IntsListConverter {
    @TypeConverter
    fun intsToString(ints: List<Int>?): String? {
        if(ints == null) return null
        return buildString {
            ints.forEach { num ->
                var x = num
                repeat(4){
                    append((x%256).toChar())
                    x/=256
                }
            }
        }
    }

    @TypeConverter
    fun decodeToInts(s: String?): List<Int>? {
        if(s == null) return null
        return (s.indices step 4).map { i ->
            ((s[i+3].toInt()*256 + s[i+2].toInt())*256 + s[i+1].toInt())*256 + s[i].toInt()
        }
    }
}