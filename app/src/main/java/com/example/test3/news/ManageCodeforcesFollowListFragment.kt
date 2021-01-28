package com.example.test3.news

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.account_manager.STATUS
import com.example.test3.job_services.CodeforcesNewsFollowJobService
import com.example.test3.makeIntentOpenUrl
import com.example.test3.setFragmentSubTitle
import com.example.test3.utils.CodeforcesURLFactory
import com.example.test3.utils.CodeforcesUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_manage_cf_follow.view.*
import kotlinx.coroutines.launch

class ManageCodeforcesFollowListFragment(): Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_manage_cf_follow, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        val mainActivity = requireActivity() as MainActivity

        val subtitle = "::news.codeforces.follow.list"
        setFragmentSubTitle(this, subtitle)
        mainActivity.setActionBarSubTitle(subtitle)
        mainActivity.navigation.visibility = View.GONE

        val followListAdapter = FollowListItemsAdapter(mainActivity)
        val followListView = view.manage_cf_follow_users_list.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = followListAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            setHasFixedSize(true)
        }

        val buttonAdd = view.manage_cf_follow_add
        buttonAdd.setOnClickListener { button -> button as Button
            lifecycleScope.launch {

                val userInfo = mainActivity.chooseUserID(
                    mainActivity.accountsFragment.codeforcesAccountManager.emptyInfo(),
                    mainActivity.accountsFragment.codeforcesAccountManager
                ) as? CodeforcesAccountManager.CodeforcesUserInfo ?: return@launch

                button.isEnabled = false
                followListAdapter.add(userInfo)
                button.isEnabled = true
                followListView.scrollToPosition(0)
            }

        }

        lifecycleScope.launch {
            followListView.isEnabled = false
            followListAdapter.initialize()
            followListView.isEnabled = true
            buttonAdd.visibility = View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        super.onCreateOptionsMenu(menu, inflater)
    }


    class FollowListItemsAdapter(val mainActivity: MainActivity) : RecyclerView.Adapter<FollowListItemsAdapter.ItemHolder>() {

        private val codeforcesAccountManager by lazy { mainActivity.accountsFragment.codeforcesAccountManager }

        private val list = mutableListOf<CodeforcesAccountManager.CodeforcesUserInfo>()
        private val dataConnector = CodeforcesNewsFollowJobService.FollowDataConnector(mainActivity)

        suspend fun initialize(){
            val handles = dataConnector.getHandles()
            val usersInfo = CodeforcesUtils.getUsersInfo(handles, true)
            handles.mapNotNull { handle ->
                usersInfo[handle]
                    ?.apply {
                        if(status == STATUS.NOT_FOUND) dataConnector.remove(handle)
                        else {
                            if(status == STATUS.OK && this.handle != handle){
                                dataConnector.changeHandle(handle, this.handle)
                            }
                        }
                    }
                    ?.takeIf { it.status != STATUS.NOT_FOUND }
            }.let { infos ->
                list.addAll(infos.distinctBy { it.handle })
            }
            dataConnector.save()
            notifyItemRangeInserted(0, list.size)
        }

        suspend fun add(userInfo: CodeforcesAccountManager.CodeforcesUserInfo){
            if(!dataConnector.add(userInfo.handle)){
                mainActivity.showToast("User already in list")
                return
            }
            dataConnector.save()
            list.add(0, userInfo)
            notifyItemInserted(0)
        }

        fun remove(userInfo: CodeforcesAccountManager.CodeforcesUserInfo){
            val index = list.indexOf(userInfo).takeIf { it!=-1 } ?: return

            dataConnector.remove(userInfo.handle)
            dataConnector.save()
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