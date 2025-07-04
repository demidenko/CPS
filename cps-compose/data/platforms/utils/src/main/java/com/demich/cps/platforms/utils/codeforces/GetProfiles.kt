package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.accounts.userinfo.CodeforcesUserInfo
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesApiHandleNotFoundException
import com.demich.cps.platforms.api.codeforces.models.CodeforcesUser
import kotlinx.datetime.toDeprecatedInstant

suspend fun CodeforcesApi.getProfiles(
    handles: Collection<String>,
    recoverHandle: Boolean
): Map<String, ProfileResult<CodeforcesUserInfo>> =
    buildMap {
        val handles = handles.toMutableSet()
        while (handles.isNotEmpty()) {
            runCatching {
                getUsers(handles = handles, checkHistoricHandles = recoverHandle)
                    .also { check(it.size == handles.size) }
            }.onFailure {
                if (it is CodeforcesApiHandleNotFoundException) {
                    val badHandle = it.handle
                    put(key = badHandle, value = ProfileResult.NotFound(badHandle))
                    handles.remove(badHandle)
                    continue
                }
                for (handle in handles) put(key = handle, value = ProfileResult.Failed(handle))
                break
            }.onSuccess { users ->
                val iter = users.iterator()
                for (handle in handles) {
                    val user = iter.next()
                    put(key = handle, value = ProfileResult.Success(user.toUserInfo()))
                }
                break
            }
        }
    }.apply {
        check(handles.all { it in this })
    }

suspend fun CodeforcesApi.getProfile(handle: String, recoverHandle: Boolean): ProfileResult<CodeforcesUserInfo> {
    // shortcut for getProfiles(setOf(handle), recoverHandle).getValue(handle)
    return runCatching {
        val user = getUser(handle = handle, checkHistoricHandles = recoverHandle)
        ProfileResult.Success(user.toUserInfo())
    }.getOrElse { e ->
        if (e is CodeforcesApiHandleNotFoundException && e.handle == handle) {
            ProfileResult.NotFound(handle)
        } else {
            ProfileResult.Failed(handle)
        }
    }
}

fun CodeforcesUser.toUserInfo(): CodeforcesUserInfo =
    CodeforcesUserInfo(
        handle = handle,
        rating = rating,
        contribution = contribution,
        lastOnlineTime = lastOnlineTime.toDeprecatedInstant()
    )