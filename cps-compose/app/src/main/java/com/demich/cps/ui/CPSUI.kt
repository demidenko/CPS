package com.demich.cps.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Badge
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.DangerType
import com.demich.cps.utils.LoadingStatus
import com.demich.cps.utils.ProvideContentColor
import com.demich.cps.utils.colorFor
import com.demich.cps.utils.toSignedString

@Composable
fun IconSp(
    painter: Painter,
    modifier: Modifier = Modifier,
    size: TextUnit,
    color: Color = LocalContentColor.current
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
    color: Color = LocalContentColor.current
) {
    IconSp(
        painter = rememberVectorPainter(image = imageVector),
        modifier = modifier,
        size = size,
        color = color
    )
}

@Composable
fun AttentionIcon(
    dangerType: DangerType,
    modifier: Modifier = Modifier,
    size: TextUnit = 14.sp
) {
    IconSp(
        imageVector = CPSIcons.Attention,
        size = size,
        color = colorFor(dangerType),
        modifier = modifier
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
        animationSpec = CPSDefaults.toggleAnimationSpec(),
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
    iconSize: Dp = 26.dp,
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
            modifier = Modifier.size(iconSize)
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
            LoadingIndicator(
                modifier = Modifier.size(20.dp),
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
fun CPSSwitch(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Switch(
        modifier = modifier,
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = cpsColors.accent
        )
    )
}

@Composable
fun CPSCheckBox(
    modifier: Modifier = Modifier,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
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
    onCheckedChange: (Boolean) -> Unit
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
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 3.dp
) = CircularProgressIndicator(
    modifier = modifier,
    color = LocalContentColor.current,
    strokeWidth = strokeWidth
)

@Composable
fun ListTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = cpsColors.contentAdditional,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

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
inline fun EmptyMessageBox(
    modifier: Modifier = Modifier,
    crossinline content: @Composable () -> Unit
) {
    ProvideContentColor(cpsColors.contentAdditional) {
        ProvideTextStyle(value = TextStyle(fontWeight = FontWeight.Medium)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier
            ) {
                content()
            }
        }
    }
}

@Composable
fun CPSCountBadge(count: Int) {
    Badge(
        backgroundColor = cpsColors.newEntry,
        contentColor = cpsColors.background,
        content = { Text(count.toString()) }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
inline fun CPSSwipeRefreshBox(
    isRefreshing: () -> Boolean,
    noinline onRefresh: () -> Unit,
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
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        var hasOverflow by remember { mutableStateOf(false) }
        Text(
            text = words.joinToString(),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { hasOverflow = it.hasVisualOverflow }
        )
        if (hasOverflow) {
            Text(text = "(${words.size})")
        }
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