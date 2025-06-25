package com.demich.cps.contests.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.contests.database.Contest
import com.demich.cps.platforms.api.clist.ClistClient
import com.demich.cps.platforms.api.clist.ClistResource
import com.demich.cps.platforms.clients.niceMessage
import com.demich.cps.platforms.utils.ClistUtils
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.ListTitle
import com.demich.cps.ui.LoadingContentBox
import com.demich.cps.ui.dialogs.CPSDialog
import com.demich.cps.ui.lazylist.LazyColumnWithScrollBar
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.BackgroundDataLoader
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import com.demich.cps.utils.randomUuid
import com.demich.cps.utils.rememberUUIDState
import com.demich.datastore_itemized.edit
import com.demich.kotlin_stdlib_boost.mapToSet
import kotlinx.coroutines.launch

@Composable
internal fun ClistAdditionalResourcesDialog(
    onDismissRequest: () -> Unit
) {
    CPSDialog(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismissRequest
    ) {
        DialogContent()
    }
}

@Composable
private fun ColumnScope.DialogContent() {
    val context = context
    val scope = rememberCoroutineScope()

    val dataLoader = remember(scope) { BackgroundDataLoader<List<ClistResource>>(scope = scope) }
    val resourcesResult by dataLoader.flowOfResult().collectAsState()

    var dataKey by rememberUUIDState
    LaunchedEffect(dataLoader, dataKey) {
        dataLoader.execute(id = dataKey) {
            val settings = context.settingsContests
            val alreadySupported = Contest.platformsExceptUnknown.mapToSet { ClistUtils.getClistApiResourceId(it) }
            ClistClient.getResources(apiAccess = settings.clistApiAccess())
                .filter { it.id !in alreadySupported }
                .sortedBy { it.name }
                .also { list ->
                    val res = list.associateBy { it.id }
                    settings.clistAdditionalResources.update {
                        it.mapNotNull { res[it.id] }
                    }
                }
        }
    }

    val item = remember { context.settingsContests.clistAdditionalResources }
    val selected by collectItemAsState { item }

    var searchFilter by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.weight(1f, fill = false),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ListTitle(text = "selected:")
        ClistResourcesList(
            resources = { filter(selected, searchFilter) },
            modifier = Modifier
                .padding(bottom = 3.dp)
                .heightIn(max = 200.dp),
            onItemClick = { resource ->
                scope.launch { item.edit { remove(resource) } }
            }
        )

        ListTitle(text = "available:")
        LoadingContentBox(
            dataResult = { resourcesResult?.map { it - selected.toSet() } },
            failedText = { it.niceMessage ?: "Failed to load resources" },
            onRetry = { dataKey = randomUuid() },
            modifier = Modifier
                .padding(bottom = 5.dp)
                .fillMaxWidth()
        ) { resources ->
            ClistResourcesList(
                resources = { filter(resources, searchFilter) },
                onItemClick = { resource ->
                    scope.launch { item.edit { add(index = 0, element = resource) } }
                }
            )
        }
    }

    OutlinedTextField(
        value = searchFilter,
        onValueChange = { searchFilter = it },
        leadingIcon = {
            Icon(
                imageVector = CPSIcons.Search,
                contentDescription = null,
                tint = cpsColors.content
            )
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun filter(resources: List<ClistResource>, searchFilter: String) =
    resources.filter { it.name.contains(searchFilter, ignoreCase = true) }


@Composable
private fun ClistResourcesList(
    resources: () -> List<ClistResource>,
    modifier: Modifier = Modifier,
    onItemClick: (ClistResource) -> Unit
) {
    LazyColumnWithScrollBar(modifier = modifier) {
        items(items = resources(), key = { it.id }) { resource ->
            Text(
                text = resource.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(resource) }
                    .padding(all = 2.dp)
                    .animateItem()
            )
            Divider(modifier = Modifier.animateItem())
        }
    }
}