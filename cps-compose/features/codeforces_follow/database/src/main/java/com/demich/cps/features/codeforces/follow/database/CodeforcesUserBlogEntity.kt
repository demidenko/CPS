package com.demich.cps.features.codeforces.follow.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.demich.cps.profiles.userinfo.CodeforcesUserInfo
import com.demich.cps.profiles.userinfo.ProfileResult

@Entity(tableName = cfFollowTableName)
data class CodeforcesUserBlogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val handle: String,

    val userInfo: CodeforcesUserInfo?,

    internal val blogEntries: Set<Int>?
)

val CodeforcesUserBlogEntity.blogSize: Int?
    get() = blogEntries?.size

val CodeforcesUserBlogEntity.profileResult: ProfileResult<CodeforcesUserInfo>
    get() = when (userInfo) {
        null -> ProfileResult.Failed(userId = handle)
        else -> ProfileResult(userInfo = userInfo)
    }

// TODO: replace blogEntries to this pair
internal data class BlogInfo(
    val size: Int,
    val savedIds: Set<Int>
)