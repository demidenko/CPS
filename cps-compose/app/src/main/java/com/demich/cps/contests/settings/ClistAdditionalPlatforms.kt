package com.demich.cps.contests.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demich.cps.contests.Contest
import com.demich.cps.ui.CPSDialog
import com.demich.cps.ui.LazyColumnWithScrollBar
import com.demich.cps.ui.SettingsItemWithInfo
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.*
import kotlinx.coroutines.launch

@Composable
fun ClistAdditionalPlatformsSettingsItem(
    item: CPSDataStoreItem<List<ClistResource>>
) {
    var showDialog by remember { mutableStateOf(false) }
    SettingsItemWithInfo(
        modifier = Modifier.clickable { showDialog = true },
        item = item,
        title = "Clist additional platforms"
    ) { resources ->
        Text(
            text = resources.joinToString { it.name },
            fontSize = 15.sp,
            color = cpsColors.textColorAdditional,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    if (showDialog) {
        ClistAdditionalResourcesDialog(
            item = item,
            onDismissRequest = { showDialog = false }
        )
    }
}

@Composable
private fun ClistAdditionalResourcesDialog(
    item: CPSDataStoreItem<List<ClistResource>>,
    onDismissRequest: () -> Unit
) {
    val context = context
    val scope = rememberCoroutineScope()

    var loadingStatus by remember { mutableStateOf(LoadingStatus.PENDING) }
    var unselectedItems: List<ClistResource> by remember { mutableStateOf(emptyList()) }

    val selectedItems by rememberCollect { item.flow }

    LaunchedEffect(Unit) {
        loadingStatus = LoadingStatus.LOADING
        kotlin.runCatching {
            CListApi.getResources(apiAccess = context.settingsContests.clistApiAccess())
        }.onFailure {
            loadingStatus = LoadingStatus.FAILED
        }.onSuccess { resources ->
            val alreadySupported =
                Contest.platformsExceptUnknown.map { CListUtils.getClistApiResourceId(it) }
            unselectedItems = resources
                .filter { it.id !in alreadySupported }
                .sortedBy { it.name }
            loadingStatus = LoadingStatus.PENDING
        }
    }

    CPSDialog(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp),
        onDismissRequest = onDismissRequest
    ) {
        LazyColumnWithScrollBar(
            modifier = Modifier.weight(1f)
        ) {
            items(items = selectedItems, key = { it.id }) { resource ->
                Text(
                    text = resource.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { item.remove(resource) }
                        }
                        .padding(all = 2.dp)
                )
                Divider()
            }
        }

        LazyColumnWithScrollBar(
            modifier = Modifier.weight(1f).padding(top = 4.dp)
        ) {
            items(items = unselectedItems, key = { it.id }) { resource ->
                Text(
                    text = resource.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = resource !in selectedItems) {
                            scope.launch { item.add(resource) }
                        }
                        .padding(all = 2.dp)
                )
                Divider()
            }
        }
    }
}