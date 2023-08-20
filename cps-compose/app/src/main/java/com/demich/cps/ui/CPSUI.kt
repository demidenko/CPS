package com.demich.cps.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.toSignedString

@Composable
fun IconSp(
    painter: Painter,
    modifier: Modifier = Modifier,
    size: TextUnit,
    color: Color = cpsColors.content
) {
    Icon(
        painter = painter,
        contentDescription = null,
        tint = color,
        modifier = modifier.size(with(LocalDensity.current) { size.toDp() })
    )
}

@Composable
fun IconSp(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    size: TextUnit,
    color: Color = cpsColors.content
) {
    IconSp(
        painter = rememberVectorPainter(image = imageVector),
        modifier = modifier,
        size = size,
        color = color
    )
}

@Composable
private fun CPSIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    color: Color,
    onState: Boolean = true
) {
    val alpha by animateFloatAsState(
        targetValue = if (onState) 1f else ContentAlpha.disabled,
        animationSpec = tween(CPSDefaults.buttonOnOffDurationMillis),
        label = "icon_alpha"
    )
    Icon(
        imageVector = icon,
        tint = color,
        contentDescription = null,
        modifier = modifier.alpha(alpha)
    )
}

@Composable
fun CPSIconButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    color: Color = cpsColors.content,
    enabled: Boolean = true,
    onState: Boolean = enabled,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        CPSIcon(
            icon = icon,
            color = color,
            onState = onState,
            modifier = Modifier.size(26.dp)
        )
    }
}


@Composable
fun CPSReloadingButton(
    loadingStatus: LoadingStatus,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        enabled = enabled && loadingStatus != LoadingStatus.LOADING,
        modifier = modifier,
        onClick = onClick
    ) {
        if (loadingStatus == LoadingStatus.LOADING) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = cpsColors.content,
                strokeWidth = 2.dp
            )
        } else {
            CPSIcon(
                icon = CPSIcons.Reload,
                color = if (loadingStatus == LoadingStatus.FAILED) cpsColors.error else cpsColors.content,
                onState = enabled,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun CPSCheckBox(
    modifier: Modifier = Modifier,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)? = null
) {
    Checkbox(
        modifier = modifier,
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
        colors = CheckboxDefaults.colors(
            checkedColor = cpsColors.accent
        )
    )
}

@Composable
fun CPSCheckBoxTitled(
    modifier: Modifier = Modifier,
    checked: Boolean,
    title: @Composable () -> Unit,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CPSCheckBox(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
        title()
    }
}

@Composable
fun CPSRadioButton(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: () -> Unit
) {
    RadioButton(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        colors = RadioButtonDefaults.colors(
            selectedColor = cpsColors.accent
        )
    )
}


@Composable
fun CPSRadioButtonTitled(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CPSRadioButton(selected = selected, onClick = onClick)
        title()
    }
}

@Composable
fun MonospacedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) = Text(
    text = text,
    modifier = modifier,
    color = color,
    fontSize = fontSize,
    textAlign = textAlign,
    maxLines = maxLines,
    overflow = overflow,
    style = CPSDefaults.MonospaceTextStyle
)


@Composable
fun<T> ButtonsSelectRow(
    values: List<T>,
    selectedValue: T,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit,
    content: @Composable (T) -> Unit
) {
    Row(modifier = modifier) {
        values.forEach { value ->
            TextButton(
                onClick = { onSelect(value) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (value == selectedValue) cpsColors.accent else cpsColors.contentAdditional
                ),
                content = { content(value) }
            )
        }
    }
}

@Composable
fun<T> TextButtonsSelectRow(
    values: List<T>,
    selectedValue: T,
    modifier: Modifier = Modifier,
    text: (T) -> String,
    onSelect: (T) -> Unit
) {
    ButtonsSelectRow(
        values = values,
        selectedValue = selectedValue,
        onSelect = onSelect,
        modifier = modifier
    ) {
        Text(text = text(it))
    }
}


@Composable
fun EmptyMessageBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        CompositionLocalProvider(LocalContentColor provides cpsColors.contentAdditional) {
            ProvideTextStyle(
                value = TextStyle(fontWeight = FontWeight.Medium),
                content = content
            )
        }
    }
}

@Composable
fun CPSCountBadge(count: Int) {
    require(count >= 0)
    AnimatedVisibility(
        visible = count > 0,
        enter = scaleIn(),
        exit = scaleOut()
    ) {
        Badge(
            backgroundColor = cpsColors.newEntry,
            contentColor = cpsColors.background,
            content = { Text(count.toString()) }
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CPSSwipeRefreshBox(
    isRefreshing: () -> Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val state = rememberPullRefreshState(
        refreshing = isRefreshing(),
        onRefresh = onRefresh
    )
    Box(
        modifier = modifier
            .pullRefresh(state)
            .clipToBounds()
    ) {
        content()
        PullRefreshIndicator(
            refreshing = isRefreshing(),
            state = state,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun WordsWithCounterOnOverflow(
    words: Collection<String>,
    modifier: Modifier = Modifier,
    fontSize: TextUnit,
    color: Color = cpsColors.contentAdditional
) {
    Row(modifier = modifier) {
        var counter by remember { mutableStateOf("") }
        Text(
            text = words.joinToString(),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            fontSize = fontSize,
            color = color,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = {
                counter = if (it.hasVisualOverflow) "(${words.size})" else ""
            }
        )
        Text(
            text = counter,
            maxLines = 1,
            fontSize = fontSize,
            color = color,
        )
    }
}

@Composable
fun VotedRating(
    rating: Int,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    showZero: Boolean = false
) {
    if (rating != 0 || showZero) {
        Text(
            text = rating.toSignedString(),
            color = cpsColors.votedRating(rating),
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            modifier = modifier
        )
    }
}