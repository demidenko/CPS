package com.demich.cps.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.LoadingStatus

const val buttonOnOffDurationMillis: Int = 800

@Composable
fun CPSIconButton(
    icon: ImageVector,
    color: Color = cpsColors.textColor,
    onState: Boolean = true,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (onState) 1f else ContentAlpha.disabled,
        animationSpec = tween(buttonOnOffDurationMillis)
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
                color = cpsColors.textColor,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                tint = if (loadingStatus == LoadingStatus.FAILED) cpsColors.errorColor else cpsColors.textColor,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
            )
        }
    }
}

@Composable
fun CPSCheckBox(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    Checkbox(
        checked = checked,
        modifier = modifier,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
        colors = CheckboxDefaults.colors(
            checkedColor = cpsColors.colorAccent
        )
    )
}

@Composable
fun MonospacedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE
) = Text(
    text = text,
    modifier = modifier,
    color = color,
    fontSize = fontSize,
    fontFamily = FontFamily.Monospace,
    maxLines = maxLines,
    letterSpacing = 0.sp
)


@Composable
fun CounterButton(text: String) {
    var counter by rememberSaveable { mutableStateOf(0) }
    Button(
        onClick = { counter++ }
    ) {
        Text("$text: $counter")
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CPSDialog(
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(12.dp),
            backgroundColor = cpsColors.backgroundAdditional,
            modifier = Modifier.padding(start = 26.dp, end = 26.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(all = 18.dp),
                horizontalAlignment = horizontalAlignment,
                content = content
            )
        }
    }
}