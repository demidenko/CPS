package com.demich.cps.news.follow

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import com.demich.cps.LocalCodeforcesAccountManager
import com.demich.cps.news.codeforces.CodeforcesBlogEntries
import com.demich.cps.news.codeforces.CodeforcesBlogEntriesController
import com.demich.cps.news.codeforces.CodeforcesNewsController
import com.demich.cps.platforms.api.CodeforcesBlogEntry
import com.demich.cps.platforms.utils.codeforces.author
import com.demich.cps.ui.dialogs.CPSYesNoDialog
import com.demich.cps.utils.context

@Composable
fun CodeforcesBlogEntriesFollowAddable(
    controller: CodeforcesNewsController,
    blogEntriesController: CodeforcesBlogEntriesController,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    scrollBarEnabled: Boolean = false,
    label: (@Composable (CodeforcesBlogEntry) -> Unit)? = null
) {
    val context = context

    var showAddToFollowDialogFor: CodeforcesBlogEntry? by remember { mutableStateOf(null) }

    CodeforcesBlogEntries(
        blogEntriesController = blogEntriesController,
        modifier = modifier,
        lazyListState = lazyListState,
        scrollBarEnabled = scrollBarEnabled,
        onLongClick = { showAddToFollowDialogFor = it },
        label = label
    )

    showAddToFollowDialogFor?.let { blogEntry ->
        /*
            TODO:
            - user already in follow
            - note if follow disabled
            - show user blog
         */
        CPSYesNoDialog(
            title = {
                Text(text = buildAnnotatedString {
                    append("Add ")
                    append(LocalCodeforcesAccountManager.current.makeHandleSpan(blogEntry.author))
                    append(" to follow list?")
                })
            },
            onDismissRequest = { showAddToFollowDialogFor = null },
            onConfirmRequest = {
                controller.addToFollowList(handle = blogEntry.authorHandle, context = context)
            }
        )
    }
}
