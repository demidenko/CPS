package com.demich.cps.utils.codeforces

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.demich.cps.accounts.managers.HandleColor
import com.demich.cps.accounts.managers.NOT_RATED
import com.demich.cps.accounts.managers.STATUS
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.signedToString
import org.jsoup.Jsoup
import java.util.*

object CodeforcesUtils {

    enum class ColorTag {
        BLACK,
        GRAY,
        GREEN,
        CYAN,
        BLUE,
        VIOLET,
        ORANGE,
        RED,
        LEGENDARY,
        ADMIN;

        companion object {
            fun fromString(str: String): ColorTag =
                valueOf(str.removePrefix("user-").uppercase(Locale.ENGLISH))
        }
    }

    fun getTagByRating(rating: Int): ColorTag {
        return when {
            rating == NOT_RATED -> ColorTag.BLACK
            rating < 1200 -> ColorTag.GRAY
            rating < 1400 -> ColorTag.GREEN
            rating < 1600 -> ColorTag.CYAN
            rating < 1900 -> ColorTag.BLUE
            rating < 2100 -> ColorTag.VIOLET
            rating < 2400 -> ColorTag.ORANGE
            rating < 3000 -> ColorTag.RED
            else -> ColorTag.LEGENDARY
        }
    }

    fun getHandleColorByTag(tag: ColorTag): HandleColor? {
        return when (tag) {
            ColorTag.GRAY -> HandleColor.GRAY
            ColorTag.GREEN -> HandleColor.GREEN
            ColorTag.CYAN -> HandleColor.CYAN
            ColorTag.BLUE -> HandleColor.BLUE
            ColorTag.VIOLET -> HandleColor.VIOLET
            ColorTag.ORANGE -> HandleColor.ORANGE
            ColorTag.RED, ColorTag.LEGENDARY -> HandleColor.RED
            else -> null
        }
    }

    suspend fun getRealHandle(handle: String): Pair<String, STATUS> {
        val page = CodeforcesAPI.getPageSource(CodeforcesAPI.URLFactory.user(handle), CodeforcesLocale.EN) ?: return handle to STATUS.FAILED
        val realHandle = extractRealHandle(page) ?: return handle to STATUS.NOT_FOUND
        return realHandle to STATUS.OK
    }

    private fun extractRealHandle(s: String): String? {
        val userBox = Jsoup.parse(s).selectFirst("div.userbox") ?: return null
        return userBox.selectFirst("a.rated-user")?.text()
    }

    @Composable
    fun VotedText(rating: Int, fontSize: TextUnit) {
        if (rating != 0) {
            Text(
                text = signedToString(rating),
                color = if (rating > 0) cpsColors.votedRatingPositive else cpsColors.votedRatingNegative,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize
            )
        }
    }

}


enum class CodeforcesLocale {
    EN, RU;

    override fun toString(): String {
        return when(this){
            EN -> "en"
            RU -> "ru"
        }
    }
}