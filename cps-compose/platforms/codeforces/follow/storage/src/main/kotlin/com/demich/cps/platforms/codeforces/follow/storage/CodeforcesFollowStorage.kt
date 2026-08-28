package com.demich.cps.platforms.codeforces.follow.storage

import com.demich.cps.profiles.userinfo.CodeforcesUserInfo
import com.demich.cps.profiles.userinfo.ProfileResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

private interface CodeforcesFollowStorage {
    //read

    fun flowOfUserBlogs(): Flow<List<CodeforcesUserBlogInfo>>

    suspend fun userBlogs(): List<CodeforcesUserBlogInfo> =
        flowOfUserBlogs().first()

    fun flowOfUserBlog(blogId: Long): Flow<CodeforcesUserBlogInfo?>

    suspend fun userBlogOrNull(blogId: Long): CodeforcesUserBlogInfo? =
        flowOfUserBlog(blogId = blogId).first()

    suspend fun userHandles(): List<String> =
        userBlogs().map { it.handle }

    // write

    suspend fun remove(blogId: Long)

    suspend fun createUserWithoutBlog(profileResult: ProfileResult<CodeforcesUserInfo>): Boolean
}