package com.example.test3

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.test3.account_manager.*
import com.example.test3.account_view.*
import com.example.test3.utils.CListUtils
import com.example.test3.utils.SharedReloadButton
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.navigation_accounts.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AccountsFragment: Fragment() {

    private val mainActivity by lazy { requireActivity() as MainActivity }

    val codeforcesAccountManager by lazy { CodeforcesAccountManager(mainActivity) }
    val atcoderAccountManager by lazy { AtCoderAccountManager(mainActivity) }
    val topcoderAccountManager by lazy { TopCoderAccountManager(mainActivity) }
    val acmpAccountManager by lazy { ACMPAccountManager(mainActivity) }
    val timusAccountManager by lazy { TimusAccountManager(mainActivity) }

    private val codeforcesPanel by lazy { CodeforcesAccountPanel(mainActivity, codeforcesAccountManager) }
    private val atcoderPanel by lazy { AtCoderAccountPanel(mainActivity, atcoderAccountManager) }
    private val topcoderPanel by lazy { TopCoderAccountPanel(mainActivity, topcoderAccountManager) }
    private val acmpPanel by lazy { ACMPAccountPanel(mainActivity, acmpAccountManager) }
    private val timusPanel by lazy { TimusAccountPanel(mainActivity, timusAccountManager) }
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
        return inflater.inflate(R.layout.fragment_accounts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        view.findViewById<LinearLayout>(R.id.panels_layout).apply {
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            addView(codeforcesPanel.createSmallView(), params)
            addView(atcoderPanel.createSmallView(), params)
            addView(topcoderPanel.createSmallView(), params)
            addView(acmpPanel.createSmallView(), params)
            addView(timusPanel.createSmallView(), params)
        }

        with(mainActivity){
            navigation_accounts_reload.setOnClickListener { reloadAccounts() }
            navigation_accounts_add.setOnClickListener { addAccount() }
        }

        showPanels()
    }

    fun addAccount() {
        lifecycleScope.launch {
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
                    else{
                        val panel = emptyPanels[index]
                        lifecycleScope.launch {
                            mainActivity.chooseUserID(panel.manager)?.let { userInfo ->
                                panel.manager.setSavedInfo(userInfo)
                                panel.show()
                            }
                        }
                    }
                }.create().show()
        }
    }

    private fun clistImport() {
        lifecycleScope.launch {
            val clistUserInfo = mainActivity.chooseUserID(CListAccountManager(mainActivity)) as? CListAccountManager.CListUserInfo ?: return@launch

            mainActivity.navigation_accounts_add.isEnabled = false

            val supported = clistUserInfo.accounts.mapNotNull { (resource, userData) ->
                CListUtils.getManager(resource, userData.first, userData.second, mainActivity)
            }

            val progressInfo = BottomProgressInfo(supported.size, "clist import", mainActivity)

            supported.map { (manager, userID) ->
                val panel = getPanel(manager.PREFERENCES_FILE_NAME)
                async {
                    while(panel.isBlocked()) delay(300)
                    panel.block()
                    val userInfo = manager.loadInfo(userID)
                    manager.setSavedInfo(userInfo)
                    panel.show()
                    panel.unblock()
                    progressInfo.increment()
                }
            }.awaitAll()

            progressInfo.finish()
            mainActivity.navigation_accounts_add.isEnabled = true
        }
    }

    suspend fun updateUI(){
        var allEmpty = true
        var statusBarColor: Int = Color.TRANSPARENT
        panels.forEach { panel ->
            if(!panel.isEmpty()){
                allEmpty = false
                with(panel.manager){
                    getColor(getSavedInfo())?.let {
                        if(statusBarColor == Color.TRANSPARENT){
                            statusBarColor = it
                        }
                    }
                }
            }
        }
        //println("update UI: $allEmpty $statusBarColor")
        requireView().findViewById<TextView>(R.id.accounts_welcome_text).visibility = if(allEmpty) View.VISIBLE else View.GONE
        mainActivity.window.statusBarColor = statusBarColor
    }

    fun showPanels() {
        lifecycleScope.launch {
            panels.forEach { it.show() }
        }
    }

    fun reloadAccounts() {
        lifecycleScope.launch {
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

    val sharedReloadButton by lazy { SharedReloadButton(mainActivity.navigation_accounts_reload) }

    override fun onHiddenChanged(hidden: Boolean) {
        if(!hidden){
            with(mainActivity){
                navigation.visibility = View.VISIBLE
            }
        }
        super.onHiddenChanged(hidden)
    }


}