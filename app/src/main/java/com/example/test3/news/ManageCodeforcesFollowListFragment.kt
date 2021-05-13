package com.example.test3.news

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.R
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.account_manager.CodeforcesUserInfo
import com.example.test3.news.codeforces.CodeforcesBlogEntriesFragment
import com.example.test3.news.codeforces.adapters.CodeforcesNewsItemsAdapter
import com.example.test3.room.CodeforcesUserBlog
import com.example.test3.room.getFollowDao
import com.example.test3.ui.CPSFragment
import com.example.test3.ui.settingsUI
import com.example.test3.utils.disable
import com.example.test3.utils.enable
import com.example.test3.utils.ignoreFirst
import com.example.test3.workers.CodeforcesNewsFollowWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ManageCodeforcesFollowListFragment: CPSFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_manage_cf_follow, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cpsTitle = "::news.codeforces.follow.list"
        setBottomPanelId(R.id.support_navigation_cf_follow, R.layout.navigation_cf_follow)

        setHasOptionsMenu(true)

        val followRecyclerView = view.findViewById<RecyclerView>(R.id.manage_cf_follow_users_list).apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            setHasFixedSize(true)
        }


        val followListAdapter = FollowListItemsAdapter(
            this,
            getFollowDao(requireContext()).flowOfAll().map { blogs ->
                blogs.sortedByDescending { it.id }
            }
        ).apply {
            setOnSelectListener { info ->
                mainActivity.cpsFragmentManager.pushBack(
                    CodeforcesBlogEntriesFragment().apply {
                        setHandle(info.handle)
                    }
                )
            }
        }

        followRecyclerView.adapter = followListAdapter

        val buttonAdd = requireBottomPanel().findViewById<ImageButton>(R.id.navigation_cf_follow_add).apply {
            disable()
        }
        buttonAdd.setOnClickListener {
            buttonAdd.disable()
            lifecycleScope.launch {
                mainActivity.chooseUserID(CodeforcesAccountManager(mainActivity))?.let { userInfo ->
                    val dataConnector = CodeforcesNewsFollowWorker.FollowDataConnector(requireContext())
                    if(!dataConnector.add(userInfo)){
                        mainActivity.showToast("User already in list")
                        return@let
                    }
                    followRecyclerView.scrollToPosition(0)
                }
                buttonAdd.enable()
            }
        }

        lifecycleScope.launch {
            followRecyclerView.isEnabled = false
            CodeforcesNewsFollowWorker.FollowDataConnector(requireContext()).updateUsersInfo()
            followRecyclerView.isEnabled = true
            buttonAdd.enable()
        }

        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.STARTED){
            mainActivity.settingsUI.getUseRealColorsFlow().ignoreFirst().collect { followListAdapter.refreshHandles() }
        }
    }

    class FollowListItemsAdapter(
        val fragment: CPSFragment,
        dataFlow: Flow<List<CodeforcesUserBlog>>
    ): CodeforcesNewsItemsAdapter<FollowListItemsAdapter.UserBlogInfoViewHolder,List<CodeforcesUserBlog>>(fragment, dataFlow) {

        private var items: Array<CodeforcesUserBlog> = emptyArray()
        override fun getItemCount(): Int = items.size

        private val dataConnector = CodeforcesNewsFollowWorker.FollowDataConnector(fragment.requireContext())

        private var openBlogEntriesCallback: ((CodeforcesUserInfo) -> Unit)? = null
        fun setOnSelectListener(callback: (CodeforcesUserInfo) -> Unit) {
            openBlogEntriesCallback = callback
        }

        fun remove(userInfo: CodeforcesUserInfo){
            runBlocking {
                dataConnector.remove(userInfo.handle)
            }
        }

        class UserBlogInfoViewHolder(val view: ConstraintLayout) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.manage_cf_follow_users_list_item_title)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserBlogInfoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.manage_cf_follow_list_item, parent, false) as ConstraintLayout
            return UserBlogInfoViewHolder(view)
        }

        override fun onBindViewHolder(holder: UserBlogInfoViewHolder, position: Int) {
            setHandle(holder, position)
            holder.view.setOnClickListener {
                val userInfo = items[holder.bindingAdapterPosition].userInfo
                PopupMenu(it.context, holder.title, Gravity.CENTER_HORIZONTAL).apply {
                    inflate(R.menu.popup_cf_follow_list_item)

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        setForceShowIcon(true)
                    }

                    setOnMenuItemClickListener {
                        when(it.itemId){
                            R.id.cf_follow_list_item_open_blogs -> {
                                openBlogEntriesCallback?.invoke(userInfo)
                                true
                            }
                            R.id.cf_follow_list_item_delete -> {
                                remove(userInfo)
                                true
                            }
                            else -> false
                        }
                    }

                    show()
                }
            }
        }

        private fun setHandle(holder: UserBlogInfoViewHolder, position: Int) {
            with(items[position]){
                holder.title.text = codeforcesAccountManager.makeSpan(userInfo.copy(handle = handle))
            }
        }

        override fun refreshHandles(holder: UserBlogInfoViewHolder, position: Int) = setHandle(holder, position)

        override suspend fun applyData(data: List<CodeforcesUserBlog>): DiffUtil.DiffResult {
            val newItems = data.toTypedArray()
            return DiffUtil.calculateDiff(diffCallback(items, newItems)).also {
                items = newItems
            }
        }

        companion object {
            private fun diffCallback(old: Array<CodeforcesUserBlog>, new: Array<CodeforcesUserBlog>) =
                object : DiffUtil.Callback() {
                    override fun getOldListSize() = old.size
                    override fun getNewListSize() = new.size

                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        return old[oldItemPosition].id == new[newItemPosition].id
                    }

                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        return old[oldItemPosition] == new[newItemPosition]
                    }

                }
        }
    }
}