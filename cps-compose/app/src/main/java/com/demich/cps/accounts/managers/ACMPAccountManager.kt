package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.demich.cps.accounts.SmallAccountPanelTypeArchive
import com.demich.cps.accounts.userinfo.ACMPUserInfo
import com.demich.cps.accounts.userinfo.STATUS
import com.demich.cps.accounts.userinfo.UserSuggestion
import com.demich.cps.platforms.api.ACMPApi
import com.demich.cps.platforms.utils.ACMPUtils
import com.demich.cps.ui.theme.CPSColors


class ACMPAccountManager :
    AccountManager<ACMPUserInfo>(AccountManagerType.acmp),
    UserSuggestionsProvider
{
    override val userIdTitle get() = "id"
    override val urlHomePage get() = ACMPApi.urls.main

    override fun isValidForUserId(char: Char): Boolean = char in '0'..'9'

    override suspend fun getUserInfo(data: String): ACMPUserInfo {
        return ACMPUtils.runCatching {
            extractUserInfo(
                source = ACMPApi.getUserPage(id = data.toInt()),
                id = data
            )
        }.getOrElse { e ->
            when (e) {
                is ACMPApi.ACMPPageNotFoundException -> ACMPUserInfo(status = STATUS.NOT_FOUND, id = data)
                else -> ACMPUserInfo(status = STATUS.FAILED, id = data)
            }
        }
    }

    override suspend fun getSuggestions(str: String): List<UserSuggestion> {
        if (str.toIntOrNull() != null) return emptyList()
        return ACMPUtils.extractUsersSuggestions(source = ACMPApi.getUsersSearch(str))
    }

    override fun makeOKInfoSpan(userInfo: ACMPUserInfo, cpsColors: CPSColors): AnnotatedString =
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
    override fun PanelContent(userInfo: ACMPUserInfo) {
        if (userInfo.status == STATUS.OK) {
            SmallAccountPanelTypeArchive(
                title = userInfo.userName,
                infoArgs = listOf(
                    "solved" to userInfo.solvedTasks.toString(),
                    "rating" to userInfo.rating.toString(),
                    "rank" to userInfo.rank.toString()
                )
            )
        } else {
            SmallAccountPanelTypeArchive(
                title = userInfo.id,
                infoArgs = emptyList()
            )
        }
    }

    override fun dataStore(context: Context) = simpleAccountDataStore(context)

}