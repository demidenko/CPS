package com.demich.cps.contests.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.demich.cps.platforms.api.clist.ClistApi
import com.demich.cps.platforms.api.clist.ClistResource
import com.demich.cps.platforms.clients.ClistClient
import com.demich.cps.platforms.clients.niceMessage
import com.demich.cps.platforms.utils.clistResourceId
import com.demich.cps.ui.CPSIcons
import com.demich.cps.ui.ListTitle
import com.demich.cps.ui.LoadingContentBox
import com.demich.cps.ui.dialogs.CPSDialog
import com.demich.cps.ui.lazylist.ItemWithDivider
import com.demich.cps.ui.lazylist.LazyColumnWithScrollBar
import com.demich.cps.ui.theme.cpsColors
import com.demich.cps.utils.backgroundDataLoader
import com.demich.cps.utils.collectItemAsState
import com.demich.cps.utils.context
import com.demich.cps.utils.randomUuid
import com.demich.cps.utils.rememberUUIDState
import com.demich.datastore_itemized.DataStoreItem
import com.demich.datastore_itemized.edit
import com.demich.kotlin_stdlib_boost.mapToSet
import com.sebaslogen.resaca.viewModelScoped
import kotlinx.coroutines.launch

@Composable
internal fun ClistAdditionalResourcesDialog(
    item: DataStoreItem<List<ClistResource>>,
    onDismissRequest: () -> Unit
) {
    val context = context
    val selected by collectItemAsState { item }

    val viewModel = viewModelScoped { CListResourcesLoadingViewModel() }
    var dataKey by rememberUUIDState()

    val fetchResult by viewModel
        .flowOfResourcesResult(settings = context.settingsContests, key = dataKey)
        .collectAsState()

    val scope = rememberCoroutineScope()

    CPSDialog(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismissRequest
    ) {
        DialogContent(
            fetchResult = { fetchResult },
            onFetchRetry = { dataKey = randomUuid() },
            selected = { selected },
            onSelectResource = {
                scope.launch { item.edit { add(index = 0, element = it) } }
            },
            onUnselectResource = {
                scope.launch { item.edit { remove(it) } }
            }
        )
    }
}

@Composable
private fun ColumnScope.DialogContent(
    fetchResult: () -> Result<List<ClistResource>>?,
    onFetchRetry: () -> Unit,
    selected: () -> List<ClistResource>,
    onSelectResource: (ClistResource) -> Unit,
    onUnselectResource: (ClistResource) -> Unit
) {
    var searchFilter by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.weight(1f, fill = false),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ListTitle(text = "selected:")
        ClistResourcesList(
            resources = { filter(selected(), searchFilter) },
            modifier = Modifier
                .padding(bottom = 3.dp)
                .heightIn(max = 200.dp),
            onItemClick = onUnselectResource
        )

        ListTitle(text = "available:")
        LoadingContentBox(
            dataResult = { fetchResult()?.map { it - selected().toSet() } },
            failedText = { it.niceMessage ?: "Failed to load resources" },
            onRetry = onFetchRetry,
            modifier = Modifier
                .padding(bottom = 5.dp)
                .fillMaxWidth()
        ) { resources ->
            ClistResourcesList(
                resources = { filter(resources, searchFilter) },
                onItemClick = onSelectResource
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
            ItemWithDivider(modifier = Modifier.animateItem()) {
                Text(
                    text = resource.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(resource) }
                        .padding(all = 2.dp)
                )
            }
        }
    }
}

private class CListResourcesLoadingViewModel: ViewModel() {
    private val loader = backgroundDataLoader<List<ClistResource>>()

    fun flowOfResourcesResult(settings: ContestsSettingsDataStore, key: Any) =
        loader.execute(key = key) {
            ClistClient.getResourcesSyncWithSettings(
                clistApiAccess = settings.clistApiAccess(),
                item = settings.clistAdditionalResources
            )
        }
}

private suspend fun ClistApi.getResourcesSyncWithSettings(
    clistApiAccess: ClistApi.ApiAccess,
    item: DataStoreItem<List<ClistResource>>
): List<ClistResource> {
    val mainIds = contestPlatforms.mapToSet { it.clistResourceId }
    return getResources(apiAccess = clistApiAccess)
        .filter { it.id !in mainIds }
        .sortedBy { it.name }
        .also { list ->
            val res = list.associateBy { it.id }
            item.update {
                it.mapNotNull { res[it.id] }
            }
        }
}