package com.demich.cps.news.follow

import androidx.compose.runtime.Composable
import com.demich.cps.room.followListDao
import com.demich.cps.ui.LazyColumnWithScrollBar
import com.demich.cps.ui.itemsNotEmpty
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect

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
