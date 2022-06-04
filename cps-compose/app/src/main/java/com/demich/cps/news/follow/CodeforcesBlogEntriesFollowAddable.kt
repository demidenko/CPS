package com.demich.cps.news.follow

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import com.demich.cps.news.codeforces.CodeforcesBlogEntries
import com.demich.cps.news.codeforces.CodeforcesBlogEntriesController
import com.demich.cps.news.codeforces.CodeforcesNewsController
import com.demich.cps.news.codeforces.LocalCodeforcesAccountManager
import com.demich.cps.news.settings.settingsNews
import com.demich.cps.ui.dialogs.CPSYesNoDialog
import com.demich.cps.utils.codeforces.CodeforcesBlogEntry
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberCollect

@Composable
fun CodeforcesBlogEntriesFollowAddable(
    controller: CodeforcesNewsController,
    blogEntriesController: CodeforcesBlogEntriesController,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    enableScrollBar: Boolean = false,
) {
    val context = context
    val followEnabled by rememberCollect { context.settingsNews.codeforcesFollowEnabled.flow }

    var showAddToFollowDialogFor: CodeforcesBlogEntry? by remember { mutableStateOf(null) }

    CodeforcesBlogEntries(
        blogEntriesController = blogEntriesController,
        modifier = modifier,
        lazyListState = lazyListState,
        enableScrollBar = enableScrollBar,
        onLongClick = { blogEntry: CodeforcesBlogEntry -> showAddToFollowDialogFor = blogEntry }.takeIf { followEnabled }
    )

    showAddToFollowDialogFor?.let { blogEntry ->
        CPSYesNoDialog(
            title = {
                Text(text = buildAnnotatedString {
                    append("Add ")
                    append(LocalCodeforcesAccountManager.current.makeHandleSpan(
                        handle = blogEntry.authorHandle,
                        tag = blogEntry.authorColorTag
                    ))
                    append(" to follow list?")
                })
            },
            onDismissRequest = { showAddToFollowDialogFor = null },
            onConfirmRequest = {
                controller.addToFollow(handle = blogEntry.authorHandle, context = context)
            }
        )
    }
}
