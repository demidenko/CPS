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
            CommentsModeButton(isOn = controller.topShowComments) {
                controller.topShowComments = it
            }
        }
        CodeforcesTitle.RECENT -> {
            if (controller.recentFilterByBlogEntry != null) {
                CPSIconButton(icon = CPSIcons.ArrowBack) {
                    controller.recentFilterByBlogEntry = null
                }
            } else {
                CommentsModeButton(isOn = controller.recentShowComments) {
                    controller.recentShowComments = it
                }
            }
        }
        else -> Unit
    }
}

@Composable
private fun CommentsModeButton(
    isOn: Boolean,
    onModeChange: (Boolean) -> Unit
) {
    CPSIconButton(
        icon = CPSIcons.Comments,
        onState = isOn
    ) {
        onModeChange(!isOn)
    }
}