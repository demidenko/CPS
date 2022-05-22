package com.demich.cps.news.codeforces

import androidx.compose.runtime.Composable
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons

@Composable
fun CodeforcesNewsBottomBar(
    controller: CodeforcesNewsController,
) {
    when (controller.currentTab) {
        CodeforcesTitle.TOP -> {
            TopSwitchButton(inCommentsMode = controller.topShowComments) {
                controller.topShowComments = controller.topShowComments.not()
            }
        }
        else -> Unit
    }
}

@Composable
private fun TopSwitchButton(
    inCommentsMode: Boolean,
    onClick: () -> Unit
) {
    CPSIconButton(
        icon = CPSIcons.Comments,
        onState = inCommentsMode,
        onClick = onClick
    )
}