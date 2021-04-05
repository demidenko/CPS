package com.example.test3.news.codeforces.adapters

import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.account_manager.CodeforcesAccountManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

abstract class CodeforcesNewsItemsAdapter<H: RecyclerView.ViewHolder, T>(
    fragment: Fragment,
    dataFlow: Flow<T>
): RecyclerView.Adapter<H>(), LifecycleOwner{

    protected val codeforcesAccountManager = CodeforcesAccountManager(fragment.requireContext())

    private val lifecycleMerge = AdapterLifecycleMerge(this, fragment)
    override fun getLifecycle() = lifecycleMerge.lifecycleRegistry

    init {
        addRepeatingJob(Lifecycle.State.STARTED) {
            dataFlow.collect {
                applyData(it)
                notifyDataSetChanged()
            }
        }
    }

    abstract suspend fun applyData(data: T)

    fun refresh(){
        beforeRefresh()
        notifyDataSetChanged()
    }
    protected open fun beforeRefresh() { }


    override fun registerAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        val before = hasObservers()
        super.registerAdapterDataObserver(observer)
        if(hasObservers() && !before) lifecycleMerge.setAdapterEvent(Lifecycle.Event.ON_START)
    }

    override fun unregisterAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        super.unregisterAdapterDataObserver(observer)
        if(!hasObservers()) lifecycleMerge.setAdapterEvent(Lifecycle.Event.ON_STOP)
    }

}


class AdapterLifecycleMerge(
    adapter: CodeforcesNewsItemsAdapter<*,*>,
    fragment: Fragment
){
    val lifecycleRegistry = LifecycleRegistry(adapter)
    private var adapterEvent: Lifecycle.Event = Lifecycle.Event.ON_STOP
    private var fragmentEvent: Lifecycle.Event = Lifecycle.Event.ON_STOP
    fun setAdapterEvent(event: Lifecycle.Event){
        adapterEvent = event
        handle()
    }
    fun setFragmentEvent(event: Lifecycle.Event){
        fragmentEvent = event
        handle()
    }
    private fun handle(){
        val event = maxOf(adapterEvent,fragmentEvent)
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    init {
        fragment.lifecycle.addObserver(object : LifecycleObserver{
            @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
            fun onAny(owner: LifecycleOwner, event: Lifecycle.Event) = setFragmentEvent(event)
        })
    }
}