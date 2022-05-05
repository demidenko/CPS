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
import com.demich.cps.contests.loaders.ContestsLoaders
import com.demich.cps.ui.CPSDropdownMenu
import com.demich.cps.ui.CPSIcons

@Composable
fun LoadersPriorityList(
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
        CPSDropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            (availableOptions - loaderType).forEach { option ->
                CPSDropdownMenuItem(
                    title = option.name,
                    icon = CPSIcons.Add,
                    onClick = { onOptionSelected(option) }
                )
            }
            if (deleteEnabled) {
                CPSDropdownMenuItem(
                    title = "delete",
                    icon = CPSIcons.Delete,
                    onClick = onDeleteRequest
                )
            }
        }
    }
}


