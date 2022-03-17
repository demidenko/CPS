package com.demich.cps.accounts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.theme.cpsColors

@Composable
fun<U: UserInfo> AccountManager<U>.Panel() {
    val userInfo: U by flowOfInfo().collectAsState(emptyInfo())
    if (!userInfo.isEmpty()) Panel(userInfo)
}

@Composable
fun SmallAccountPanelTwoLines(
    title: @Composable () -> Unit,
    additionalTitle: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, top = 10.dp)
    ) {
        title()
        additionalTitle()
    }
}

@Composable
fun<U: UserInfo> RatedAccountManager<U>.SmallAccountPanelTypeRated(userInfo: U) {
    SmallAccountPanelTwoLines(
        title = {
            Text(
                text = makeHandleSpan(userInfo),
                fontSize = 30.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        additionalTitle = {
            if (userInfo.status == STATUS.OK) {
                val rating = getRating(userInfo)
                Text(
                    text = if (rating == NOT_RATED) "[not rated]" else rating.toString(),
                    fontSize = 25.sp,
                    color = colorFor(rating = rating),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}

@Composable
fun SmallAccountPanelTypeArchive(
    title: String,
    infoArgs: List<Pair<String, String>>
) {
    SmallAccountPanelTwoLines(
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                color = cpsColors.textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        additionalTitle = {
            Text(
                text = buildAnnotatedString {
                    infoArgs.forEach {
                        withStyle(SpanStyle(color = cpsColors.textColorAdditional)) {
                            append("${it.first}: ")
                        }
                        append("${it.second} ")
                    }
                },
                fontSize = 15.sp,
                color = cpsColors.textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}