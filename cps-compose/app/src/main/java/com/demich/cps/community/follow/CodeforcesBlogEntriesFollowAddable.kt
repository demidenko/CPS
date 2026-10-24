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
import com.demich.cps.platforms.utils.codeforces.CodeforcesHandle
import com.demich.cps.platforms.utils.codeforces.CodeforcesWebBlogEntry
import com.demich.cps.profiles.managers.toHandleSpan
import com.demich.cps.ui.dialogs.CPSYesNoDialog
import com.demich.cps.ui.withVibration
import com.demich.cps.utils.backgroundCoroutineScope
import com.demich.cps.utils.context
import kotlinx.coroutines.launch

@Composable
fun CodeforcesBlogEntriesFollowAddable(
    blogEntries: List<CodeforcesWebBlogEntry>?,
    newEntriesState: CodeforcesNewEntriesState,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    scrollBarEnabled: Boolean = false,
    label: (@Composable (CodeforcesWebBlogEntry) -> Unit)? = null
) {
    var showAddToFollowDialogFor: CodeforcesHandle? by remember { mutableStateOf(null) }

    CodeforcesBlogEntries(
        blogEntries = blogEntries,
        newEntriesState = newEntriesState,
        modifier = modifier,
        lazyListState = lazyListState,
        scrollBarEnabled = scrollBarEnabled,
        onLongClick = withVibration { showAddToFollowDialogFor = it.author },
        label = label
    )

    showAddToFollowDialogFor?.let { user ->
        val context = context
        val scope = backgroundCoroutineScope
        UserBlogDialog(
            user = user,
            onDismissRequest = { showAddToFollowDialogFor = null },
            onConfirmRequest = {
                scope.launch {
                    context.settingsCommunity.codeforcesFollowEnabled.setValue(true)
                    context.followRepository.addNewUser(user.handle)
                }
            }
        )
    }
}

@Composable
private fun UserBlogDialog(
    user: CodeforcesHandle,
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
                append(user.toHandleSpan())
                append(" to follow list?")
            })
        },
        onDismissRequest = onDismissRequest,
        onConfirmRequest = onConfirmRequest
    )
}
