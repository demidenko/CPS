package com.demich.cps.community.codeforces

import androidx.compose.runtime.Composable
import com.demich.cps.ui.CPSIconButton
import com.demich.cps.ui.CPSIcons

@Composable
fun CodeforcesCommunityBottomBar(
    controller: CodeforcesCommunityController,
) {
    when (controller.currentTab) {
        CodeforcesTitle.TOP -> {
            CommentsModeButton(
                isOn = controller.topPageType == CodeforcesCommunityController.TopPageType.Comments
            ) { isOn ->
                controller.topPageType = if (isOn) CodeforcesCommunityController.TopPageType.Comments
                else CodeforcesCommunityController.TopPageType.BlogEntries
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