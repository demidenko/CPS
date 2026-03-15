package com.demich.cps.profiles.managers

import android.content.Context
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.UserInfo
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class ProfileResultWithManager<U: UserInfo>(
    val profileResult: ProfileResult<U>,
    val manager: ProfileManager<U>
) {
}

val ProfileResultWithManager<*>.platform: ProfilePlatform
    get() = manager.platform

fun <U: UserInfo> ProfileManager<U>.flowWithProfileResult(context: Context) =
    dataStore(context).profile.asFlow().map { result ->
        result?.let { ProfileResultWithManager(it, this) }
    }

fun Collection<ProfileManager<*>>.flowOfExisted(context: Context) =
    combine(flows = map { it.flowWithProfileResult(context) }) { it.filterNotNull() }