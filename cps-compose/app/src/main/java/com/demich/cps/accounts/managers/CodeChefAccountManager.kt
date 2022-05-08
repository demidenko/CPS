package com.demich.cps.accounts.managers

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.preferencesDataStore
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.accounts.SmallRatedAccountPanel
import com.demich.cps.ui.RatingGraph
import com.demich.cps.ui.RatingLoadButton
import com.demich.cps.ui.rememberRatingGraphUIStates
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.CodeChefApi
import com.demich.cps.utils.CodeChefRatingChange
import com.demich.cps.utils.jsonCPS
import io.ktor.client.features.*
import io.ktor.http.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import org.jsoup.Jsoup

@Serializable
data class CodeChefUserInfo(
    override val status: STATUS,
    override val handle: String,
    override val rating: Int = NOT_RATED
): RatedUserInfo() {
    override fun link() = CodeChefApi.urls.user(handle)
}

class CodeChefAccountManager(context: Context):
    RatedAccountManager<CodeChefUserInfo>(context, AccountManagers.codechef),
    AccountSuggestionsProvider
{
    companion object {
        private val Context.account_codechef_dataStore by preferencesDataStore(AccountManagers.codechef.name)

        private const val star = "★"
    }

    override val urlHomePage get() = CodeChefApi.urls.main

    override fun isValidForSearch(char: Char) = isValidForUserId(char)
    override fun isValidForUserId(char: Char) = when(char) {
        in 'a'..'z', in 'A'..'Z', in '0'..'9', in "._" -> true
        else -> false
    }

    override fun emptyInfo(): CodeChefUserInfo = CodeChefUserInfo(STATUS.NOT_FOUND, "")

    override suspend fun downloadInfo(data: String, flags: Int): CodeChefUserInfo {
        try {
            Jsoup.parse(CodeChefApi.getUserPage(handle = data)).run {
                val rating = selectFirst("div.rating-ranks")
                    ?.select("a")
                    ?.takeIf { !it.all { it.text() == "Inactive" } }
                    ?.let { selectFirst("div.rating-header > div.rating-number")?.text()?.toInt() }
                    ?: NOT_RATED
                val userName = selectFirst("section.user-details")?.selectFirst("span.m-username--link")
                return CodeChefUserInfo(
                    status = STATUS.OK,
                    handle = userName?.text() ?: data,
                    rating = rating
                )
            }
        } catch (e: Throwable) {
            if (e is RedirectResponseException && e.response.status == HttpStatusCode.fromValue(302)) {
                return CodeChefUserInfo(status = STATUS.NOT_FOUND, handle = data)
            }
            return CodeChefUserInfo(status = STATUS.FAILED, handle = data)
        }
    }

    override suspend fun loadSuggestions(str: String): List<AccountSuggestion>? {
        try {
            return CodeChefApi.getSuggestions(str).list.map {
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
        try {
            val s = CodeChefApi.getUserPage(handle = info.handle)
            val i = s.indexOf("var all_rating = ")
            if (i == -1) return emptyList()
            val ar = s.substring(s.indexOf("[", i), s.indexOf("];", i) + 1)
            return jsonCPS.decodeFromString<List<CodeChefRatingChange>>(ar).map {
                RatingChange(
                    rating = it.rating.toInt(),
                    rank = it.rank.toInt(),
                    title = it.name,
                    date = Instant.parse(it.end_date.split(' ').run { "${get(0)}T${get(1)}Z" })
                )
            }
        } catch (e: Throwable) {
            return null
        }
    }

    override val ratingsUpperBounds = arrayOf(
        HandleColor.GRAY to 1400,
        HandleColor.GREEN to 1600,
        HandleColor.BLUE to 1800,
        HandleColor.VIOLET to 2000,
        HandleColor.YELLOW to 2200,
        HandleColor.ORANGE to 2500
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

    override val rankedHandleColorsList = HandleColor.rankedCodeChef

    private fun getRatingStarNumber(rating: Int): Int {
        val index = ratingsUpperBounds.indexOfFirst { rating < it.second }
        return if (index == -1) ratingsUpperBounds.size + 1 else index + 1
    }

    @Composable
    override fun makeRatedSpan(text: String, rating: Int) = buildAnnotatedString {
        append(AnnotatedString(
            text = "${getRatingStarNumber(rating)}$star ",
            spanStyle = SpanStyle(color = colorFor(rating = rating))
        ))
        append(text)
    }

    @Composable
    private fun StarBox(
        rating: Int,
        textColor: Color,
        fontSize: TextUnit = 20.sp,
        modifier: Modifier = Modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .background(color = colorFor(rating = rating))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    append(getRatingStarNumber(rating).toString())
                    appendInlineContent(star)
                },
                color = textColor,
                fontSize = fontSize,
                inlineContent = mapOf(
                    star to InlineTextContent(
                        Placeholder(
                            width = fontSize,
                            height = fontSize,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                )
            )

        }
    }

    @Composable
    override fun Panel(userInfo: CodeChefUserInfo) {
        SmallRatedAccountPanel(
            userInfo = userInfo,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (userInfo.isRated()) {
                        StarBox(
                            rating = userInfo.rating,
                            textColor = cpsColors.background,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Text(
                        text = userInfo.handle,
                        fontSize = 30.sp
                    )
                }
            },
            additionalTitle = {
                if (userInfo.status == STATUS.OK) {
                    Text(
                        text = userInfo.ratingToString(),
                        fontSize = 25.sp
                    )
                }
            }
        )
    }

    @Composable
    override fun BigView(
        userInfo: CodeChefUserInfo,
        setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit,
        modifier: Modifier
    ) {
        val ratingGraphUIStates = rememberRatingGraphUIStates()
        Box(modifier = modifier) {
            Panel(userInfo)
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

    override fun getDataStore() = accountDataStore(context.account_codechef_dataStore)
}