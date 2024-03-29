package com.example.test3.ui

import androidx.annotation.CallSuper
import androidx.lifecycle.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

abstract class FlowItemsAdapter<H: RecyclerView.ViewHolder, T>(
    private val fragment: HideShowLifecycleFragment,
    private val dataFlow: Flow<T>
): RecyclerView.Adapter<H>(), LifecycleOwner {
    private val lifecycleMerge by lazy { LifecycleMerge(fragment.getHideShowLifecycleOwner()) }
    override fun getLifecycle() = lifecycleMerge.lifecycle

    private var started: Boolean = false
    fun startCollect() {
        if (started) return
        started = true
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dataFlow.distinctUntilChanged().collect {
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

var RecyclerView.flowAdapter: FlowItemsAdapter<*, *>?
    get() = adapter as? FlowItemsAdapter<*, *>
    set(value) {
        adapter = value?.apply { startCollect() }
    }

interface TimeDepends {
    var startTime: Instant
    fun refreshTime(currentTime: Instant)
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
    private fun setMainEvent(event: Lifecycle.Event){
        mainEvent = event
        handle()
    }
    private fun handle(){
        val event = maxOf(mainEvent, additionalEvent)
            .takeIf { it > Lifecycle.Event.ON_RESUME }
            ?: minOf(mainEvent, additionalEvent)
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    init {
        originalLifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) = setMainEvent(event)
        })
    }
}