package com.demich.cps.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LoadingStatus

@Composable
fun IconSp(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    size: TextUnit,
    color: Color = cpsColors.content
) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = color,
        modifier = modifier.size(with(LocalDensity.current) { size.toDp() })
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
    val alpha by animateFloatAsState(
        targetValue = if (onState) 1f else ContentAlpha.disabled,
        animationSpec = tween(CPSDefaults.buttonOnOffDurationMillis)
    )
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            tint = color,
            contentDescription = null,
            modifier = Modifier
                .size(26.dp)
                .alpha(alpha)
        )
    }
}


@Composable
fun CPSReloadingButton(
    loadingStatus: LoadingStatus,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IconButton(
        enabled = loadingStatus != LoadingStatus.LOADING,
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
            Icon(
                imageVector = CPSIcons.Reload,
                tint = if (loadingStatus == LoadingStatus.FAILED) cpsColors.error else cpsColors.content,
                contentDescription = null,
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
    onCheckedChange: ((Boolean) -> Unit)? = null,
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
    fontFamily = FontFamily.Monospace,
    textAlign = textAlign,
    maxLines = maxLines,
    overflow = overflow,
    letterSpacing = 0.sp
)


@Composable
fun<T> TextButtonsSelectRow(
    values: List<T>,
    selectedValue: T,
    modifier: Modifier = Modifier,
    text: (T) -> String,
    onSelect: (T) -> Unit
) {
    Row(modifier = modifier) {
        values.forEach { value ->
            TextButton(onClick = { onSelect(value) }) {
                Text(
                    text = text(value),
                    color = if (value == selectedValue) cpsColors.accent else cpsColors.contentAdditional
                )
            }
        }
    }
}

@Composable
fun EmptyListMessageBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "List is empty",
            color = cpsColors.contentAdditional,
            fontWeight = FontWeight.Medium
        )
    }
}

inline fun <T> LazyListScope.itemsNotEmpty(
    items: List<T>,
    noinline onEmptyMessage: @Composable () -> Unit = { Text(text = "List is empty") },
    noinline key: ((item: T) -> Any)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
) {
    if (items.isEmpty()) {
        item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillParentMaxSize()
            ) {
                CompositionLocalProvider(LocalContentColor provides cpsColors.contentAdditional) {
                    ProvideTextStyle(
                        value = TextStyle(fontWeight = FontWeight.Medium),
                        content = onEmptyMessage
                    )
                }
            }
        }
    } else {
        items(
            items = items,
            key = key,
            contentType = contentType,
            itemContent = itemContent
        )
    }
}

@Composable
fun CPSCountBadge(count: Int) {
    require(count >= 0)
    if (count > 0) {
        Badge(
            backgroundColor = cpsColors.newEntry,
            contentColor = cpsColors.background
        ) {
            Text(count.toString())
        }
    }
}