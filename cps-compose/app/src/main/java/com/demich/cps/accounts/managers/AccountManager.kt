package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.accounts.userinfo.UserSuggestion
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.theme.CPSColors
import kotlinx.coroutines.flow.map

@androidx.annotation.Keep //navigation agr type
enum class AccountManagerType {
    codeforces,
    atcoder,
    codechef,
    dmoj,
    acmp,
    timus,
    clist
}

val allAccountManagers: List<AccountManager<out UserInfo>>
    get() = listOf(
        CodeforcesAccountManager(),
        AtCoderAccountManager(),
        CodeChefAccountManager(),
        DmojAccountManager(),
        ACMPAccountManager(),
        TimusAccountManager()
    )

val allRatedAccountManagers: List<RatedAccountManager<out RatedUserInfo>>
    get() = allAccountManagers.filterIsInstance<RatedAccountManager<*>>()

fun accountManagerOf(type: AccountManagerType) =
    allAccountManagers.first { it.type == type }

abstract class AccountManager<U: UserInfo>(val type: AccountManagerType) {

    abstract val userIdTitle: String
    abstract val urlHomePage: String

    abstract fun dataStore(context: Context): AccountDataStore<U>

    fun flowOfInfoWithManager(context: Context) =
        dataStore(context).flowOfInfo().map { info ->
            info?.let { UserInfoWithManager(it, this) }
        }

    open fun isValidForUserId(char: Char): Boolean = true

    abstract suspend fun getUserInfo(data: String): U

    abstract fun makeOKInfoSpan(userInfo: U, cpsColors: CPSColors): AnnotatedString

    @Composable
    open fun PanelContent(userInfo: U) {}

    @Composable
    open fun ExpandedContent(
        userInfo: U,
        setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit,
        modifier: Modifier
     ) = Box(modifier) {
        PanelContent(userInfo)
    }
}

data class UserInfoWithManager<U: UserInfo>(
    val userInfo: U,
    val manager: AccountManager<U>
) {
    val type: AccountManagerType get() = manager.type
}

interface UserSuggestionsProvider {
    suspend fun getSuggestions(str: String): List<UserSuggestion>
    fun isValidForSearch(char: Char): Boolean = true
}

