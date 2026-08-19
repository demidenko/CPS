package com.demich.cps.features.codeforces.follow.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.demich.cps.platforms.codeforces.follow.storage.CodeforcesUserBlog
import com.demich.cps.profiles.userinfo.CodeforcesUserInfo
import com.demich.cps.profiles.userinfo.ProfileResult

@Entity(tableName = cfFollowTableName)
internal data class CodeforcesUserBlogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val handle: String,

    val userInfo: CodeforcesUserInfo?,

    @Embedded
    val blogInfo: BlogInfo?
)

internal data class BlogInfo(
    val blogSize: Int,
    val savedIds: Set<Int>
)

internal class CodeforcesUserBlogEntityShort(
    val id: Long,
    val handle: String,
    val userInfo: CodeforcesUserInfo?,
    val blogSize: Int?
)

internal fun CodeforcesUserBlogEntity.toCodeforcesUserBlog() =
    CodeforcesUserBlog(
        id = id,
        blogSize = blogInfo?.blogSize,
        userProfile = when (userInfo) {
            null -> ProfileResult.Failed(userId = handle)
            else -> ProfileResult(userInfo = userInfo)
        }
    )

internal fun CodeforcesUserBlogEntityShort.toCodeforcesUserBlog() =
    CodeforcesUserBlog(
        id = id,
        blogSize = blogSize,
        userProfile = when (userInfo) {
            null -> ProfileResult.Failed(userId = handle)
            else -> ProfileResult(userInfo = userInfo)
        }
    )