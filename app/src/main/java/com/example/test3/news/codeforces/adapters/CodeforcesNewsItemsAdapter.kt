package com.example.test3.news.codeforces.adapters

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.account_manager.CodeforcesAccountManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

abstract class CodeforcesNewsItemsAdapter<H: RecyclerView.ViewHolder, D>(
    lifecycleCoroutineScope: LifecycleCoroutineScope,
    dataFlow: Flow<D>
): RecyclerView.Adapter<H>(){

    protected lateinit var recyclerView: RecyclerView
    protected lateinit var codeforcesAccountManager: CodeforcesAccountManager
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        codeforcesAccountManager = CodeforcesAccountManager(recyclerView.context)
    }

    init {
        lifecycleCoroutineScope.launchWhenStarted {
            dataFlow.collect {
                applyData(it)
                notifyDataSetChanged()
            }
        }
    }

    abstract suspend fun applyData(data: D)

    fun refresh(){
        beforeRefresh()
        notifyDataSetChanged()
    }
    open fun beforeRefresh() { }

}
