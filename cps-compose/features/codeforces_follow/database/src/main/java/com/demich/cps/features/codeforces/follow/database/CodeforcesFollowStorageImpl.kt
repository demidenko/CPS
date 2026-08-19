package com.demich.cps.features.codeforces.follow.database

/*
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
 */