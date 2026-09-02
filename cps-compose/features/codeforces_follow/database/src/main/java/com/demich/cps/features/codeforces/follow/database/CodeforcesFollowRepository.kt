package com.demich.cps.features.codeforces.follow.database

import android.content.Context
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesApiUserNotFoundException
import com.demich.cps.platforms.api.codeforces.getUser
import com.demich.cps.platforms.api.codeforces.getUserBlogEntriesRecovered
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.api.codeforces.models.CodeforcesLocale
import com.demich.cps.platforms.codeforces.follow.storage.CodeforcesUserBlogInfo
import com.demich.cps.platforms.codeforces.follow.storage.handle
import com.demich.cps.platforms.utils.codeforces.getProfile
import com.demich.cps.platforms.utils.codeforces.getProfiles
import com.demich.cps.platforms.utils.codeforces.toUserInfo
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

    // TODO
    suspend fun getAndReloadBlogEntries(handle: String) = runCatching {
        getAndUpdateBlogEntries(handle = handle, locale = getLocale())
    }

    private suspend fun getAndUpdateBlogEntries(
        handle: String,
        locale: CodeforcesLocale
    ): List<CodeforcesBlogEntry> {
        var handle = handle
        val blogEntries = try {
            getApi(locale).getBlogEntriesRecoverHotFound(
                handle = handle,
                onChangeUserInfo = {
                    dao.updateUserProfile(handle = handle, result = ProfileResult(it))
                    handle = it.handle
                }
            )
        } catch (it: CodeforcesApiUserNotFoundException) {
            dao.updateUserProfile(handle = handle, result = ProfileResult.NotFound(handle))
            throw it
        }

        dao.updateBlogEntries(
            handle = handle,
            blogEntries = blogEntries,
            onNewBlogEntries = { it.forEach(::notifyNewBlogEntry) }
        )

        return blogEntries
    }

    suspend fun addNewUser(result: ProfileResult<CodeforcesUserInfo>) {
        if (!dao.createUserWithoutBlog(profileResult = result)) return

        val result = when (result) {
            is ProfileResult.Success -> result
            is ProfileResult.Failed -> getApi(EN)
                .getProfile(handle = result.handle, checkHistoricHandles = true)
                .also {
                    dao.updateUserProfile(
                        handle = result.handle,
                        result = it
                    )
                }
        }

        getAndReloadBlogEntries(handle = result.handle)
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

private suspend inline fun CodeforcesApi.getBlogEntriesRecoverHotFound(
    handle: String,
    onChangeUserInfo: (CodeforcesUserInfo) -> Unit
): List<CodeforcesBlogEntry> {
    try {
        return getUserBlogEntriesRecovered(handle = handle)
    } catch (it: CodeforcesApiUserNotFoundException) {
        val user = getUser(handle = handle, checkHistoricHandles = true)
        onChangeUserInfo(user.toUserInfo())
        return getUserBlogEntriesRecovered(handle = user.handle) // TODO: not perfect (wrap to while?)
    }
}