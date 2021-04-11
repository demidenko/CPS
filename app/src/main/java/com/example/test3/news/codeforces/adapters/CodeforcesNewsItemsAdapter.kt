package com.example.test3.news.codeforces.adapters

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.ui.FlowItemsAdapter
import kotlinx.coroutines.flow.Flow

abstract class CodeforcesNewsItemsAdapter<H: RecyclerView.ViewHolder, T>(
    fragment: Fragment,
    dataFlow: Flow<T>
): FlowItemsAdapter<H,T>(fragment, dataFlow) {

    protected val codeforcesAccountManager = CodeforcesAccountManager(fragment.requireContext())

}
