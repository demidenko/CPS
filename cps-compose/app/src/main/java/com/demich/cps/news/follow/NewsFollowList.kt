package com.demich.cps.news.follow

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.accounts.DialogAccountChooser
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.news.codeforces.CodeforcesNewsViewModel
import com.demich.cps.news.codeforces.LocalCodeforcesAccountManager
import com.demich.cps.room.followListDao
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.LazyColumnWithScrollBar
import com.demich.cps.ui.itemsNotEmpty
import com.demich.cps.utils.LocalCurrentTime
import com.demich.cps.utils.collectCurrentTimeEachMinute
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.flow.map

@Composable
fun NewsFollowList() {
    val context = context

    val userBlogsState = rememberCollect {
        context.followListDao.flowOfAll().map {
            it.sortedByDescending { it.id }
        }
    }

    val manager = remember { CodeforcesAccountManager(context) }
    val currentTime by collectCurrentTimeEachMinute()
    CompositionLocalProvider(
        LocalCodeforcesAccountManager provides manager,
        LocalCurrentTime provides currentTime
    ) {
        LazyColumnWithScrollBar {
            itemsNotEmpty(userBlogsState.value) {
                NewsFollowListItem(
                    userInfo = it.userInfo,
                    blogEntriesCount = it.blogEntries?.size,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )
                Divider()
            }
        }
    }

}

fun newsFollowListBottomBarBuilder(
    newsViewModel: CodeforcesNewsViewModel
): AdditionalBottomBarBuilder = {
    val context = context

    var showChooseDialog by remember { mutableStateOf(false) }

    CPSIconButton(icon = CPSIcons.Add) {
        showChooseDialog = true
    }

    if (showChooseDialog) {
        DialogAccountChooser(
            manager = CodeforcesAccountManager(context),
            onDismissRequest = { showChooseDialog = false },
            onResult = { newsViewModel.addToFollowList(userInfo = it, context = context) }
        )
    }
}