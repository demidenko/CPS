package com.demich.cps.accounts

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.accounts.managers.*
import com.demich.cps.ui.dialogs.CPSDialog
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.LazyColumnWithScrollBar
import com.demich.cps.ui.MonospacedText
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.append
import com.demich.cps.utils.context
import com.demich.cps.utils.showToast
import kotlinx.coroutines.Dispatchers
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

        val textFieldValue = remember { mutableStateOf(initialUserInfo.userId.toTextFieldValue()) }
        val userId by remember { derivedStateOf { textFieldValue.value.text } }

        var userInfo by remember { mutableStateOf(initialUserInfo) }
        var loadingInProgress by remember { mutableStateOf(false) }

        var suggestionsList: List<AccountSuggestion> by remember { mutableStateOf(emptyList()) }
        var loadingSuggestionsInProgress by remember { mutableStateOf(false) }
        var suggestionsLoadError by remember { mutableStateOf(false) }
        var blockSuggestionsReload by remember { mutableStateOf(false) }

        val focusRequester = remember { FocusRequester() }
        var ignoreLaunch by remember { mutableStateOf(true) }

        AccountChooserHeader(
            text = "getUser(${manager.type.name}):",
            color = cpsColors.contentAdditional
        ) {
            Icon(imageVector = CPSIcons.Account, contentDescription = null, tint = it)
        }

        UserIdTextField(
            manager = manager,
            userInfo = userInfo,
            textFieldValue = textFieldValue,
            loadingInProgress = loadingInProgress,
            modifier = Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth(),
            inputTextSize = 18.sp,
            resultTextSize = 14.sp,
            inputValidator = charValidator
        ) {
            if (userInfo.status != STATUS.NOT_FOUND && !loadingInProgress) {
                if (userId.all(manager::isValidForUserId)) {
                    onResult(userInfo)
                    onDismissRequest()
                } else {
                    context.showToast("${manager.userIdTitle} contains unacceptable symbols")
                }
            }
        }

        if (manager is AccountSuggestionsProvider) {
            SuggestionsList(
                suggestions = suggestionsList,
                isLoading = loadingSuggestionsInProgress,
                isError = suggestionsLoadError,
                modifier = Modifier.fillMaxWidth(),
                onClick = { suggestion ->
                    blockSuggestionsReload = true
                    textFieldValue.value = suggestion.userId.toTextFieldValue()
                }
            )
        }

        val suggestionTextLimit = 3
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
                    loadingSuggestionsInProgress = true
                    launch(Dispatchers.IO) {
                        val result = manager.runCatching { loadSuggestions(userId) }
                        loadingSuggestionsInProgress = false
                        if (isActive) { //Because of "StandaloneCoroutine was cancelled" exception during cancelling LaunchedEffect
                            suggestionsList = result.getOrDefault(emptyList())
                            suggestionsLoadError = result.isFailure
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
private fun<U: UserInfo> UserIdTextField(
    manager: AccountManager<U>,
    userInfo: U,
    textFieldValue: MutableState<TextFieldValue>,
    loadingInProgress: Boolean,
    modifier: Modifier,
    inputTextSize: TextUnit,
    resultTextSize: TextUnit,
    inputValidator: (Char) -> Boolean,
    onDoneRequest: () -> Unit
) {
    TextField(
        value = textFieldValue.value,
        modifier = modifier,
        singleLine = true,
        textStyle = TextStyle(fontSize = inputTextSize, fontFamily = FontFamily.Monospace),
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = cpsColors.background
        ),
        onValueChange = {
            if (it.text.all(inputValidator)) textFieldValue.value = it
        },
        placeholder = {
            Text(
                text = buildString {
                    append(manager.userIdTitle)
                    if (manager is AccountSuggestionsProvider) append(" or search query")
                },
                color = cpsColors.contentAdditional
            )
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
            TextFieldMainIcon(
                loadingInProgress = loadingInProgress,
                userInfoStatus = userInfo.status,
                iconSize = 32.dp,
                onDoneClick = onDoneRequest
            )
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDoneRequest() }),
    )
}

@Composable
private fun TextFieldMainIcon(
    loadingInProgress: Boolean,
    userInfoStatus: STATUS,
    iconSize: Dp,
    onDoneClick: () -> Unit
) {
    if (loadingInProgress || userInfoStatus != STATUS.NOT_FOUND) {
        IconButton(
            onClick = onDoneClick,
            enabled = !loadingInProgress
        ) {
            if (loadingInProgress) {
                CircularProgressIndicator(
                    color = cpsColors.content,
                    modifier = Modifier.size(iconSize),
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    imageVector = CPSIcons.Done,
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
    if (suggestions.isNotEmpty() || isLoading || isError) {
        Column(modifier = modifier) {
            AccountChooserHeader(
                text = if (isError) "suggestions load failed" else "suggestions:",
                color = if (isError) cpsColors.error else cpsColors.contentAdditional
            ) { color ->
                if (isLoading) {
                    CircularProgressIndicator(
                        color = color,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = CPSIcons.Search,
                        tint = color,
                        contentDescription = null
                    )
                }
            }
            LazyColumnWithScrollBar(
                modifier = Modifier
                    .heightIn(max = 230.dp) //TODO adjustSpan like solution needed
            ) {
                items(suggestions) {
                    SuggestionItem(suggestion = it, onClick = onClick)
                    Divider()
                }
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
        Box(modifier = Modifier
            .padding(3.dp)
            .size(18.dp)) {
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
        withStyle(SpanStyle(color = cpsColors.content)) {
            when (userInfo.status) {
                STATUS.OK -> append(manager.makeOKInfoSpan(userInfo))
                STATUS.NOT_FOUND -> append(text = "User not found", fontStyle = FontStyle.Italic)
                STATUS.FAILED -> append(text = "Loading failed", fontStyle = FontStyle.Italic)
            }
        }
    }
}