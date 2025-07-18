package com.demich.cps.accounts.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.accounts.userinfo.CodeforcesUserInfo
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.userInfoOrNull
import com.demich.cps.platforms.api.codeforces.CodeforcesUrls
import com.demich.cps.ui.CPSFontSize
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.ListTitle
import com.demich.cps.ui.VotedRating
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.lazylist.LazyColumnOfData
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.collectAsState
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import com.demich.cps.utils.jsonCPS
import com.demich.cps.utils.openUrlInBrowser
import com.demich.cps.utils.saver
import kotlinx.coroutines.flow.map

@Composable
context(manager: CodeforcesAccountManager)
fun CodeforcesUserInfoExpandedContent(
    profileResult: ProfileResult<CodeforcesUserInfo>,
    setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit,
    modifier: Modifier
) {
    var showItem: ItemType? by rememberSaveable(stateSaver = jsonCPS.saver()) {
        mutableStateOf(null)
    }

    Box(modifier = modifier) {
        Column {
            manager.PanelContent(profileResult)
            if (profileResult is ProfileResult.Success) {
                val userInfo = profileResult.userInfo
                if (userInfo.contribution != 0) {
                    Contribution(contribution = userInfo.contribution)
                }
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            //TODO: saveables as pager
            when (showItem) {
                ItemType.RATING -> {
                    if (profileResult is ProfileResult.Success) {
                        RatingGraphItem(
                            manager = manager,
                            handle = profileResult.userInfo.handle
                        )
                    }
                }
                ItemType.UPSOLVING -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ListTitle(text = "upsolving suggestions:")
                        UpsolvingSuggestionsList(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 5.dp)
                                .heightIn(max = 240.dp)
                        )
                    }
                }
                null -> Unit
            }
        }
    }

    val context = context
    val upsolvingSuggestionsEnabled by collectItemAsState {
        manager.getSettings(context).upsolvingSuggestionsEnabled
    }

    setBottomBarContent {
        if (upsolvingSuggestionsEnabled) {
            CPSIconButton(
                icon = CPSIcons.Upsolving,
                enabled = showItem != ItemType.UPSOLVING,
                onClick = { showItem = ItemType.UPSOLVING }
            )
        }
        if (profileResult.userInfoOrNull()?.rating != null) {
            CPSIconButton(
                icon = CPSIcons.RatingGraph,
                enabled = showItem != ItemType.RATING,
                onClick = { showItem = ItemType.RATING }
            )
        }
    }
}

private enum class ItemType {
    RATING, UPSOLVING
}

@Composable
private fun Contribution(contribution: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = "contribution:",
            color = cpsColors.contentAdditional,
            fontSize = 18.sp
        )
        VotedRating(
            rating = contribution,
            fontSize = 20.sp
        )
    }
}

@Composable
private fun UpsolvingSuggestionsList(
    modifier: Modifier = Modifier
) {
    val context = context
    val problems by collectAsState {
        CodeforcesAccountManager().dataStore(context)
            .upsolvingSuggestedProblems.asFlow()
            .map { it.valuesSortedByTime().asReversed() }
    }

    LazyColumnOfData(
        items = { problems },
        key = { it.problemId },
        modifier = modifier
    ) {
        Text(
            text = "${it.problemId}. ${it.name}",
            fontSize = CPSFontSize.itemTitle,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clickable {
                    context.openUrlInBrowser(
                        CodeforcesUrls.problem(
                            contestId = it.contestId,
                            problemIndex = it.index
                        )
                    )
                }
                .padding(horizontal = 2.dp)
                .padding(vertical = 4.dp)
                .fillMaxWidth()
        )
        Divider()
    }
}