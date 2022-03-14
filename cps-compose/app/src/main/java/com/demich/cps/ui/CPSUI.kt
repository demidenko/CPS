package com.demich.cps.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.demich.cps.ui.theme.cpsColors

@Composable
fun CPSIconButton(
    icon: ImageVector,
    onState: Boolean = true,
    enabledState: Boolean = true,
    onClick: () -> Unit
) {
    val a by animateFloatAsState(if (onState) 1f else ContentAlpha.disabled, tween(800))
    IconButton(
        onClick = onClick,
        enabled = enabledState
    ) {
        Icon(
            imageVector = icon,
            tint = cpsColors.textColor,
            contentDescription = null,
            modifier = Modifier
                .size(26.dp)
                .alpha(a)
        )
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
        onCheckedChange = onCheckedChange
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
    maxLines = maxLines
)


@Composable
fun CounterButton(
    text: String
) {
    var counter by rememberSaveable { mutableStateOf(0) }
    Button(
        onClick = {
            counter++
        }
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
            modifier = Modifier.wrapContentHeight().padding(start = 26.dp, end = 26.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(all = 18.dp).fillMaxWidth().wrapContentHeight(),
                horizontalAlignment = horizontalAlignment,
                content = content
            )
        }
    }
}