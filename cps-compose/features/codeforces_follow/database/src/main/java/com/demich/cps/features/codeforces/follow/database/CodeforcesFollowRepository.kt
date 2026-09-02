package com.demich.cps.features.codeforces.follow.database

import android.content.Context
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesApiHandleNotFoundException
import com.demich.cps.platforms.api.codeforces.getUserBlogEntriesRecovered
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.api.codeforces.models.CodeforcesLocale
import com.demich.cps.platforms.codeforces.follow.storage.CodeforcesUserBlogInfo
import com.demich.cps.platforms.codeforces.follow.storage.handle
import com.demich.cps.platforms.utils.codeforces.getProfile
import com.demich.cps.platforms.utils.codeforces.getProfiles
import com.demich.cps.platforms.utils.codeforces.getUserCatching
import com.demich.cps.platforms.utils.codeforces.toProfileResult
import com.demich.cps.profiles.userinfo.CodeforcesUserInfo
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.handle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

abstract class CodeforcesFollowRepository(
    context: Context
) {
    private val dao: CodeforcesFollowDao =
        context.followDataBase.followListDao()

    suspend fun remove(blogId: Long) = dao.remove(blogId = blogId)

    // TODO: without toCodeforcesUserBlog?

    fun flowOfUserBlogInfo(blogId: Long): Flow<CodeforcesUserBlogInfo?> =
        dao.flowOfShortEntity(id = blogId).map { it?.toCodeforcesUserBlog() }

    suspend fun blogOrNull(blogId: Long): CodeforcesUserBlogInfo? =
        dao.getShortEntity(blogId)?.toCodeforcesUserBlog()

    fun flowOfUserBlogs(): Flow<List<CodeforcesUserBlogInfo>> =
        dao.flowOfShortBlogs().map { it.map { it.toCodeforcesUserBlog() } }

    suspend fun blogs(): List<CodeforcesUserBlogInfo> =
        dao.getShortBlogs().map { it.toCodeforcesUserBlog() }

    suspend fun getAndReloadBlogEntries(handle: String) =
        getAndReloadBlogEntries(handle = handle, locale = getLocale())

    private suspend fun getAndReloadBlogEntries(
        handle: String,
        locale: CodeforcesLocale
    ): Result<List<CodeforcesBlogEntry>> {
        var handle = handle
        val result = getApi(locale).getBlogEntries(
            handle = handle,
            onChangeProfile = {
                dao.updateUserProfile(handle = handle, result = it)
                handle = it.handle
            }
        )
        result.onSuccess { blogEntries ->
            dao.updateBlogEntries(
                handle = handle,
                blogEntries = blogEntries,
                onNewBlogEntries = { it.forEach(::notifyNewBlogEntry) }
            )
        }
        return result
    }

    suspend fun addNewUser(result: ProfileResult<CodeforcesUserInfo>) {
        if (!dao.createUserWithoutBlog(profileResult = result)) return

        val handle = result.handle
        getAndReloadBlogEntries(handle = handle)

        // TODO: parallel?
        // TODO: reload can change handle
        if (result is ProfileResult.Failed) {
            dao.updateUserProfile(
                handle = handle,
                result = getApi(EN).getProfile(handle = handle, checkHistoricHandles = true)
            )
        }
    }

    @IgnorableReturnValue
    suspend fun updateProfiles() =
        getApi(EN).getProfiles(handles = dao.usersHandles(), checkHistoricHandles = true)
            .also { dao.updateUserProfiles(it) }

    protected abstract suspend fun getLocale(): CodeforcesLocale

    protected abstract fun getApi(locale: CodeforcesLocale): CodeforcesApi

    protected abstract fun notifyNewBlogEntry(blogEntry: CodeforcesBlogEntry)
}

suspend fun CodeforcesFollowRepository.blog(blogId: Long) =
    checkNotNull(blogOrNull(blogId)) { "blogId $blogId not in repository" }

suspend fun CodeforcesFollowRepository.updateFailedBlogEntries() {
    blogs().forEach {
        if (it.blogSize == null) getAndReloadBlogEntries(handle = it.handle)
    }
}

suspend fun CodeforcesFollowRepository.addNewUser(handle: String) =
    addNewUser(result = ProfileResult.Failed(handle))

private suspend inline fun CodeforcesApi.getBlogEntries(
    handle: String,
    onChangeProfile: (ProfileResult<CodeforcesUserInfo>) -> Unit
): Result<List<CodeforcesBlogEntry>> {
    runCatching {
        getUserBlogEntriesRecovered(handle = handle)
    }.onFailure {
        if (it is CodeforcesApiHandleNotFoundException && it.handle == handle) {
            val result = getUserCatching(handle = handle, checkHistoricHandles = true)
            val profile = result.toProfileResult(handle)
            onChangeProfile(profile)
            return result.mapCatching { getUserBlogEntriesRecovered(handle = profile.handle) }
        }
    }.also {
        return it
    }
}
