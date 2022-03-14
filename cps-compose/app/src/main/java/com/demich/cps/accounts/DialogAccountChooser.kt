package com.demich.cps.accounts

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSDialog
import com.demich.cps.ui.LazyColumnWithScrollBar
import com.demich.cps.ui.MonospacedText
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.context
import com.demich.cps.utils.showToast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.reflect.KFunction1

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun<U: UserInfo> DialogAccountChooser(
    manager: AccountManager<U>,
    onDismissRequest: () -> Unit,
    onResult: (U) -> Unit
) {
    val charValidator: KFunction1<Char, Boolean> =
        if (manager is AccountSuggestionsProvider) manager::isValidForSearch
        else manager::isValidForUserId

    CPSDialog(onDismissRequest = onDismissRequest) {
        val context = context

        val iconSize = 32.dp
        val inputTextSize = 18.sp
        val resultTextSize = 14.sp

        var userId by remember { mutableStateOf("") }
        var userInfo by remember { mutableStateOf(manager.emptyInfo()) }
        var loadingInProgress by remember { mutableStateOf(false) }

        var suggestionsList by remember { mutableStateOf(emptyList<AccountSuggestion>()) }
        var blockSuggestionsReload by remember { mutableStateOf(false) }
        var loadingSuggestionsInProgress by remember { mutableStateOf(false) }

        val focusRequester = remember { FocusRequester() }

        val done = {
            if (userInfo.status != STATUS.NOT_FOUND && !loadingInProgress) {
                if (userId.all(manager::isValidForUserId)) {
                    onResult(userInfo)
                    onDismissRequest()
                } else {
                    context.showToast("${manager.userIdTitle} contains unacceptable symbols")
                }
            }
        }

        MonospacedText(
            text = "getUser(${manager.managerName}):",
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth()
        )

        TextField(
            value = userId,
            modifier = Modifier.focusRequester(focusRequester).fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(fontSize = inputTextSize),
            onValueChange = { str ->
                if (!str.all(charValidator)) return@TextField
                userId = str
            },
            placeholder = {
                val label = buildString {
                    append(manager.userIdTitle)
                    if (manager is AccountSuggestionsProvider) append(" or search query")
                }
                Text(text = label, color = cpsColors.textColorAdditional)
            },
            label = {
                Text(
                    text = makeUserInfoSpan(userInfo, manager),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = resultTextSize
                )
            },
            trailingIcon = {
                if (loadingInProgress || userInfo.status != STATUS.NOT_FOUND)
                    IconButton(
                        onClick = done,
                        enabled = !loadingInProgress
                    ) {
                        if (loadingInProgress) {
                            CircularProgressIndicator(
                                color = cpsColors.textColor,
                                modifier = Modifier.size(iconSize),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = null,
                                tint = cpsColors.success,
                                modifier = Modifier
                                    .size(iconSize)
                                    .border(
                                        border = ButtonDefaults.outlinedBorder,
                                        shape = MaterialTheme.shapes.small
                                    )
                            )
                        }
                    }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { done() }),
        )

        LazyColumnWithScrollBar(
            modifier = Modifier
                .heightIn(max = 250.dp) //TODO ajustSpan like solution needed
        ) {
            items(suggestionsList, key = { it.userId }) {
                SuggestionItem(it)
                Divider()
            }
        }

        LaunchedEffect(userId) {
            if (userId.length < 3) {
                suggestionsList = emptyList()
                loadingSuggestionsInProgress = false
            }
            if (userId.isBlank()) {
                loadingInProgress = false
                loadingSuggestionsInProgress = false
                return@LaunchedEffect
            }
            userInfo = manager.emptyInfo()
            delay(300)
            launch {
                loadingInProgress = true
                userInfo = manager.loadInfo(userId, 1)
                loadingInProgress = false
            }
            if (manager is AccountSuggestionsProvider && userId.length > 2) {
                launch {
                    loadingSuggestionsInProgress = true
                    suggestionsList = manager.loadSuggestions(userId) ?: emptyList() //TODO indicate error on null
                    loadingSuggestionsInProgress = false
                }
            }
        }

        LaunchedEffect(Unit) {
            delay(100) //TODO fix this shit: keyboard not showed without it
            focusRequester.requestFocus()
        }
    }
}

@Composable
private fun SuggestionItem(suggestion: AccountSuggestion) {
    Row(
        modifier = Modifier.padding(3.dp)
    ) {
        Text(
            text = suggestion.title,
            fontSize = 17.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (suggestion.info.isNotBlank()) {
            Text(
                text = suggestion.info,
                fontSize = 17.sp,
                modifier = Modifier.padding(start = 5.dp)
            )
        }
    }
}

@Composable
fun<U: UserInfo> makeUserInfoSpan(userInfo: U, manager: AccountManager<U>): AnnotatedString {
    if (userInfo.isEmpty()) return AnnotatedString("")
    return buildAnnotatedString {
        withStyle(SpanStyle(color = cpsColors.textColor)) {
            when (userInfo.status) {
                STATUS.OK -> append(manager.makeOKInfoSpan(userInfo))
                STATUS.NOT_FOUND -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append("User not found")
                    }
                }
                STATUS.FAILED -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append("Load failed")
                    }
                }
            }
        }
    }
}