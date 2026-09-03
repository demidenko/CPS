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
): Map<String, FetchResult<CodeforcesUser?>> =
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
                        put(key = notFoundHandle, value = FetchResult.Success(null))
                        handles.remove(notFoundHandle)
                        continue
                    }
                    for (handle in handles) put(key = handle, value = result)
                    break
                }
                is FetchResult.Success -> {
                    val iter = result.value.iterator()
                    for (handle in handles) {
                        val user = iter.next()
                        put(key = handle, value = FetchResult.Success(user))
                    }
                    break
                }
            }
        }
    }

suspend fun CodeforcesApi.getUsersCatchingFilterFound(
    handles: Collection<String>,
    checkHistoricHandles: Boolean
): Map<String, CodeforcesUser> =
    buildMap {
        getUsersCatching(handles = handles, checkHistoricHandles = checkHistoricHandles)
            .forEach { (handle, result) ->
                if (result is FetchResult.Success) {
                    result.value?.let { put(key = handle, value = it) }
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

private fun FetchResult<CodeforcesUser?>.toProfileResult(handle: String): ProfileResult<CodeforcesUserInfo> =
    when (this) {
        is FetchResult.Failure -> ProfileResult.Failed(handle)
        is FetchResult.Success -> value.let { value ->
            if (value != null) ProfileResult(value.toUserInfo())
            else ProfileResult.NotFound(userId = handle)
        }
    }
