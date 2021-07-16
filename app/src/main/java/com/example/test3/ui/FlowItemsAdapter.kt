package com.example.test3.ui

import androidx.annotation.CallSuper
import androidx.lifecycle.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

abstract class FlowItemsAdapter<H: RecyclerView.ViewHolder, T>(
    fragment: HideShowLifecycleFragment,
    dataFlow: Flow<T>
): RecyclerView.Adapter<H>(), LifecycleOwner {
    private val lifecycleMerge by lazy { LifecycleMerge(fragment.getHideShowLifecycleOwner()) }
    override fun getLifecycle() = lifecycleMerge.lifecycle

    private var previousValue: T? = null

    init {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dataFlow.collect {
                    if (it == previousValue) return@collect
                    previousValue = it
                    applyData(it)
                        ?.dispatchUpdatesTo(this@FlowItemsAdapter)
                        ?: notifyDataSetChanged()
                }
            }
        }
    }

    abstract suspend fun applyData(data: T): DiffUtil.DiffResult?


    override fun registerAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        val before = hasObservers()
        super.registerAdapterDataObserver(observer)
        if(hasObservers() && !before) lifecycleMerge.setAdditionalEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun unregisterAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        super.unregisterAdapterDataObserver(observer)
        if(!hasObservers()) lifecycleMerge.setAdditionalEvent(Lifecycle.Event.ON_STOP)
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


interface TimeDepends {
    var startTimeSeconds: Long
    fun refreshTime(currentTimeSeconds: Long)
}

class LifecycleMerge(
    originalLifecycleOwner: LifecycleOwner
): LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(originalLifecycleOwner)
    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    private var additionalEvent: Lifecycle.Event = Lifecycle.Event.ON_START
    private var mainEvent: Lifecycle.Event = Lifecycle.Event.ON_STOP
    fun setAdditionalEvent(event: Lifecycle.Event){
        additionalEvent = event
        handle()
    }
    fun setMainEvent(event: Lifecycle.Event){
        mainEvent = event
        handle()
    }
    private fun handle(){
        val event = maxOf(mainEvent, additionalEvent).takeIf { it>Lifecycle.Event.ON_RESUME } ?: minOf(mainEvent, additionalEvent)
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    init {
        originalLifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) = setMainEvent(event)
        })
    }
}