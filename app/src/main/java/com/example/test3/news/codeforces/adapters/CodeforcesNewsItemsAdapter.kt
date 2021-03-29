package com.example.test3.news.codeforces.adapters

import androidx.recyclerview.widget.RecyclerView
import com.example.test3.account_manager.CodeforcesAccountManager

abstract class CodeforcesNewsItemsAdapter<H: RecyclerView.ViewHolder>: RecyclerView.Adapter<H>(){

    protected lateinit var recyclerView: RecyclerView
    protected lateinit var codeforcesAccountManager: CodeforcesAccountManager
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        codeforcesAccountManager = CodeforcesAccountManager(recyclerView.context)
    }


    fun refresh(){
        beforeRefresh()
        notifyDataSetChanged()
    }
    open fun beforeRefresh() { }

}
