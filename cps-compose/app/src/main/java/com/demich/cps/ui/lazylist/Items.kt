package com.demich.cps.ui.lazylist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
inline fun LazyItemScope.ItemWithDivider(
    modifier: Modifier = Modifier,
    content: @Composable LazyItemScope.() -> Unit
) {
    Column(modifier = modifier) {
        content()
        Divider()
    }
}