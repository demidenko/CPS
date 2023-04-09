package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.accounts.userinfo.UserSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

enum class AccountManagers {
    codeforces,
    atcoder,
    codechef,
    dmoj,
    acmp,
    timus,
    clist
}

val Context.allAccountManagers: List<AccountManager<out UserInfo>>
    get() = listOf(
        CodeforcesAccountManager(this),
        AtCoderAccountManager(this),
        CodeChefAccountManager(this),
        DmojAccountManager(this),
        ACMPAccountManager(this),
        TimusAccountManager(this)
    )


abstract class AccountManager<U: UserInfo>(val context: Context, val type: AccountManagers) {

    abstract val userIdTitle: String
    abstract val urlHomePage: String

    protected abstract fun getDataStore(): AccountDataStore<U>
    fun flowOfInfo() = getDataStore().userInfo.flow
    fun flowOfInfoWithManager() = flowOfInfo().map { info -> info?.let { UserInfoWithManager(it, this) } }

    suspend fun getSavedInfo(): U? = flowOfInfo().first()

    suspend fun setSavedInfo(info: U) {
        val oldUserId = getSavedInfo()?.userId ?: ""
        getDataStore().userInfo(info)
        if (info.userId != oldUserId && this is AccountSettingsProvider) getSettings().resetRelatedItems()
    }

    suspend fun deleteSavedInfo() {
        getDataStore().userInfo(null)
    }

    open fun isValidForUserId(char: Char): Boolean = true

    protected abstract suspend fun downloadInfo(data: String): U
    suspend fun loadInfo(data: String): U {
        require(data.isNotBlank())
        return withContext(Dispatchers.IO) {
            downloadInfo(data)
        }
    }

    @Composable
    abstract fun makeOKInfoSpan(userInfo: U): AnnotatedString

    @Composable
    open fun PanelContent(userInfo: U) {}

    @Composable
    open fun ExpandedContent(
        userInfo: U,
        setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit,
        modifier: Modifier = Modifier
     ) = Box(modifier) {
        PanelContent(userInfo)
    }
}

data class UserInfoWithManager<U: UserInfo>(
    val userInfo: U,
    val manager: AccountManager<U>
) {
    val type: AccountManagers get() = manager.type
}

interface UserSuggestionsProvider {
    suspend fun loadSuggestions(str: String): List<UserSuggestion>
    fun isValidForSearch(char: Char): Boolean = true
}

