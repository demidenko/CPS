package com.demich.cps.features.codeforces.follow.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.demich.cps.profiles.userinfo.CodeforcesUserInfo

@Entity(tableName = cfFollowTableName)
data class CodeforcesUserBlog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val handle: String,
    internal val blogEntries: List<Int>?,
    val userInfo: CodeforcesUserInfo?
)

val CodeforcesUserBlog.blogSize: Int?
    get() = blogEntries?.size