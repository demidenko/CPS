package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.utils.DmojAPI
import io.ktor.client.features.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class DmojUserInfo(
    override val status: STATUS,
    val handle: String,
    val rating: Int = NOT_RATED
): UserInfo() {
    override val userId: String
        get() = handle

    override fun link() = DmojAPI.URLFactory.user(handle)
}

class DmojAccountManager(context: Context):
    RatedAccountManager<DmojUserInfo>(context, AccountManagers.dmoj),
    AccountSuggestionsProvider
{
    companion object {
        private val Context.account_dmoj_dataStore by preferencesDataStore(AccountManagers.dmoj.name)
    }

    override val urlHomePage get() = DmojAPI.URLFactory.main

    override fun isValidForUserId(char: Char): Boolean = when(char) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', in "_" -> true
        else -> false
    }

    override fun emptyInfo() = DmojUserInfo(status = STATUS.NOT_FOUND, handle = "")

    override suspend fun downloadInfo(data: String, flags: Int): DmojUserInfo {
        try {
            val res = DmojAPI.getUser(handle = data)
            return DmojUserInfo(
                status = STATUS.OK,
                handle = res.username,
                rating = res.rating ?: NOT_RATED
            )
        } catch (e: Throwable) {
            if (e is ClientRequestException) {
                if (e.response.status == HttpStatusCode.NotFound) {
                    return DmojUserInfo(status = STATUS.NOT_FOUND, handle = data)
                }
            }
            return DmojUserInfo(status = STATUS.FAILED, handle = data)
        }
    }

    override suspend fun loadSuggestions(str: String): List<AccountSuggestion>? {
        try {
            return DmojAPI.getSuggestions(query = str).map {
                AccountSuggestion(
                    title = it.text,
                    userId = it.id,
                    info = ""
                )
            }
        } catch (e: Throwable) {
            return null
        }
    }

    override suspend fun loadRatingHistory(info: DmojUserInfo): List<RatingChange>? {
        TODO("not yet")
    }

    override fun getRating(userInfo: DmojUserInfo) = userInfo.rating

    override val ratingsUpperBounds = arrayOf(
        HandleColor.GRAY to 1000,
        HandleColor.GREEN to 1300,
        HandleColor.BLUE to 1600,
        HandleColor.VIOLET to 1900,
        HandleColor.YELLOW to 2400
    )

    override val rankedHandleColorsList = HandleColor.rankedDmoj

    override fun originalColor(handleColor: HandleColor) =
        when (handleColor) {
            HandleColor.GRAY -> Color(0xFF999999)
            HandleColor.GREEN -> Color(0xff00a900)
            HandleColor.BLUE -> Color(0xFF0000FF)
            HandleColor.VIOLET -> Color(0xFF800080)
            HandleColor.YELLOW -> Color(0xFFFFB100)
            HandleColor.RED -> Color(0xFFEE0000)
            else -> throw HandleColor.UnknownHandleColorException(handleColor, this)
        }

    @Composable
    override fun makeOKInfoSpan(userInfo: DmojUserInfo): AnnotatedString =
        buildAnnotatedString {
            //TODO: first letter from 3000
            require(userInfo.status == STATUS.OK)
            withStyle(SpanStyle(
                color = colorFor(userInfo),
                fontWeight = if (userInfo.rating != NOT_RATED) FontWeight.Bold else null
            )) {
                append(userInfo.handle)
                append(' ')
                if (userInfo.rating != NOT_RATED) append(userInfo.rating.toString())
                else append("[not rated]")
            }
        }

    override fun getDataStore() = accountDataStore(context.account_dmoj_dataStore, emptyInfo())
}
