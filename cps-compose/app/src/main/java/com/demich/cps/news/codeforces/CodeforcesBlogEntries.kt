package com.demich.cps.news.codeforces

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.demich.cps.utils.codeforces.CodeforcesBlogEntry

@Composable
fun CodeforcesBlogEntries(
    blogEntries: List<CodeforcesBlogEntry>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
    ) {
        items(items = blogEntries, key = { it.id }) {
            BlogEntryInfo(
                blogEntry = it,
                modifier = Modifier
                    .padding(all = 2.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BlogEntryInfo(
    blogEntry: CodeforcesBlogEntry,
    modifier: Modifier = Modifier
) {
    Text(text = blogEntry.toString(), modifier = modifier)
}