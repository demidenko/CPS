package com.example.test3.news.codeforces.adapters

import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.ui.FlowItemsAdapter
import com.example.test3.utils.getCurrentTimeSeconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive

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

abstract class CodeforcesNewsItemsTimedAdapter<H, T>(
    fragment: Fragment,
    dataFlow: Flow<T>
): CodeforcesNewsItemsAdapter<H, T>(fragment, dataFlow)
    where H: RecyclerView.ViewHolder, H: TimeDepends
{
    init {
        addRepeatingJob(Lifecycle.State.RESUMED) {
            while (isActive) {
                with(getActiveViewHolders()){
                    if(isNotEmpty()){
                        val currentTimeSeconds = getCurrentTimeSeconds()
                        forEach { holder -> holder.refreshTime(currentTimeSeconds) }
                    }
                }
                delay(1000)
            }
        }
    }
}

interface TimeDepends {
    var startTimeSeconds: Long
    fun refreshTime(currentTimeSeconds: Long)
}