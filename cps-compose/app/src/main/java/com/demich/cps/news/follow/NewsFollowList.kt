package com.demich.cps.news.follow

import androidx.compose.runtime.*
import com.demich.cps.AdditionalBottomBarBuilder
import com.demich.cps.accounts.DialogAccountChooser
import com.demich.cps.accounts.managers.CodeforcesAccountManager
import com.demich.cps.room.followListDao
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.LazyColumnWithScrollBar
import com.demich.cps.ui.itemsNotEmpty
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.launch

@Composable
fun NewsFollowList() {
    val context = context

    val userBlogsState = rememberCollect {
        context.followListDao.flowOfAll()
    }

    LazyColumnWithScrollBar {
        itemsNotEmpty(userBlogsState.value) {

        }
    }

}

fun newsFollowListBottomBarBuilder(): AdditionalBottomBarBuilder = {
    val context = context
    val scope = rememberCoroutineScope()

    var showChooseDialog by remember { mutableStateOf(false) }

    CPSIconButton(icon = CPSIcons.Add) {
        showChooseDialog = true
    }

    if (showChooseDialog) {
        DialogAccountChooser(
            manager = CodeforcesAccountManager(context),
            onDismissRequest = { showChooseDialog = false },
            onResult = { userInfo ->
                scope.launch {

                }
            }
        )
    }
}