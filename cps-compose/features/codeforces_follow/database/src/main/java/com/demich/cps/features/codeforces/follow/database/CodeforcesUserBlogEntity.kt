package com.demich.cps.features.codeforces.follow.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.demich.cps.profiles.userinfo.CodeforcesUserInfo
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.handle

@Entity(tableName = cfFollowTableName)
internal data class CodeforcesUserBlogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val handle: String,

    val userInfo: CodeforcesUserInfo?,

    internal val blogEntries: Set<Int>?
)

data class CodeforcesUserBlog(
    val userProfile: ProfileResult<CodeforcesUserInfo>,
    val blogSize: Int?,
    val id: Long
)

val CodeforcesUserBlog.handle: String
    get() = userProfile.handle

internal fun CodeforcesUserBlogEntity.toCodeforcesUserBlog() =
    CodeforcesUserBlog(
        id = id,
        blogSize = blogEntries?.size,
        userProfile = when (userInfo) {
            null -> ProfileResult.Failed(userId = handle)
            else -> ProfileResult(userInfo = userInfo)
        }
    )

// TODO: replace blogEntries to this pair
internal data class BlogInfo(
    val size: Int,
    val savedIds: Set<Int>
)