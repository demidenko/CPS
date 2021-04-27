package com.example.test3.news.codeforces.adapters

import androidx.annotation.CallSuper
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


    protected abstract fun refreshHandles(holder: H, position: Int)

    fun refreshHandles() {
        getActiveViewHolders().forEach { holder ->
            refreshHandles(holder, holder.bindingAdapterPosition)
        }
    }

    @CallSuper
    override fun onBindViewHolder(holder: H, position: Int, payloads: MutableList<Any>) {
        if(payloads.isEmpty()) super.onBindViewHolder(holder, position, payloads)
    }

}
