package com.demich.cps.profiles.managers

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import com.demich.cps.profiles.userinfo.ProfileResult
import com.demich.cps.profiles.userinfo.RatedUserInfo
import com.demich.cps.profiles.userinfo.UserInfo
import com.demich.cps.profiles.userinfo.UserSuggestion
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.theme.CPSColors

@androidx.annotation.Keep //navigation agr type
enum class ProfilePlatform {
    codeforces,
    atcoder,
    codechef,
    dmoj,
    acmp,
    timus,
    clist
}

abstract class AccountManager<U: UserInfo> {
    abstract val platform: ProfilePlatform

    override fun equals(other: Any?): Boolean {
        return other is AccountManager<U> && platform == other.platform
    }

    override fun hashCode() = platform.hashCode()

    abstract val userIdTitle: String
    abstract val urlHomePage: String

    abstract fun dataStore(context: Context): ProfileDataStore<U>

    open fun isValidForUserId(char: Char): Boolean = true

    abstract suspend fun fetchProfile(data: String): ProfileResult<U>

    abstract fun makeUserInfoSpan(userInfo: U, cpsColors: CPSColors): AnnotatedString

    @Composable
    open fun PanelContent(profileResult: ProfileResult<U>) {}

    @Composable
    open fun ExpandedContent(
        profileResult: ProfileResult<U>,
        setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit,
        modifier: Modifier
    ) {
        Box(modifier) {
            PanelContent(profileResult)
        }
    }

    companion object {
        fun entries(): List<AccountManager<out UserInfo>> =
            listOf(
                CodeforcesAccountManager(),
                AtCoderAccountManager(),
                CodeChefAccountManager(),
                DmojAccountManager(),
                ACMPAccountManager(),
                TimusAccountManager()
            )

        fun ratedEntries(): List<RatedAccountManager<out RatedUserInfo>> =
            entries().filterIsInstance<RatedAccountManager<*>>()
    }
}

fun accountManagerOf(platform: ProfilePlatform) =
    AccountManager.entries().first { it.platform == platform }

interface ProfileSuggestionsProvider {
    suspend fun fetchSuggestions(str: String): List<UserSuggestion>

    fun isValidForSearch(char: Char): Boolean = true
}

