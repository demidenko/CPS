package com.demich.cps.features.codeforces.follow.database

import android.content.Context
import com.demich.cps.platforms.codeforces.follow.storage.CodeforcesFollowStorage
import com.demich.cps.platforms.codeforces.follow.storage.CodeforcesUserBlog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CodeforcesFollowStorageImpl(context: Context): CodeforcesFollowStorage {
    private val dao: CodeforcesFollowDao =
        context.followDataBase.followListDao()

    override suspend fun blogs(): List<CodeforcesUserBlog> =
        dao.getShortBlogs().map { it.toCodeforcesUserBlog() }

    override fun flowOfUserBlogs(): Flow<List<CodeforcesUserBlog>> =
        dao.flowOfShortBlogs().map { it.map { it.toCodeforcesUserBlog() } }

    override suspend fun blogOrNull(blogId: Long): CodeforcesUserBlog? =
        dao.getShortEntity(blogId)?.toCodeforcesUserBlog()

    override fun flowOfUserBlog(blogId: Long): Flow<CodeforcesUserBlog?> =
        dao.flowOfShortEntity(id = blogId).map { it?.toCodeforcesUserBlog() }

    override suspend fun remove(blogId: Long) {
        dao.remove(blogId = blogId)
    }
}