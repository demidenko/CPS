package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.accounts.SmallAccountPanelTypeRated
import com.demich.cps.ui.RatingGraph
import com.demich.cps.ui.RatingLoadButton
import com.demich.cps.ui.rememberRatingGraphUIStates
import com.demich.cps.utils.DmojAPI
import com.demich.cps.utils.DmojRatingChange
import com.demich.cps.utils.jsonCPS
import io.ktor.client.features.*
import io.ktor.http.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable
data class DmojUserInfo(
    override val status: STATUS,
    override val handle: String,
    override val rating: Int = NOT_RATED
): RatedUserInfo() {
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
                AccountSuggestion(title = it.text, userId = it.id)
            }
        } catch (e: Throwable) {
            return null
        }
    }

    override suspend fun loadRatingHistory(info: DmojUserInfo): List<RatingChange>? {
        try {
            val s = DmojAPI.getUserPage(handle = info.handle)
            val i = s.indexOf("var rating_history = [")
            if (i == -1) return emptyList()
            val j = s.indexOf("];", i)
            val str = s.substring(s.indexOf('[', i), j+1)
            return jsonCPS.decodeFromString<List<DmojRatingChange>>(str).map {
                RatingChange(
                    rating = it.rating,
                    date = Instant.fromEpochMilliseconds(it.timestamp.toLong()),
                    title = it.label,
                    rank = it.ranking
                )
            }
        } catch (e: Throwable) {
            return null
        }
    }

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
    override fun makeOKSpan(text: String, rating: Int): AnnotatedString {
        if (rating == NOT_RATED || rating < 3000) return super.makeOKSpan(text, rating)
        return buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text[0]) }
            append(super.makeOKSpan(text.substring(startIndex = 1), rating))
        }
    }

    @Composable
    override fun BigView(
        userInfo: DmojUserInfo,
        setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit,
        modifier: Modifier
    ) {
        val ratingGraphUIStates = rememberRatingGraphUIStates()
        Box(modifier = modifier) {
            SmallAccountPanelTypeRated(userInfo)
            RatingGraph(
                ratingGraphUIStates = ratingGraphUIStates,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
        setBottomBarContent {
            if (userInfo.isRated()) {
                RatingLoadButton(ratingGraphUIStates)
            }
        }
    }

    override fun getDataStore() = accountDataStore(context.account_dmoj_dataStore, emptyInfo())
}
