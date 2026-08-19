package com.demich.cps.platforms.codeforces.follow.storage

interface CodeforcesFollowStorage {
    suspend fun remove(blogId: Long)

}