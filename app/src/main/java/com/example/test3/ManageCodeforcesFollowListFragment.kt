package com.example.test3

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.job_services.CodeforcesNewsFollowJobService
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

    private val followListView by lazy {
        requireView().manage_cf_follow_users_list
    }

    private val followListAdapter = FollowListItemsAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as MainActivity

        val subtitle = "::news.codeforces.follow.list"
        setFragmentSubTitle(this, subtitle)
        activity.setActionBarSubTitle(subtitle)
        activity.navigation.visibility = View.GONE

        followListView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = followListAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            setHasFixedSize(true)
        }

        val buttonAdd = view.manage_cf_follow_add.apply {
            visibility = View.GONE
        }
        buttonAdd.setOnClickListener { button -> button as Button
            with(activity){
                scope.launch {
                    val userInfo = chooseUserID("", accountsFragment.codeforcesAccountManager) ?: return@launch
                    userInfo as CodeforcesAccountManager.CodeforcesUserInfo
                    followListAdapter.add(userInfo)
                    followListView.scrollToPosition(0)
                }
            }
        }

        val followList = CodeforcesNewsFollowJobService.getSavedHandles(activity)

        activity.scope.launch {
            followListView.isEnabled = false

            val usersInfo = CodeforcesUtils.getUsersInfo(followList)
            followListAdapter.initialize(usersInfo)

            followListView.isEnabled = true
            buttonAdd.visibility = View.VISIBLE
        }

    }


    class FollowListItemsAdapter() : RecyclerView.Adapter<FollowListItemsAdapter.ItemHolder>() {

        private val list = mutableListOf<CodeforcesAccountManager.CodeforcesUserInfo>()

        fun handlesList() = list.map { it.handle }

        fun initialize(usersInfo: List<CodeforcesAccountManager.CodeforcesUserInfo>){
            list.clear()
            list.addAll(usersInfo)
            notifyItemRangeInserted(0, list.size)
        }

        fun add(userInfo: CodeforcesAccountManager.CodeforcesUserInfo){
            if(list.any { it.handle == userInfo.handle }){
                activity.showToast("User already in list")
                return
            }
            list.add(0, userInfo)
            CodeforcesNewsFollowJobService.saveHandles(activity, handlesList())
            notifyItemInserted(0)
        }

        fun remove(userInfo: CodeforcesAccountManager.CodeforcesUserInfo){
            val index = list.indexOf(userInfo)
            list.removeAt(index)
            CodeforcesNewsFollowJobService.saveHandles(activity, handlesList())
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
            holder.title.text = CodeforcesUtils.makeSpan(userInfo)

            holder.view.setOnClickListener {
                PopupMenu(activity, holder.title, Gravity.CENTER_HORIZONTAL).apply {
                    inflate(R.menu.popup_cf_follow_list_item)

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        setForceShowIcon(true)
                    }

                    setOnMenuItemClickListener {
                        when(it.itemId){
                            R.id.cf_follow_list_item_open_blogs -> {
                                activity.startActivity(makeIntentOpenUrl(CodeforcesURLFactory.userBlogs(userInfo.handle)))
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


        lateinit var activity: MainActivity
        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            super.onAttachedToRecyclerView(recyclerView)
            activity = recyclerView.context as MainActivity
        }
    }


}