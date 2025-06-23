package com.demich.cps.platforms.utils.codeforces

import com.demich.cps.accounts.userinfo.CodeforcesUserInfo
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.platforms.api.codeforces.CodeforcesApi
import com.demich.cps.platforms.api.codeforces.CodeforcesApiHandleNotFoundException
import com.demich.cps.platforms.api.codeforces.models.CodeforcesUser

//TODO: non recursive O(n) version (coping map is n^2, getUsers is n^2 in total, can be improved by mitm)
suspend fun CodeforcesApi.getProfiles(
    handles: Set<String>,
    recoverHandle: Boolean
): Map<String, ProfileResult<CodeforcesUserInfo>> {
    return runCatching {
        getUsers(handles = handles, checkHistoricHandles = recoverHandle)
            .apply { check(size == handles.size) }
    }.map { users ->
        //relying to cf api return in same order
        handles.zip(users.map { ProfileResult.Success(it.toUserInfo()) }).toMap()
    }.getOrElse { e ->
        if (e is CodeforcesApiHandleNotFoundException) {
            val badHandle = e.handle
            return@getOrElse getProfiles(handles = handles - badHandle, recoverHandle = recoverHandle)
                .plus(badHandle to ProfileResult.NotFound(badHandle))
        }
        handles.associateWith { ProfileResult.Failed(it) }
    }.apply {
        check(handles.all { it in this })
    }
}

suspend fun CodeforcesApi.getProfile(handle: String, recoverHandle: Boolean): ProfileResult<CodeforcesUserInfo> {
    // shortcut for getProfiles(setOf(handle), recoverHandle).getValue(handle)
    return runCatching {
        ProfileResult.Success(
            userInfo = getUser(handle = handle, checkHistoricHandles = recoverHandle).toUserInfo()
        )
    }.getOrElse { e ->
        if (e is CodeforcesApiHandleNotFoundException && e.handle == handle) {
            ProfileResult.NotFound(handle)
        } else {
            ProfileResult.Failed(handle)
        }
    }
}

suspend fun CodeforcesApi.getProfiles(handles: Collection<String>, recoverHandle: Boolean) =
    getProfiles(handles.toSet(), recoverHandle)

fun CodeforcesUser.toUserInfo(): CodeforcesUserInfo =
    CodeforcesUserInfo(
        handle = handle,
        rating = rating,
        contribution = contribution,
        lastOnlineTime = lastOnlineTime
    )