package com.demich.cps.platforms.codeforces.follow.storage

import kotlinx.coroutines.flow.Flow

interface CodeforcesFollowStorage {

    suspend fun blogs(): List<CodeforcesUserBlog>

    fun flowOfUserBlogs(): Flow<List<CodeforcesUserBlog>>

    suspend fun blogOrNull(blogId: Long): CodeforcesUserBlog?

    fun flowOfUserBlog(blogId: Long): Flow<CodeforcesUserBlog?>

    suspend fun remove(blogId: Long)
}