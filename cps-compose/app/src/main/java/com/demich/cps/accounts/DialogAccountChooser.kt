package com.demich.cps.accounts

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.accounts.managers.AccountManager
import com.demich.cps.accounts.managers.ProfileSuggestionsProvider
import com.demich.cps.accounts.userinfo.ProfileResult
import com.demich.cps.accounts.userinfo.UserInfo
import com.demich.cps.accounts.userinfo.UserSuggestion
import com.demich.cps.ui.CPSDefaults
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.LoadingIndicator
import com.demich.cps.ui.dialogs.CPSDialog
import com.demich.cps.ui.lazylist.LazyColumnWithScrollBar
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.ProvideContentColor
import com.demich.cps.utils.append
import com.demich.cps.utils.context
import com.demich.cps.utils.rememberFocusOnCreationRequester
import com.demich.cps.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext

private const val suggestionsMinLength = 3
private const val requestDebounceDelay: Long = 300

@Composable
fun <U: UserInfo> DialogAccountChooser(
    manager: AccountManager<U>,
    initial: ProfileResult<U>?,
    onDismissRequest: () -> Unit,
    onResult: (ProfileResult<U>) -> Unit
) {
    CPSDialog(onDismissRequest = onDismissRequest) {
        AccountChooserHeader(
            text = "getUser(${manager.type.name}):",
            color = cpsColors.contentAdditional
        ) {
            Icon(imageVector = CPSIcons.Profile, contentDescription = null)
        }

        DialogContent(
            manager = manager,
            initial = initial,
            onDismissRequest = onDismissRequest,
            onResult = onResult,
            charValidator = when (manager) {
                is ProfileSuggestionsProvider -> manager::isValidForSearch
                else -> manager::isValidForUserId
            }
        )
    }
}

@Composable
private fun<U: UserInfo> DialogContent(
    manager: AccountManager<U>,
    initial: ProfileResult<U>?,
    onDismissRequest: () -> Unit,
    onResult: (ProfileResult<U>) -> Unit,
    charValidator: (Char) -> Boolean
) {
    val context = context

    val textFieldValueState = rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(initial?.userId.orEmpty().toTextFieldValue())
    }
    var textFieldValue by textFieldValueState

    val profileLoading by profileState(
        textState = textFieldValueState,
        manager = manager,
        initial = initial
    )

    var blockSuggestionsReload by remember { mutableStateOf(true) }

    val focusRequester = rememberFocusOnCreationRequester()

    UserIdTextField(
        manager = manager,
        profileResult = profileLoading.profile,
        loadingInProgress = profileLoading.isLoading,
        textFieldValue = textFieldValue,
        onValueChange = {
            blockSuggestionsReload = false
            if (it.text.all(charValidator)) {
                textFieldValue = it
            } else {
                // keyboard suggestion can contain invalid char (like space in the end)
                textFieldValue = it.copy(text = it.text.filter(charValidator))
            }
        },
        modifier = Modifier
            .focusRequester(focusRequester)
            .fillMaxWidth(),
        inputTextSize = 18.sp,
        resultTextSize = 14.sp
    ) {
        if (it.userId.all(manager::isValidForUserId)) {
            onResult(it)
            onDismissRequest()
        } else {
            context.showToast("${manager.userIdTitle} contains unacceptable symbols")
        }
    }

    if (manager is ProfileSuggestionsProvider) {
        val userId by rememberUpdatedState(newValue = textFieldValue.text)
        var suggestionsResult: Result<List<UserSuggestion>> by remember { mutableStateOf(Result.success(emptyList())) }
        val loadingSuggestionsInProgressState = remember { mutableStateOf(false) }

        SuggestionsList(
            suggestionsResult = suggestionsResult,
            isLoading = loadingSuggestionsInProgressState.value,
            modifier = Modifier.fillMaxWidth(),
            onClick = { suggestion ->
                blockSuggestionsReload = true
                textFieldValue = suggestion.userId.toTextFieldValue()
            }
        )

        //TODO: rework this to flow
        LaunchedEffect(userId) {
            // if (userId.isBlank()) ???
            if (userId.length < suggestionsMinLength) {
                suggestionsResult = Result.success(emptyList())
                loadingSuggestionsInProgressState.value = false
                blockSuggestionsReload = false
                return@LaunchedEffect
            }
            delay(requestDebounceDelay)
            if (!blockSuggestionsReload) {
                loadingSuggestionsInProgressState.value = true
                val data = withContext(Dispatchers.Default) {
                    manager.runCatching { fetchSuggestions(userId) }
                }
                loadingSuggestionsInProgressState.value = false
                ensureActive()
                suggestionsResult = data
            } else {
                blockSuggestionsReload = false
            }
        }
    }
}


private data class ProfileLoadingResult<U: UserInfo>(
    val profile: ProfileResult<U>?,
    val isLoading: Boolean
)

@Composable
private fun <U: UserInfo> profileState(
    textState: State<TextFieldValue>,
    manager: AccountManager<U>,
    initial: ProfileResult<U>?
): State<ProfileLoadingResult<U>> =
    remember(textState, manager) {
        snapshotFlow { textState.value.text }
            .drop(1)
            .transformLatest { userId ->
                emit(ProfileLoadingResult<U>(null, false))
                if (userId.isBlank()) return@transformLatest
                delay(requestDebounceDelay)
                emit(ProfileLoadingResult<U>(null, true))
                emit(ProfileLoadingResult(manager.fetchProfile(userId), false))
            }
    }.collectAsState(initial = ProfileLoadingResult(initial, false))


@Composable
private fun<U: UserInfo> UserIdTextField(
    manager: AccountManager<U>,
    profileResult: ProfileResult<U>?,
    loadingInProgress: Boolean,
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier,
    inputTextSize: TextUnit,
    resultTextSize: TextUnit,
    onDoneRequest: (ProfileResult<U>) -> Unit
) {
    val onDoneRequestWithCheck = {
        if (profileResult != null && profileResult !is ProfileResult.NotFound && !loadingInProgress) {
            onDoneRequest(profileResult)
        }
    }
    TextField(
        value = textFieldValue,
        modifier = modifier,
        singleLine = true,
        textStyle = TextStyle(fontSize = inputTextSize, fontFamily = FontFamily.Monospace),
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = cpsColors.background,
            placeholderColor = cpsColors.contentAdditional
        ),
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = buildString {
                    append(manager.userIdTitle)
                    if (manager is ProfileSuggestionsProvider) append(" or search query")
                }
            )
        },
        label = {
            Text(
                text = manager.makeSpan(profileResult),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = resultTextSize
            )
        },
        trailingIcon = {
            TextFieldMainIcon(
                loadingInProgress = loadingInProgress,
                profileNotFound = profileResult == null || profileResult is ProfileResult.NotFound,
                iconSize = 32.dp,
                onDoneClick = onDoneRequestWithCheck
            )
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDoneRequestWithCheck() }),
    )
}

@Composable
private fun TextFieldMainIcon(
    loadingInProgress: Boolean,
    profileNotFound: Boolean,
    iconSize: Dp,
    onDoneClick: () -> Unit
) {
    if (loadingInProgress || !profileNotFound) {
        IconButton(
            onClick = onDoneClick,
            enabled = !loadingInProgress
        ) {
            if (loadingInProgress) {
                LoadingIndicator(
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
    suggestionsResult: Result<List<UserSuggestion>>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onClick: (UserSuggestion) -> Unit
) {
    val isError = suggestionsResult.isFailure
    val suggestions = suggestionsResult.getOrDefault(emptyList())
    if (suggestions.isNotEmpty() || isLoading || isError) {
        Column(modifier = modifier) {
            AccountChooserHeader(
                text = if (isError) "suggestions load failed" else "suggestions:",
                color = if (isError) cpsColors.error else cpsColors.contentAdditional
            ) {
                if (isLoading) {
                    LoadingIndicator(strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = CPSIcons.Search,
                        contentDescription = null
                    )
                }
            }
            LazyColumnWithScrollBar {
                items(suggestions, key = { it.userId }) {
                    SuggestionItem(
                        suggestion = it,
                        modifier = Modifier
                            .clickable { onClick(it) }
                            .padding(3.dp)
                    )
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    suggestion: UserSuggestion,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
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
    icon: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProvideContentColor(color = color) {
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .size(18.dp),
                content = { icon() }
            )
            Text(
                text = text,
                style = CPSDefaults.MonospaceTextStyle.copy(fontSize = 14.sp)
            )
        }
    }
}

@Composable
@ReadOnlyComposable
private fun <U: UserInfo> AccountManager<U>.makeSpan(profileResult: ProfileResult<U>?): AnnotatedString {
    if (profileResult == null) return AnnotatedString("")
    return buildAnnotatedString {
        withStyle(SpanStyle(color = cpsColors.content)) {
            when (profileResult) {
                is ProfileResult.Success<U> -> append(makeOKInfoSpan(profileResult.userInfo, cpsColors))
                is ProfileResult.NotFound -> append(text = "User not found", fontStyle = FontStyle.Italic)
                is ProfileResult.Failed -> append(text = "Loading failed", fontStyle = FontStyle.Italic)
            }
        }
    }
}


private fun String.toTextFieldValue() = TextFieldValue(text = this, selection = TextRange(length))
