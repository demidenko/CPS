package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.fetchstate.FetchResult
import com.demich.cps.fetchstate.fetchResultOf
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesApiUserNotFoundException
import com.demich.cps.platforms.api.codeforces.getUser
import com.demich.cps.platforms.api.codeforces.models.CodeforcesUser
import com.demich.cps.profiles.userinfo.CodeforcesUserInfo
import com.demich.cps.profiles.userinfo.ProfileResult

suspend fun CodeforcesApi.getUsersCatching(
    handles: Collection<String>,
    checkHistoricHandles: Boolean
): Map<String, Result<CodeforcesUser?>> =
    buildMap {
        val handles = handles.toMutableSet()
        while (handles.isNotEmpty()) {
            val result = fetchResultOf {
                getUsers(handles = handles, checkHistoricHandles = checkHistoricHandles)
                    .also { check(it.size == handles.size) }
            }
            when (result) {
                is FetchResult.Failure -> {
                    val it = result.exception
                    if (it is CodeforcesApiUserNotFoundException) {
                        val notFoundHandle = it.handle
                        put(key = notFoundHandle, value = Result.success(null))
                        handles.remove(notFoundHandle)
                        continue
                    }
                    for (handle in handles) put(key = handle, value = Result.failure(it))
                    break
                }
                is FetchResult.Success -> {
                    val iter = result.value.iterator()
                    for (handle in handles) {
                        val user = iter.next()
                        put(key = handle, value = Result.success(user))
                    }
                    break
                }
            }
        }
    }

suspend fun CodeforcesApi.getProfiles(
    handles: Collection<String>,
    checkHistoricHandles: Boolean
): Map<String, ProfileResult<CodeforcesUserInfo>> =
    getUsersCatching(handles = handles, checkHistoricHandles = checkHistoricHandles)
        .mapValues { it.value.toProfileResult(handle = it.key) }

suspend fun CodeforcesApi.getProfile(handle: String, checkHistoricHandles: Boolean): ProfileResult<CodeforcesUserInfo> {
    val result = fetchResultOf { getUserOrNull(handle = handle, checkHistoricHandles = checkHistoricHandles) }
    return when (result) {
        is FetchResult.Failure -> ProfileResult.Failed(handle)
        is FetchResult.Success -> {
            val user = result.value
            if (user == null) ProfileResult.NotFound(handle)
            else ProfileResult(user.toUserInfo())
        }
    }
}

// returns null if user not found
suspend fun CodeforcesApi.getUserOrNull(
    handle: String,
    checkHistoricHandles: Boolean
): CodeforcesUser? {
    return try {
        getUser(handle = handle, checkHistoricHandles = checkHistoricHandles)
    } catch (it: CodeforcesApiUserNotFoundException) {
        // TODO: check(it.handle == handle) ???
        null
    }
}

private fun Result<CodeforcesUser?>.toProfileResult(handle: String): ProfileResult<CodeforcesUserInfo> =
    fold(
        onSuccess = {
            if (it != null) ProfileResult(it.toUserInfo())
            else ProfileResult.NotFound(handle)
        },
        onFailure = {
            ProfileResult.Failed(handle)
        }
    )
