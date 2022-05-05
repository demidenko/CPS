package com.demich.cps.contests.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.contests.Contest
import com.demich.cps.contests.loaders.ContestsLoaders
import com.demich.cps.ui.CPSDropdownMenu
import com.demich.cps.ui.CPSIcons
import com.demich.cps.utils.CPSDataStoreItem
import com.demich.cps.utils.rememberCollect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


@Composable
fun LoadersPriorityList(
    modifier: Modifier = Modifier,
    platform: Contest.Platform,
    availableOptions: Set<ContestsLoaders>,
    settingsItem: CPSDataStoreItem<Map<Contest.Platform,List<ContestsLoaders>>>
) {
    val scope = rememberCoroutineScope()
    val priorityList: List<ContestsLoaders> by rememberCollect {
        settingsItem.flow.map { it.getValue(platform) }
    }
    LoadersPriorityList(
        modifier = modifier,
        list = priorityList,
        availableOptions = availableOptions,
        onListChange = { newList ->
            scope.launch {
                val newMap = settingsItem().toMutableMap()
                newMap[platform] = newList
                settingsItem(newValue = newMap)
            }
        }
    )
}

@Composable
private fun LoadersPriorityList(
    modifier: Modifier = Modifier,
    list: List<ContestsLoaders>,
    availableOptions: Set<ContestsLoaders>,
    onListChange: (List<ContestsLoaders>) -> Unit
) {
    require(list.isNotEmpty())
    Column(modifier = modifier) {
        list.forEachIndexed { index, loaderType ->
            PriorityListItem(
                loaderType = loaderType,
                deleteEnabled = list.size > 1,
                onDeleteRequest = {
                    val newList = list.toMutableList()
                    newList.removeAt(index)
                    onListChange(newList)
                },
                availableOptions = availableOptions,
                onOptionSelected = { newType ->
                    require(newType != loaderType)
                    val newList = list.toMutableList()
                    newList.remove(newType)
                    newList[newList.indexOf(loaderType)] = newType
                    onListChange(newList)
                },
                modifier = Modifier.padding(all = 2.dp)
            )
        }
        if (!list.containsAll(availableOptions)) {
            PriorityListItemAdd(
                availableOptions = availableOptions - list,
                onOptionSelected = { loaderType ->
                    require(loaderType !in list)
                    onListChange(list + loaderType)
                }
            )
        }
    }
}

@Composable
private fun PriorityListItem(
    modifier: Modifier = Modifier,
    loaderType: ContestsLoaders,
    deleteEnabled: Boolean,
    onDeleteRequest: () -> Unit,
    availableOptions: Set<ContestsLoaders>,
    onOptionSelected: (ContestsLoaders) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Box(
        modifier = modifier.clickable { showMenu = true }
    ) {
        Text(text = loaderType.name)
        LoadersPopup(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            options = availableOptions - loaderType,
            deleteOption = deleteEnabled,
            onDeleteRequest = onDeleteRequest,
            onOptionSelected = onOptionSelected
        )
    }
}

@Composable
private fun PriorityListItemAdd(
    modifier: Modifier = Modifier,
    availableOptions: Set<ContestsLoaders>,
    onOptionSelected: (ContestsLoaders) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Box(
        modifier = modifier.clickable { showMenu = true }
    ) {
        Text(text = "+ add")
        LoadersPopup(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            options = availableOptions,
            deleteOption = false,
            onDeleteRequest = {},
            onOptionSelected = onOptionSelected
        )
    }
}

@Composable
private fun LoadersPopup(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    options: Set<ContestsLoaders>,
    deleteOption: Boolean,
    onDeleteRequest: () -> Unit,
    onOptionSelected: (ContestsLoaders) -> Unit
) {
    CPSDropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        options.forEach { option ->
            CPSDropdownMenuItem(
                title = option.name,
                icon = CPSIcons.Add,
                onClick = { onOptionSelected(option) }
            )
        }
        if (deleteOption) {
            CPSDropdownMenuItem(
                title = "delete",
                icon = CPSIcons.Delete,
                onClick = onDeleteRequest
            )
        }
    }
}
