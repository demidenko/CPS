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
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.R
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.account_manager.CodeforcesUserInfo
import com.example.test3.account_manager.STATUS
import com.example.test3.news.codeforces.CodeforcesBlogEntriesFragment
import com.example.test3.ui.CPSFragment
import com.example.test3.ui.settingsUI
import com.example.test3.utils.CodeforcesUtils
import com.example.test3.utils.disable
import com.example.test3.utils.enable
import com.example.test3.utils.ignoreFirst
import com.example.test3.workers.CodeforcesNewsFollowWorker
import kotlinx.coroutines.flow.collect
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


        val followListAdapter = FollowListItemsAdapter(this).apply {
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
            isVisible = false
        }
        buttonAdd.setOnClickListener {
            buttonAdd.disable()
            lifecycleScope.launch {
                mainActivity.chooseUserID(CodeforcesAccountManager(mainActivity))?.let { userInfo ->
                    followListAdapter.add(userInfo as CodeforcesUserInfo)
                    followRecyclerView.scrollToPosition(0)
                }
                buttonAdd.enable()
            }
        }

        lifecycleScope.launch {
            followRecyclerView.isEnabled = false
            followListAdapter.initialize()
            followRecyclerView.isEnabled = true
            buttonAdd.isVisible = true
        }

        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.STARTED){
            mainActivity.settingsUI.getUseRealColorsFlow().ignoreFirst().collect { use ->
                followListAdapter.notifyDataSetChanged()
            }
        }
    }


    class FollowListItemsAdapter(val fragment: CPSFragment) : RecyclerView.Adapter<FollowListItemsAdapter.ItemHolder>() {

        private val codeforcesAccountManager = CodeforcesAccountManager(fragment.requireContext())

        private val list = mutableListOf<CodeforcesUserInfo>()
        override fun getItemCount(): Int = list.size

        private val dataConnector = CodeforcesNewsFollowWorker.FollowDataConnector(fragment.requireContext())

        private var openBlogEntriesCallback: ((CodeforcesUserInfo) -> Unit)? = null
        fun setOnSelectListener(callback: (CodeforcesUserInfo) -> Unit) {
            openBlogEntriesCallback = callback
        }

        suspend fun initialize(){
            val usersInfo = CodeforcesUtils.getUsersInfo(dataConnector.getHandles(), true)
                .mapNotNull { (handle, info) ->
                    if(info.status == STATUS.NOT_FOUND){
                        dataConnector.remove(handle)
                        null
                    } else {
                        if(info.status == STATUS.OK && info.handle != handle){
                            dataConnector.changeHandle(handle, info.handle)
                            info.handle to info
                        } else {
                            handle to info
                        }
                    }
                }.toMap()

            dataConnector.getHandles().mapNotNull { handle ->
                usersInfo[handle]?.takeIf { it.status != STATUS.NOT_FOUND }
            }.let { infos ->
                list.addAll(infos)
            }

            notifyItemRangeInserted(0, list.size)
        }

        suspend fun add(userInfo: CodeforcesUserInfo){
            if(!dataConnector.add(userInfo.handle)){
                fragment.mainActivity.showToast("User already in list")
                return
            }
            list.add(0, userInfo)
            notifyItemInserted(0)
        }

        fun remove(userInfo: CodeforcesUserInfo){
            val index = list.indexOf(userInfo).takeIf { it!=-1 } ?: return

            runBlocking {
                dataConnector.remove(userInfo.handle)
            }
            list.removeAt(index)
            notifyItemRemoved(index)
        }

        class ItemHolder(val view: ConstraintLayout) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.manage_cf_follow_users_list_item_title)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.manage_cf_follow_list_item, parent, false) as ConstraintLayout
            return ItemHolder(view)
        }

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            val userInfo = list[position]
            holder.title.text = codeforcesAccountManager.makeSpan(userInfo)

            holder.view.setOnClickListener {
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


    }
}