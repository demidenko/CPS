package com.demich.cps.accounts.rating_graph

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.accounts.managers.RatedAccountManager
import com.demich.cps.accounts.managers.RatedUserInfo
import com.demich.cps.accounts.managers.RatingChange
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.format
import com.demich.cps.utils.toSignedString

@Composable
internal fun ContestResult(
    ratingChange: RatingChange,
    manager: RatedAccountManager<out RatedUserInfo>,
    rectangles: RatingGraphRectangles,
    modifier: Modifier = Modifier,
    titleFontSize: TextUnit = 16.sp,
    subTitleFontSize: TextUnit = 12.sp,
    ratingFontSize: TextUnit = 30.sp
) {
    ContestResult(
        ratingChange = ratingChange,
        ratingColor = manager.colorFor(handleColor = rectangles.getHandleColor(ratingChange.toPoint())),
        titleFontSize = titleFontSize,
        subTitleFontSize = subTitleFontSize,
        ratingFontSize = ratingFontSize,
        modifier = modifier
    )
}

@Composable
private fun ContestResult(
    ratingChange: RatingChange,
    ratingColor: Color,
    titleFontSize: TextUnit,
    subTitleFontSize: TextUnit,
    ratingFontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = ratingChange.title, fontSize = titleFontSize)
            Text(
                text = buildString {
                    append(ratingChange.date.format("dd.MM.yyyy HH:mm"))
                    ratingChange.rank?.let {
                        append("  rank: $it")
                    }
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
                val change = ratingChange.rating - ratingChange.oldRating
                Text(
                    text = change.toSignedString(),
                    fontSize = subTitleFontSize,
                    fontWeight = FontWeight.Bold,
                    color = if (change < 0) cpsColors.error else cpsColors.success,
                )
            }
        }
    }
}