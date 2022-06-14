package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.accounts.SmallRatedAccountPanel
import com.demich.cps.ui.RatingGraph
import com.demich.cps.ui.RatingLoadButton
import com.demich.cps.ui.rememberRatingGraphUIStates
import com.demich.cps.utils.DmojApi
import com.demich.cps.utils.DmojRatingChange
import com.demich.cps.utils.append
import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class DmojUserInfo(
    override val status: STATUS,
    override val handle: String,
    override val rating: Int = NOT_RATED
): RatedUserInfo() {
    override fun link() = DmojApi.urls.user(handle)
}

class DmojAccountManager(context: Context):
    RatedAccountManager<DmojUserInfo>(context, AccountManagers.dmoj),
    AccountSuggestionsProvider
{
    companion object {
        private val Context.account_dmoj_dataStore by preferencesDataStore(AccountManagers.dmoj.name)
    }

    override val urlHomePage get() = DmojApi.urls.main

    override fun isValidForUserId(char: Char): Boolean = when(char) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', in "_" -> true
        else -> false
    }

    override fun emptyInfo() = DmojUserInfo(status = STATUS.NOT_FOUND, handle = "")

    override suspend fun downloadInfo(data: String, flags: Int): DmojUserInfo {
        try {
            val res = DmojApi.getUser(handle = data)
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
            return DmojApi.getSuggestions(query = str).map {
                AccountSuggestion(title = it.text, userId = it.id)
            }
        } catch (e: Throwable) {
            return null
        }
    }

    override suspend fun loadRatingHistory(info: DmojUserInfo): List<RatingChange>
        = DmojApi.getRatingChanges(handle = info.handle)
            .map(DmojRatingChange::toRatingChange)

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
    override fun makeRatedSpan(text: String, rating: Int): AnnotatedString {
        if (rating < 3000) return super.makeRatedSpan(text, rating)
        return buildAnnotatedString {
            append(text[0].toString(), fontWeight = FontWeight.Bold)
            append(super.makeRatedSpan(text.drop(1), rating))
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
            SmallRatedAccountPanel(userInfo)
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

    override fun getDataStore() = accountDataStore(context.account_dmoj_dataStore)
}
