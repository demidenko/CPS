package com.demich.cps.community.follow

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import com.demich.cps.community.codeforces.CodeforcesBlogEntries
import com.demich.cps.community.codeforces.CodeforcesNewEntriesState
import com.demich.cps.community.settings.settingsCommunity
import com.demich.cps.features.codeforces.follow.database.addNewUser
import com.demich.cps.platforms.utils.codeforces.CodeforcesWebBlogEntry
import com.demich.cps.profiles.managers.toHandleSpan
import com.demich.cps.ui.dialogs.CPSYesNoDialog
import com.demich.cps.ui.withVibration
import com.demich.cps.utils.backgroundCoroutineScope
import com.demich.cps.utils.context
import kotlinx.coroutines.launch

@Composable
fun CodeforcesBlogEntriesFollowAddable(
    blogEntries: () -> List<CodeforcesWebBlogEntry>,
    newEntriesState: CodeforcesNewEntriesState,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    scrollBarEnabled: Boolean = false,
    label: (@Composable (CodeforcesWebBlogEntry) -> Unit)? = null
) {
    val context = context

    var showAddToFollowDialogFor: CodeforcesWebBlogEntry? by remember { mutableStateOf(null) }

    CodeforcesBlogEntries(
        blogEntries = blogEntries,
        newEntriesState = newEntriesState,
        modifier = modifier,
        lazyListState = lazyListState,
        scrollBarEnabled = scrollBarEnabled,
        onLongClick = withVibration { showAddToFollowDialogFor = it },
        label = label
    )

    showAddToFollowDialogFor?.let { blogEntry ->
        val scope = backgroundCoroutineScope
        BlogEntryDialog(
            blogEntry = blogEntry,
            onDismissRequest = { showAddToFollowDialogFor = null },
            onConfirmRequest = {
                scope.launch {
                    context.settingsCommunity.codeforcesFollowEnabled.setValue(true)
                    context.followRepository.addNewUser(blogEntry.author.handle)
                }
            }
        )
    }
}

@Composable
private fun BlogEntryDialog(
    blogEntry: CodeforcesWebBlogEntry,
    onDismissRequest: () -> Unit,
    onConfirmRequest: () -> Unit
) {
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
                append(blogEntry.author.toHandleSpan())
                append(" to follow list?")
            })
        },
        onDismissRequest = onDismissRequest,
        onConfirmRequest = onConfirmRequest
    )
}
