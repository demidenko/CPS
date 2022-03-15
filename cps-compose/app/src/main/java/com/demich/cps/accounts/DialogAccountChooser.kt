package com.demich.cps.accounts

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.reflect.KFunction1

@Composable
fun<U: UserInfo> DialogAccountChooser(
    manager: AccountManager<U>,
    initialUserInfo: U = manager.emptyInfo(),
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
        val suggestionTextLimit = 3

        var userInfo by remember { mutableStateOf(initialUserInfo) }
        var ignoreLaunch by remember { mutableStateOf(true) }

        var textFieldValue by remember { mutableStateOf(initialUserInfo.userId.toTextFieldValue()) }
        val userId by derivedStateOf { textFieldValue.text }

        var loadingInProgress by remember { mutableStateOf(false) }

        var loadingSuggestionsInProgress by remember { mutableStateOf(false) }
        var suggestionsLoadError by remember { mutableStateOf(false) }
        var blockSuggestionsReload by remember { mutableStateOf(false) }
        var suggestionsList by remember { mutableStateOf(emptyList<AccountSuggestion>()) }

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

        AccountChooserHeader(
            text = "getUser(${manager.managerName}):",
            color = cpsColors.textColorAdditional
        ) {
            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = it)
        }

        TextField(
            value = textFieldValue,
            modifier = Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(fontSize = inputTextSize, fontFamily = FontFamily.Monospace),
            onValueChange = { value ->
                val str = value.text
                if (!str.all(charValidator)) return@TextField
                textFieldValue = value
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

        if (manager is AccountSuggestionsProvider) {
            SuggestionsList(
                suggestions = suggestionsList,
                isLoading = loadingSuggestionsInProgress,
                isError = suggestionsLoadError,
                modifier = Modifier.fillMaxWidth(),
                onClick = { suggestion ->
                    blockSuggestionsReload = true
                    textFieldValue = suggestion.userId.toTextFieldValue()
                }
            )
        }

        LaunchedEffect(userId) {
            if (ignoreLaunch) {
                ignoreLaunch = false
                return@LaunchedEffect
            }
            userInfo = manager.emptyInfo()
            if (userId.length < suggestionTextLimit) {
                suggestionsList = emptyList()
                loadingSuggestionsInProgress = false
                suggestionsLoadError = false
                blockSuggestionsReload = false
            }
            if (userId.isBlank()) {
                loadingInProgress = false
                loadingSuggestionsInProgress = false
                return@LaunchedEffect
            }
            delay(300)
            launch {
                loadingInProgress = true
                userInfo = manager.loadInfo(userId, 1)
                loadingInProgress = false
            }
            if (manager is AccountSuggestionsProvider && userId.length >= suggestionTextLimit) {
                if (!blockSuggestionsReload) {
                    launch {
                        loadingSuggestionsInProgress = true
                        val result = manager.loadSuggestions(userId)
                        loadingSuggestionsInProgress = false
                        if (isActive) { //Because of "StandaloneCoroutine was cancelled" exception during cancelling LaunchedEffect
                            suggestionsList = result ?: emptyList()
                            suggestionsLoadError = result == null
                        }
                    }
                } else {
                    blockSuggestionsReload = false
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
private fun SuggestionsList(
    suggestions: List<AccountSuggestion>,
    isLoading: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier,
    onClick: (AccountSuggestion) -> Unit
) {
    if (suggestions.isNotEmpty() || isLoading || isError)
    Column(
        modifier = modifier
    ) {
        AccountChooserHeader(
            text = if (isError) "suggestions load failed" else "suggestions:",
            color = if (isError) cpsColors.errorColor else cpsColors.textColorAdditional
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = it,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Search,
                    tint = it,
                    contentDescription = null
                )
            }
        }
        LazyColumnWithScrollBar(
            modifier = Modifier
                .heightIn(max = 230.dp) //TODO ajustSpan like solution needed
        ) {
            items(suggestions, key = { it.userId }) {
                SuggestionItem(suggestion = it, onClick = onClick)
                Divider()
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    suggestion: AccountSuggestion,
    onClick: (AccountSuggestion) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(3.dp)
            .clickable { onClick(suggestion) }
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
private fun AccountChooserHeader(
    text: String,
    color: Color,
    icon: @Composable (Color) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconSize = 18.dp
        Box(modifier = Modifier
            .padding(3.dp)
            .size(iconSize)) {
            icon(color)
        }
        MonospacedText(
            text = text,
            fontSize = 14.sp,
            color = color
        )
    }
}

private fun String.toTextFieldValue() = TextFieldValue(text = this, selection = TextRange(length))

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