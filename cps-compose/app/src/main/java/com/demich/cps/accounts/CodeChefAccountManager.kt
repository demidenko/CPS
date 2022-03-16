package com.demich.cps.accounts

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.utils.CodeChefAPI
import com.demich.cps.utils.CodeChefUser
import com.demich.cps.utils.CodeChefUtils
import kotlinx.serialization.Serializable

@Serializable
data class CodeChefUserInfo(
    override val status: STATUS,
    val handle: String,
    val rating: Int = NOT_RATED
): UserInfo() {
    constructor(codeChefUser: CodeChefUser): this(
        status = STATUS.OK,
        handle = codeChefUser.username,
        rating = codeChefUser.rating
    )

    override val userId: String
        get() = handle

    override fun link() = CodeChefUtils.CodeChefURLFactory.user(handle)
}

class CodeChefAccountManager(context: Context):
    RatedAccountManager<CodeChefUserInfo>(context, manager_name),
    AccountSuggestionsProvider
{
    companion object {
        const val manager_name = "codechef"
        private val Context.account_codechef_dataStore by preferencesDataStore(manager_name)
    }

    override val urlHomePage get() = CodeChefUtils.CodeChefURLFactory.main

    override fun isValidForSearch(char: Char) = isValidForUserId(char)
    override fun isValidForUserId(char: Char) = when(char) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', in "._" -> true
        else -> false
    }

    override fun emptyInfo(): CodeChefUserInfo = CodeChefUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String, flags: Int): CodeChefUserInfo {
        try {
            //TODO need more confident way to get user
            val list = CodeChefAPI.getSuggestions(data).list
            return list.find {
                it.username == data
            }?.let {
                CodeChefUserInfo(it)
            } ?: CodeChefUserInfo(status = STATUS.NOT_FOUND, handle = data)
        } catch (e: Throwable) {
            return CodeChefUserInfo(status = STATUS.FAILED, handle = data)
        }
    }

    override suspend fun loadSuggestions(str: String): List<AccountSuggestion>? {
        try {
            val list = CodeChefAPI.getSuggestions(str).list
            return list.map {
                AccountSuggestion(
                    title = it.username,
                    info = it.rating.toString(),
                    userId = it.username
                )
            }
        } catch (e: Throwable) {
            return null
        }
    }

    override suspend fun loadRatingHistory(info: CodeChefUserInfo): List<RatingChange>? {
        TODO("not yet")
    }

    override fun getRating(userInfo: CodeChefUserInfo): Int = userInfo.rating

    override val ratingsUpperBounds = arrayOf(
        1400 to HandleColor.GRAY,
        1600 to HandleColor.GREEN,
        1800 to HandleColor.BLUE,
        2000 to HandleColor.VIOLET,
        2200 to HandleColor.YELLOW,
        2500 to HandleColor.ORANGE
    )

    override fun originalColor(handleColor: HandleColor): Color =
        when (handleColor) {
            HandleColor.GRAY -> Color(0xFF666666)
            HandleColor.GREEN -> Color(0xFF1E7D22)
            HandleColor.BLUE -> Color(0xFF3366CC)
            HandleColor.VIOLET -> Color(0xFF684273)
            HandleColor.YELLOW -> Color(255, 191, 0)
            HandleColor.ORANGE -> Color(255, 127, 0)
            HandleColor.RED -> Color(208,1,27)
            else -> throw HandleColor.UnknownHandleColorException(handleColor, this)
        }

    override val rankedHandleColorsList: Array<HandleColor>
        get() = TODO("Not yet implemented")


    @Composable
    override fun makeOKInfoSpan(userInfo: CodeChefUserInfo): AnnotatedString =
        buildAnnotatedString {
            require(userInfo.status == STATUS.OK)
            append(makeHandleSpan(userInfo.copy(
                handle = userInfo.handle
                        + " "
                        + (userInfo.rating.takeIf { it != NOT_RATED }?.toString() ?: "[not rated]")
            )))
        }

    override fun getDataStore() = accountDataStore(context.account_codechef_dataStore, emptyInfo())
}