package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import com.demich.cps.*
import kotlinx.coroutines.Dispatchers
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
    fun flowOfInfoWithManager() = getDataStore().userInfo.flow.map { info -> UserInfoWithManager(info, this) }

    abstract fun emptyInfo(): U

    protected abstract suspend fun downloadInfo(data: String): U
    suspend fun loadInfo(data: String): U {
        if (data.isBlank()) return emptyInfo()
        return withContext(Dispatchers.IO) {
            downloadInfo(data)
        }
    }

    open fun isValidForUserId(char: Char): Boolean = true

    suspend fun getSavedInfo(): U = getDataStore().userInfo()

    suspend fun setSavedInfo(info: U) {
        val old = getSavedInfo()
        getDataStore().userInfo(info)
        if (info.userId != old.userId && this is AccountSettingsProvider) getSettings().resetRelatedItems()
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

enum class STATUS {
    OK,
    NOT_FOUND,
    FAILED
}

abstract class UserInfo {
    abstract val userId: String
    abstract val status: STATUS

    abstract val userPageUrl: String

    fun isEmpty() = userId.isBlank()
}

data class UserInfoWithManager<U: UserInfo>(
    val userInfo: U,
    val manager: AccountManager<U>
) {
    val type: AccountManagers get() = manager.type
}

data class AccountSuggestion(
    val userId: String,
    val title: String = userId,
    val info: String = ""
)

interface AccountSuggestionsProvider {
    suspend fun loadSuggestions(str: String): List<AccountSuggestion>
    fun isValidForSearch(char: Char): Boolean = true
}

