package com.example.test3

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.test3.account_manager.*
import com.example.test3.utils.CListUtils
import com.example.test3.utils.CodeforcesUtils
import com.example.test3.utils.SharedReloadButton
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.navigation_accounts.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class AccountsFragment: Fragment() {

    lateinit var codeforcesAccountManager: CodeforcesAccountManager
    lateinit var atcoderAccountManager: AtCoderAccountManager
    lateinit var topcoderAccountManager: TopCoderAccountManager
    lateinit var acmpAccountManager: ACMPAccountManager
    lateinit var timusAccountManager: TimusAccountManager

    private lateinit var codeforcesPanel: AccountPanel
    private lateinit var atcoderPanel: AccountPanel
    private lateinit var topcoderPanel: AccountPanel
    private lateinit var acmpPanel: AccountPanel
    private lateinit var timusPanel: AccountPanel
    private val panels by lazy {
        listOf(
            codeforcesPanel,
            atcoderPanel,
            topcoderPanel,
            acmpPanel,
            timusPanel
        ).apply {
            if(this.map { it.manager.PREFERENCES_FILE_NAME }.distinct().size != this.size)
                throw Exception("not different file names in panels managers")
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        println("fragment accounts onCreateView $savedInstanceState")
        return inflater.inflate(R.layout.fragment_accounts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        println("fragment accounts onViewCreated "+savedInstanceState)

        val activity = requireActivity() as MainActivity

        codeforcesAccountManager = CodeforcesAccountManager(activity)
        codeforcesPanel = object : AccountPanel(activity, codeforcesAccountManager){
            override fun show(info: UserInfo) { info as CodeforcesAccountManager.CodeforcesUserInfo
                val color = manager.getColor(info)
                textMain.text = CodeforcesUtils.makeSpan(info)
                textAdditional.text = ""
                textAdditional.setTextColor(color ?: activity.defaultTextColor)
                if(info.status == STATUS.OK){
                    textMain.typeface = Typeface.DEFAULT_BOLD
                    textAdditional.text = if(info.rating == NOT_RATED) "[not rated]" else "${info.rating}"
                }else{
                    textMain.typeface = Typeface.DEFAULT
                }
            }
        }

        atcoderAccountManager = AtCoderAccountManager(activity)
        atcoderPanel = object : AccountPanel(activity, atcoderAccountManager){
            override fun show(info: UserInfo) { info as AtCoderAccountManager.AtCoderUserInfo
                val color = manager.getColor(info)
                textMain.text = info.handle
                textMain.setTextColor(color ?: activity.defaultTextColor)
                textAdditional.text = ""
                textAdditional.setTextColor(color ?: activity.defaultTextColor)
                if(info.status == STATUS.OK){
                    textMain.typeface = Typeface.DEFAULT_BOLD
                    textAdditional.text = if(info.rating == NOT_RATED) "[not rated]" else "${info.rating}"
                }else{
                    textMain.typeface = Typeface.DEFAULT
                }
            }
        }

        topcoderAccountManager = TopCoderAccountManager(activity)
        topcoderPanel = object : AccountPanel(activity, topcoderAccountManager){
            override fun show(info: UserInfo) { info as TopCoderAccountManager.TopCoderUserInfo
                val color = manager.getColor(info)
                textMain.text = info.handle
                textMain.setTextColor(color ?: activity.defaultTextColor)
                textAdditional.text = ""
                textAdditional.setTextColor(color ?: activity.defaultTextColor)
                if(info.status == STATUS.OK){
                    textMain.typeface = Typeface.DEFAULT_BOLD
                    textAdditional.text = if(info.rating_algorithm == NOT_RATED) "[not rated]" else "${info.rating_algorithm}"
                }else{
                    textMain.typeface = Typeface.DEFAULT
                }
            }
        }

        acmpAccountManager = ACMPAccountManager(activity)
        acmpPanel = object : AccountPanel(activity, acmpAccountManager){
            override fun show(info: UserInfo) { info as ACMPAccountManager.ACMPUserInfo
                with(info){
                    if (status == STATUS.OK) {
                        textMain.text = userName
                        textAdditional.text = "Solved: $solvedTasks  Rank: $place  Rating: $rating"
                    }else{
                        textMain.text = id
                        textAdditional.text = ""
                    }
                }
                textMain.setTextColor(activity.defaultTextColor)
                textAdditional.setTextColor(activity.defaultTextColor)
            }
        }

        timusAccountManager = TimusAccountManager(activity)
        timusPanel = object : AccountPanel(activity, timusAccountManager){
            override fun show(info: UserInfo) { info as TimusAccountManager.TimusUserInfo
                with(info){
                    if (status == STATUS.OK) {
                        textMain.text = userName
                        textAdditional.text = "Solved: $solvedTasks  Rank: $placeTasks"
                    }else{
                        textMain.text = id
                        textAdditional.text = ""
                    }
                }
                textMain.setTextColor(activity.defaultTextColor)
                textAdditional.setTextColor(activity.defaultTextColor)
            }
        }

        codeforcesPanel.buildAndAdd(30F, 25F, view)
        atcoderPanel.buildAndAdd(30F, 25F, view)
        topcoderPanel.buildAndAdd(30F, 25F, view)
        acmpPanel.buildAndAdd(17F, 14F, view)
        timusPanel.buildAndAdd(17F, 14F, view)

        showPanels()

        with(activity){
            navigation_accounts_reload.setOnClickListener { reloadAccounts() }
            navigation_accounts_add.setOnClickListener { addAccount() }
        }
    }

    fun addAccount() {
        val emptyPanels = panels.filter { it.isEmpty() }

        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.select_dialog_item)
        emptyPanels.forEach {
            adapter.add(it.manager.PREFERENCES_FILE_NAME)
        }

        adapter.add("Import from clist.by")

        AlertDialog.Builder(activity)
            .setTitle("Add account")
            .setAdapter(adapter) { _, index ->
                if(index == emptyPanels.size) clistImport()
                else emptyPanels[index].callExpand()
            }.create().show()
    }

    private fun clistImport() {
        val activity = requireActivity() as MainActivity
        activity.scope.launch {
            val clistUserInfo = activity.chooseUserID("", CListAccountManager(activity)) ?: return@launch

            activity.navigation_accounts_add.isEnabled = false

            clistUserInfo as CListAccountManager.CListUserInfo
            clistUserInfo.accounts.mapNotNull { (resource, userData) ->
                val (manager, userID) = CListUtils.getManager(resource, userData.first, userData.second, activity) ?: return@mapNotNull null
                val panel = getPanel(manager.PREFERENCES_FILE_NAME)
                panel.block()
                async {
                    val userInfo = manager.loadInfo(userID)
                    manager.savedInfo = userInfo
                    panel.show()
                    panel.unblock()
                }
            }.awaitAll()

            activity.navigation_accounts_add.isEnabled = true

            Toast.makeText(activity,"Import finished", Toast.LENGTH_LONG).show()
        }
    }

    fun updateUI(){
        var allEmpty = true
        var statusBarColor: Int = Color.TRANSPARENT
        panels.forEach { panel ->
            if(!panel.isEmpty()){
                allEmpty = false
                with(panel.manager){
                    getColor(savedInfo)?.let {
                        if(statusBarColor == Color.TRANSPARENT){
                            statusBarColor = it
                        }
                    }
                }
            }
        }
        //println("update UI: $allEmpty $statusBarColor")
        requireView().findViewById<TextView>(R.id.accounts_welcome_text).visibility = if(allEmpty) View.VISIBLE else View.GONE
        requireActivity().window.statusBarColor = statusBarColor
    }

    fun showPanels() {
        panels.forEach { it.show() }
    }

    fun reloadAccounts() {
        (requireActivity() as MainActivity).scope.launch {
            panels.forEach {
                launch { it.reload() }
            }
        }
    }

    fun getPanel(managerType: String): AccountPanel {
        return panels.find { panel ->
            panel.manager.PREFERENCES_FILE_NAME == managerType
        } ?: throw Exception("Unknown type of manager: $managerType")
    }

    val sharedReloadButton by lazy { SharedReloadButton(requireActivity().navigation_accounts_reload) }

    override fun onHiddenChanged(hidden: Boolean) {
        if(!hidden){
            with(requireActivity() as MainActivity){
                navigation.visibility = View.VISIBLE
            }
        }
        super.onHiddenChanged(hidden)
    }


}