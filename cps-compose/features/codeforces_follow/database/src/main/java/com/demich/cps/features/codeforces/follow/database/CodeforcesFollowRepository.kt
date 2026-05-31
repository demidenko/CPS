package com.demich.cps.features.codeforces.follow.database

import android.content.Context
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesApiBlogReadNotAllowedException
import com.demich.cps.platforms.api.codeforces.CodeforcesApiHandleNotFoundException
import com.demich.cps.platforms.api.codeforces.models.CodeforcesBlogEntry
import com.demich.cps.platforms.api.codeforces.models.CodeforcesLocale
import com.demich.cps.platforms.utils.codeforces.getProfile
import com.demich.cps.platforms.utils.codeforces.getProfiles
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

    suspend fun remove(handle: String) = dao.remove(handle)

    fun flowOfUserBlogs(): Flow<List<CodeforcesUserBlog>> =
        dao.flowOfShortBlogs().map { it.map { it.toCodeforcesUserBlog() } }

    // TODO: without toCodeforcesUserBlog?
    suspend fun blogs(): List<CodeforcesUserBlog> =
        dao.getShortBlogs().map { it.toCodeforcesUserBlog() }

    suspend fun getAndReloadBlogEntries(handle: String) =
        getAndReloadBlogEntries(handle = handle, locale = getLocale())

    private suspend fun getAndReloadBlogEntries(
        handle: String,
        locale: CodeforcesLocale
    ): Result<List<CodeforcesBlogEntry>> {
        val (newProfile, result) = getApi(locale).getBlogEntries(handle = handle)
        if (newProfile != null) dao.applyProfileResult(handle = handle, result = newProfile)
        result.onSuccess { blogEntries ->
            dao.updateBlogEntries(
                handle = newProfile?.handle ?: handle,
                blogEntries = blogEntries,
                onNewBlogEntry = ::notifyNewBlogEntry
            )
        }
        return result
    }

    suspend fun addNewUser(result: ProfileResult<CodeforcesUserInfo>): Boolean {
        if (dao.createUserWithoutBlog(profileResult = result)) {
            getAndReloadBlogEntries(handle = result.handle)
            return true
        } else {
            return false
        }
    }

    suspend fun addNewUser(handle: String) {
        //TODO: sync?? parallel? (addNewUser loads blog without info)
        if (addNewUser(ProfileResult.Failed(handle))) {
            dao.applyProfileResult(
                handle = handle,
                result = getApi(EN).getProfile(handle = handle, checkHistoricHandles = true)
            )
        }
    }

    suspend fun updateUsers() =
        getApi(EN).getProfiles(handles = dao.getHandles(), checkHistoricHandles = true)
            .also { dao.applyProfilesResults(it) }

    protected abstract suspend fun getLocale(): CodeforcesLocale

    protected abstract fun getApi(locale: CodeforcesLocale): CodeforcesApi

    protected abstract fun notifyNewBlogEntry(blogEntry: CodeforcesBlogEntry)
}

suspend fun CodeforcesFollowRepository.updateFailedBlogEntries() {
    blogs().forEach {
        if (it.blogSize == null) getAndReloadBlogEntries(handle = it.handle)
    }
}

private data class GetBlogEntriesResult(
    val newProfile: ProfileResult<CodeforcesUserInfo>?,
    val blogEntries: Result<List<CodeforcesBlogEntry>>
)

private suspend fun CodeforcesApi.getBlogEntries(
    handle: String
): GetBlogEntriesResult {
    runCatching {
        getUserBlogEntries(handle = handle)
    }.recoverCatching {
        when (it) {
            is CodeforcesApiBlogReadNotAllowedException -> emptyList()
            is CodeforcesApiHandleNotFoundException if it.handle == handle -> {
                val profile = getProfile(handle = handle, checkHistoricHandles = true)
                return GetBlogEntriesResult(
                    newProfile = profile,
                    blogEntries = when (profile) {
                        is ProfileResult.Success -> runCatching { getUserBlogEntries(handle = profile.handle) }
                        else -> Result.failure(it)
                    }
                )
            }
            else -> throw it
        }
    }.also {
        return GetBlogEntriesResult(newProfile = null, blogEntries = it)
    }
}