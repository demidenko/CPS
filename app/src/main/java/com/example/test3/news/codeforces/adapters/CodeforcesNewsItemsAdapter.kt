package com.example.test3.news.codeforces.adapters

import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.ui.FlowItemsAdapter
import com.example.test3.ui.HideShowLifecycleFragment
import com.example.test3.ui.TimeDepends
import com.example.test3.utils.getCurrentTimeSeconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

abstract class CodeforcesNewsItemsAdapter<H : RecyclerView.ViewHolder, T>(
    fragment: HideShowLifecycleFragment,
    dataFlow: Flow<T>
): FlowItemsAdapter<H, T>(fragment, dataFlow) {

    protected val codeforcesAccountManager = CodeforcesAccountManager(fragment.requireContext())

    protected abstract fun refreshHandles(holder: H, position: Int)

    fun refreshHandles() {
        getActiveViewHolders().forEach { holder ->
            val position = holder.bindingAdapterPosition
            if(position != RecyclerView.NO_POSITION) refreshHandles(holder, position)
        }
    }

    @CallSuper
    override fun onBindViewHolder(holder: H, position: Int, payloads: MutableList<Any>) {
        if(payloads.isEmpty()) super.onBindViewHolder(holder, position, payloads)
    }

}

abstract class CodeforcesNewsItemsTimedAdapter<H, T>(
    fragment: HideShowLifecycleFragment,
    dataFlow: Flow<T>
): CodeforcesNewsItemsAdapter<H, T>(fragment, dataFlow)
    where H: RecyclerView.ViewHolder, H: TimeDepends
{
    init {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    getActiveViewHolders().takeIf { it.isNotEmpty() }?.let { holders ->
                        val currentTimeSeconds = getCurrentTimeSeconds()
                        holders.forEach { it.refreshTime(currentTimeSeconds) }
                    }
                    delay(1000)
                }
            }
        }
    }
}
