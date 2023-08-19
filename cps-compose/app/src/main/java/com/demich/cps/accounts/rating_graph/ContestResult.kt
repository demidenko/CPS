package com.demich.cps.accounts.rating_graph

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.accounts.HandleColor
import com.demich.cps.accounts.managers.RatedAccountManager
import com.demich.cps.accounts.managers.RatingChange
import com.demich.cps.accounts.managers.colorFor
import com.demich.cps.accounts.userinfo.RatedUserInfo
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.theme.CPSTheme
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.format
import com.demich.cps.utils.toSignedString
import kotlinx.datetime.Instant

@Composable
internal fun ContestResult(
    ratingChange: RatingChange,
    manager: RatedAccountManager<out RatedUserInfo>,
    rectangles: RatingGraphRectangles,
    modifier: Modifier = Modifier
) {
    ContestResult(
        ratingChange = ratingChange,
        ratingColor = manager.colorFor(handleColor = rectangles.getHandleColor(ratingChange.toPoint())),
        modifier = modifier
    )
}

@Composable
private fun ContestResult(
    ratingChange: RatingChange,
    ratingColor: Color,
    modifier: Modifier = Modifier,
    titleFontSize: TextUnit = 16.sp,
    subTitleFontSize: TextUnit = 12.sp,
    ratingFontSize: TextUnit = 30.sp
) {
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ratingChange.title,
                fontSize = titleFontSize,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(align = Alignment.CenterVertically)
            )
            Text(
                text = ratingChange.run {
                    date.format("dd.MM.yyyy HH:mm") + "  rank: $rank"
                },
                fontSize = subTitleFontSize,
                color = cpsColors.contentAdditional
            )
        }
        Column(
            modifier = Modifier.padding(start = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = ratingChange.rating.toString(),
                fontSize = ratingFontSize,
                fontWeight = FontWeight.Bold,
                color = ratingColor,
            )
            if (ratingChange.oldRating != null) {
                RatingChange(
                    change = ratingChange.rating - ratingChange.oldRating,
                    fontSize = subTitleFontSize
                )
            }
        }
    }
}

@Composable
private fun RatingChange(
    change: Int,
    fontSize: TextUnit
) {
    val color = if (change < 0) cpsColors.error else cpsColors.success
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconSp(
            imageVector = if (change < 0) CPSIcons.RatingDown else CPSIcons.RatingUp,
            size = fontSize,
            color = color
        )
        Text(
            text = change.toSignedString(zeroAsPositive = true),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(start = 3.dp)
        )
    }
}

@Composable
private fun ContestResultTest(
    change: Int?,
    longTitle: Boolean = false,
    handleColor: HandleColor = HandleColor.ORANGE
) {
    val rating = 2150
    ContestResult(
        ratingChange = RatingChange(
            title = "Contest " + "very long ".repeat(if (longTitle) 10 else 0) + "title",
            date = Instant.fromEpochSeconds(1e9.toLong()),
            rating = rating,
            rank = 345,
            oldRating = if (change != null) rating - change else null
        ),
        ratingColor = cpsColors.handleColor(handleColor = handleColor),
        modifier = Modifier
            .background(cpsColors.backgroundAdditional)
            .padding(all = 3.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun ContestResults() {
    CPSTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(5.dp)
        ) {
            ContestResultTest(change = +150)
            ContestResultTest(change = -150)
            ContestResultTest(change = null)
            ContestResultTest(change = 0, longTitle = true)
        }
    }
}