package com.demich.cps.profiles.managers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import com.demich.cps.platforms.api.timus.TimusUrls
import com.demich.cps.platforms.clients.TimusClient
import com.demich.cps.platforms.utils.TimusUtils
import com.demich.cps.profiles.SmallProfilePanelTypeArchive
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.TimusUserInfo
import com.demich.cps.profiles.userinfo.UserSuggestion
import com.demich.cps.ui.theme.CPSColors


class TimusProfileManager :
    ProfileManager<TimusUserInfo>(),
    ProfileSuggestionsProvider
{
    override val platform get() = ProfilePlatform.timus
    override val userIdTitle get() = "id"
    override val urlHomePage get() = TimusUrls.main

    override fun isValidForUserId(char: Char): Boolean = char in '0'..'9'
    override fun isValidForSearch(char: Char): Boolean = true

    override suspend fun fetchProfile(data: String): ProfileResult<TimusUserInfo> =
        TimusUtils.runCatching {
            extractProfile(
                source = TimusClient.getUserPage(data.toInt()),
                handle = data
            )
        }.getOrElse {
            ProfileResult.Failed(data)
        }

    override suspend fun fetchSuggestions(str: String): List<UserSuggestion> {
        if (str.toIntOrNull() != null) return emptyList()
        return TimusUtils.extractUsersSuggestions(source = TimusClient.getSearchPage(str))
    }

    override fun makeUserInfoSpan(userInfo: TimusUserInfo, cpsColors: CPSColors): AnnotatedString =
        with(userInfo) {
            AnnotatedString("$userName $solvedTasks")
        }

    @Composable
    override fun PanelContent(profileResult: ProfileResult<TimusUserInfo>) {
        if (profileResult is ProfileResult.Success) {
            val userInfo = profileResult.userInfo
            SmallProfilePanelTypeArchive(
                title = userInfo.userName,
                infoArgs = listOf(
                    "solved" to userInfo.solvedTasks.toString(),
                    "rank" to userInfo.rankTasks.toString()
                )
            )
        } else {
            SmallProfilePanelTypeArchive(
                title = profileResult.userId,
                infoArgs = emptyList()
            )
        }
    }

    override fun profileStorage(context: Context) = simpleProfileStorage(context)

}