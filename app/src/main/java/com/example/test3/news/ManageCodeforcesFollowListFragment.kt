package com.example.test3.news

import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.*
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.account_manager.STATUS
import com.example.test3.utils.CodeforcesURLFactory
import com.example.test3.utils.CodeforcesUtils
import com.example.test3.workers.CodeforcesNewsFollowWorker
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
        setBottomPanelId(R.id.support_navigation_cf_follow)

        setHasOptionsMenu(true)

        val followListAdapter = FollowListItemsAdapter(mainActivity)
        val followListView = view.findViewById<RecyclerView>(R.id.manage_cf_follow_users_list).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = followListAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            setHasFixedSize(true)
        }

        val buttonAdd = requireBottomPanel().findViewById<ImageButton>(R.id.navigation_cf_follow_add).apply {
            visibility = View.GONE
        }
        buttonAdd.setOnClickListener {
            lifecycleScope.launch {

                val userInfo = mainActivity.chooseUserID(
                    mainActivity.accountsFragment.codeforcesAccountManager.emptyInfo(),
                    mainActivity.accountsFragment.codeforcesAccountManager
                ) as? CodeforcesAccountManager.CodeforcesUserInfo ?: return@launch

                buttonAdd.isEnabled = false
                followListAdapter.add(userInfo)
                buttonAdd.isEnabled = true
                followListView.scrollToPosition(0)
            }

        }

        lifecycleScope.launch {
            followListView.isEnabled = false
            followListAdapter.initialize()
            followListView.isEnabled = true
            buttonAdd.visibility = View.VISIBLE
        }

        mainActivity.settingsUI.useRealColorsLiveData.observeUpdates(viewLifecycleOwner){ use ->
            followListAdapter.notifyDataSetChanged()
        }
    }


    class FollowListItemsAdapter(val mainActivity: MainActivity) : RecyclerView.Adapter<FollowListItemsAdapter.ItemHolder>() {

        private val codeforcesAccountManager by lazy { mainActivity.accountsFragment.codeforcesAccountManager }

        private val list = mutableListOf<CodeforcesAccountManager.CodeforcesUserInfo>()
        private val dataConnector = CodeforcesNewsFollowWorker.FollowDataConnector(mainActivity)

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

        suspend fun add(userInfo: CodeforcesAccountManager.CodeforcesUserInfo){
            if(!dataConnector.add(userInfo.handle)){
                mainActivity.showToast("User already in list")
                return
            }
            list.add(0, userInfo)
            notifyItemInserted(0)
        }

        fun remove(userInfo: CodeforcesAccountManager.CodeforcesUserInfo){
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
                PopupMenu(mainActivity, holder.title, Gravity.CENTER_HORIZONTAL).apply {
                    inflate(R.menu.popup_cf_follow_list_item)

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        setForceShowIcon(true)
                    }

                    setOnMenuItemClickListener {
                        when(it.itemId){
                            R.id.cf_follow_list_item_open_blogs -> {
                                mainActivity.startActivity(makeIntentOpenUrl(CodeforcesURLFactory.userBlogs(userInfo.handle)))
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

        override fun getItemCount(): Int = list.size

    }


}