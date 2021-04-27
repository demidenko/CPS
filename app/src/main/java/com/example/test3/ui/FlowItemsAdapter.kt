package com.example.test3.ui

import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

abstract class FlowItemsAdapter<H: RecyclerView.ViewHolder, T>(
    fragment: Fragment,
    dataFlow: Flow<T>
): RecyclerView.Adapter<H>(), LifecycleOwner {
    private val lifecycleMerge by lazy { AdapterLifecycleMerge(this, fragment) }
    override fun getLifecycle() = lifecycleMerge.lifecycleRegistry

    private var previousValue: T? = null

    init {
        addRepeatingJob(Lifecycle.State.STARTED) {
            dataFlow.collect {
                if (it == previousValue) return@collect
                previousValue = it
                applyData(it)
                notifyDataSetChanged()
            }
        }
    }

    abstract suspend fun applyData(data: T)


    override fun registerAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        val before = hasObservers()
        super.registerAdapterDataObserver(observer)
        if(hasObservers() && !before) lifecycleMerge.setAdapterEvent(Lifecycle.Event.ON_START)
    }

    override fun unregisterAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        super.unregisterAdapterDataObserver(observer)
        if(!hasObservers()) lifecycleMerge.setAdapterEvent(Lifecycle.Event.ON_STOP)
    }

    private val holders = mutableListOf<H>()
    protected fun getActiveViewHolders(): List<H> = holders.toList()

    @CallSuper
    override fun onViewAttachedToWindow(holder: H) {
        holders.add(holder)
    }

    @CallSuper
    override fun onViewDetachedFromWindow(holder: H) {
        holders.remove(holder)
    }
}



class AdapterLifecycleMerge(
    adapter: FlowItemsAdapter<*, *>,
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
        fragment.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
            fun onAny(owner: LifecycleOwner, event: Lifecycle.Event) = setFragmentEvent(event)
        })
    }
}