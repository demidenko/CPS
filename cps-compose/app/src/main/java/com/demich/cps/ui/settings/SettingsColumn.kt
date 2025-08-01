package com.demich.cps.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.ui.CPSFontSize
import com.demich.cps.ui.IconSp
import com.demich.cps.ui.NotificationsPermissionPanel
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.ProvideContentColor

@Composable
inline fun SettingsColumn(
    modifier: Modifier = Modifier,
    content: @Composable SettingsContainerScope.() -> Unit
) {
    val border: Dp = 10.dp
    //spaceBy adds space only between items but start + end required too
    ColumnSpaced(
        space = border,
        modifier = modifier
            .padding(horizontal = border)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.fillMaxWidth())
        SettingsColumnScopeInstance.content()
        Spacer(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
inline fun SettingsColumn(
    requiredNotificationsPermission: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable SettingsContainerScope.() -> Unit
) {
    Column(modifier = modifier) {
        SettingsColumn(content = content, modifier = Modifier.weight(1f))
        NotificationsPermissionPanel(permissionRequired = requiredNotificationsPermission)
    }
}

@Composable
inline fun SettingsContainerScope.SettingsSectionHeader(
    title: String,
    painter: Painter,
    modifier: Modifier = Modifier,
    content: SettingsContainerScope.() -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProvideContentColor(color = cpsColors.accent) {
            IconSp(
                painter = painter,
                size = 18.sp,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            Text(
                text = title,
                fontSize = CPSFontSize.settingsSectionTitle,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
    content()
}

object SettingsColumnScopeInstance: SettingsContainerScope {
    @Composable
    override fun append(
        modifier: Modifier,
        content: @Composable () -> Unit
    ) {
        Box(
            modifier = modifier
                .background(cpsColors.backgroundAdditional, RoundedCornerShape(4.dp))
                .fillMaxWidth()
                .padding(all = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            content()
        }
    }
}