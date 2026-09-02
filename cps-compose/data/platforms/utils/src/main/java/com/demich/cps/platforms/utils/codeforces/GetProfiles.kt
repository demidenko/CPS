package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.fetchstate.FetchResult
import com.demich.cps.fetchstate.asResult
import com.demich.cps.fetchstate.fetchResultOf
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesApiHandleNotFoundException
import com.demich.cps.platforms.api.codeforces.getUser
import com.demich.cps.platforms.api.codeforces.models.CodeforcesUser
import com.demich.cps.profiles.userinfo.CodeforcesUserInfo
import com.demich.cps.profiles.userinfo.ProfileResult

suspend fun CodeforcesApi.getUsersCatching(
    handles: Collection<String>,
    checkHistoricHandles: Boolean
): Map<String, Result<CodeforcesUser>> =
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
                    if (it is CodeforcesApiHandleNotFoundException) {
                        val badHandle = it.handle
                        put(key = badHandle, value = Result.failure(it))
                        handles.remove(badHandle)
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

suspend fun CodeforcesApi.getUserCatching(handle: String, checkHistoricHandles: Boolean): Result<CodeforcesUser> =
    fetchResultOf { getUser(handle = handle, checkHistoricHandles = checkHistoricHandles) }
        .asResult()

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
    } catch (it: CodeforcesApiHandleNotFoundException) {
        // TODO: check(it.handle == handle) ???
        null
    }
}

fun Result<CodeforcesUser>.toProfileResult(handle: String): ProfileResult<CodeforcesUserInfo> =
    fold(
        onSuccess = {
            ProfileResult(it.toUserInfo())
        },
        onFailure = {
            if (it is CodeforcesApiHandleNotFoundException && it.handle == handle) ProfileResult.NotFound(handle)
            else ProfileResult.Failed(handle)
        }
    )

fun CodeforcesUser.toUserInfo(): CodeforcesUserInfo =
    CodeforcesUserInfo(
        handle = handle,
        rating = rating,
        contribution = contribution,
        lastOnlineTime = lastOnlineTime
    )