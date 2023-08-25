package com.demich.cps.accounts.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.accounts.SmallRatedAccountPanel
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.accounts.userinfo.CodeforcesUserInfo
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.VotedRating
import com.demich.cps.ui.bottombar.AdditionalBottomBarBuilder
import com.demich.cps.ui.lazylist.LazyColumnOfData
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.jsonCPS
import com.demich.cps.utils.rememberCollect
import com.demich.cps.utils.saver
import kotlinx.coroutines.flow.map

@Composable
fun CodeforcesUserInfoExpandedContent(
    userInfo: CodeforcesUserInfo,
    setBottomBarContent: (AdditionalBottomBarBuilder) -> Unit,
    modifier: Modifier
) {
    val manager = remember { CodeforcesAccountManager() }

    var showItem: ItemType? by rememberSaveable(stateSaver = jsonCPS.saver()) {
        mutableStateOf(null)
    }

    Box(modifier = modifier) {
        Column {
            manager.SmallRatedAccountPanel(userInfo)
            if (userInfo.contribution != 0) {
                Contribution(contribution = userInfo.contribution)
            }
        }
        Box(modifier = Modifier
            .align(Alignment.BottomCenter)
            .heightIn(max = 300.dp)
        ) {
            //TODO: saveables as pager
            when (showItem) {
                ItemType.RATING -> {
                    RatingGraphItem(
                        manager = manager,
                        userInfo = userInfo
                    )
                }
                ItemType.UPSOLVING -> {
                    UpsolvingSuggestionsList(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                null -> Unit
            }
        }
    }

    val context = context
    val upsolvingSuggestionsEnabled by rememberCollect {
        manager.getSettings(context).upsolvingSuggestionsEnabled.flow
    }

    setBottomBarContent {
        if (upsolvingSuggestionsEnabled) {
            CPSIconButton(
                icon = CPSIcons.Upsolving,
                enabled = showItem != ItemType.UPSOLVING,
                onClick = { showItem = ItemType.UPSOLVING }
            )
        }
        if (userInfo.hasRating()) {
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "contribution:",
            color = cpsColors.contentAdditional,
            fontSize = 18.sp,
            modifier = Modifier.padding(end = 5.dp)
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
    val problems by rememberCollect {
        CodeforcesAccountManager().dataStore(context)
            .upsolvingSuggestedProblems.flow
            .map { list -> list.map { it.first } }
    }

    LazyColumnOfData(
        items = { problems },
        key = { it.problemId },
        modifier = modifier
    ) {
        Text(text = "${it.problemId}. ${it.name}")
    }
}