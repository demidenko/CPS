package com.demich.cps.profiles.managers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.demich.cps.platforms.api.acmp.ACMPUrls
import com.demich.cps.platforms.clients.ACMPClient
import com.demich.cps.platforms.clients.isRedirect
import com.demich.cps.platforms.utils.ACMPUtils
import com.demich.cps.profiles.SmallProfilePanelTypeArchive
import com.demich.cps.profiles.userinfo.ACMPUserInfo
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.UserSuggestion
import com.demich.cps.ui.theme.CPSColors


class ACMPProfileManager :
    ProfileManager<ACMPUserInfo>(),
    ProfileSuggestionsProvider
{
    override val platform get() = ProfilePlatform.acmp
    override val userIdTitle get() = "id"
    override val urlHomePage get() = ACMPUrls.main

    override fun isValidForUserId(char: Char): Boolean = char in '0'..'9'

    override suspend fun fetchProfile(data: String): ProfileResult<ACMPUserInfo> {
        return ACMPUtils.runCatching {
            ProfileResult(
                userInfo = extractUserInfo(
                    source = ACMPClient.getUserPage(id = data.toInt()),
                    id = data
                )
            )
        }.getOrElse {
            if (it.isRedirect) ProfileResult.NotFound(data)
            else ProfileResult.Failed(data)
        }
    }

    override suspend fun fetchSuggestions(str: String): List<UserSuggestion> {
        if (str.toIntOrNull() != null) return emptyList()
        return ACMPUtils.extractUsersSuggestions(source = ACMPClient.getUsersSearch(str))
    }

    override fun makeUserInfoSpan(userInfo: ACMPUserInfo, cpsColors: CPSColors): AnnotatedString =
        buildAnnotatedString {
            val words = userInfo.userName.split(' ')
            if (words.size < 3) append(userInfo.userName)
            else {
                words.forEachIndexed { index, word ->
                    if (index == 0) append(word)
                    else {
                        append(' ')
                        append(word[0])
                        append('.')
                    }
                }
            }
            if (userInfo.solvedTasks > 0) append(" [${userInfo.solvedTasks} / ${userInfo.rating}]")
        }

    @Composable
    override fun PanelContent(profileResult: ProfileResult<ACMPUserInfo>) {
        if (profileResult is ProfileResult.Success) {
            val userInfo = profileResult.userInfo
            SmallProfilePanelTypeArchive(
                title = userInfo.userName,
                infoArgs = listOf(
                    "solved" to userInfo.solvedTasks.toString(),
                    "rating" to userInfo.rating.toString(),
                    "rank" to userInfo.rank.toString()
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