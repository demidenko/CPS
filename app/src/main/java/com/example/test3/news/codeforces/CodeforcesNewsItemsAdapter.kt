package com.example.test3.news.codeforces

import androidx.recyclerview.widget.RecyclerView
import com.example.test3.MainActivity
import com.example.test3.utils.MutableSetLiveSize

abstract class CodeforcesNewsItemsAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    abstract suspend fun parseData(s: String): Boolean

    protected lateinit var activity: MainActivity
    protected lateinit var recyclerView: RecyclerView
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        activity = recyclerView.context as MainActivity
        this.recyclerView = recyclerView
    }


    fun refresh(){
        beforeRefresh()
        notifyDataSetChanged()
    }
    open fun beforeRefresh() { }

    companion object {
        const val typeClassic = 0
        const val typeRecent = 1
        const val typeLost = 2
        fun getFromType(type: Int): CodeforcesNewsItemsAdapter {
            return when(type) {
                typeClassic -> CodeforcesNewsItemsClassicAdapter()
                typeRecent -> CodeforcesNewsItemsRecentAdapter()
                typeLost -> CodeforcesNewsItemsLostRecentAdapter()
                else -> throw Exception("Unknown type of CodeforcesNewsItemsAdapter: $type")
            }
        }
    }
}

interface CodeforcesNewsItemsAdapterManagesNewEntries {
    fun getBlogIDs(): List<Int>

    val newEntries: MutableSetLiveSize<Int>
    fun getNewEntriesCountLiveData() = newEntries.size
    fun addNewEntries(entries: Collection<Int>) = newEntries.addAll(entries)
    fun clearNewEntries() = newEntries.clear()
}

interface CodeforcesNewsItemsAdapterAutoUpdatable {
    fun subscribeLiveData(
        fragment: CodeforcesNewsFragment,
        dataReadyCallback: () -> Unit
    )
}