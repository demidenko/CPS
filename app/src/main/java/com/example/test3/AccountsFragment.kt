package com.example.test3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.lifecycle.lifecycleScope
import com.example.test3.account_manager.*
import com.example.test3.account_view.*
import com.example.test3.ui.*
import com.example.test3.utils.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

class AccountsFragment: CPSFragment() {

    val accountViewModel: AccountViewModel by viewModels()

    val codeforcesAccountManager by lazy { CodeforcesAccountManager(mainActivity) }
    val atcoderAccountManager by lazy { AtCoderAccountManager(mainActivity) }
    val topcoderAccountManager by lazy { TopCoderAccountManager(mainActivity) }
    val acmpAccountManager by lazy { ACMPAccountManager(mainActivity) }
    val timusAccountManager by lazy { TimusAccountManager(mainActivity) }

    private val panels by lazy {
        listOf(
            CodeforcesAccountPanel(mainActivity, codeforcesAccountManager),
            AtCoderAccountPanel(mainActivity, atcoderAccountManager),
            TopCoderAccountPanel(mainActivity, topcoderAccountManager),
            ACMPAccountPanel(mainActivity, acmpAccountManager),
            TimusAccountPanel(mainActivity, timusAccountManager)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_accounts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cpsTitle = "::accounts"
        setBottomPanelId(R.id.support_navigation_accounts, R.layout.navigation_accounts)

        val notEmptyPanels = MutableSetLiveSize<String>()
        view.findViewById<LinearLayout>(R.id.panels_layout).apply {
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

            panels.forEach { panel -> addView(panel.createSmallView(), params) }

            viewLifecycleOwner.addRepeatingJob(Lifecycle.State.STARTED){
                panels.forEach { panel ->
                    launch {
                        panel.manager.getInfoFlow().collect { (manager, info) ->
                            panel.show()
                            if(info.isEmpty()) notEmptyPanels.remove(manager.managerName)
                            else notEmptyPanels.add(manager.managerName)
                        }
                    }
                }
            }
        }

        view.findViewById<TextView>(R.id.accounts_welcome_text).apply {
            notEmptyPanels.sizeLiveData.observe(viewLifecycleOwner){ count ->
                isVisible = count==0
            }
        }

        reloadButton.setOnClickListener { reloadAccounts() }
        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.STARTED){
            BlockedState.combineBlockedStatesFlows(
                panels.map { accountViewModel.getAccountSmallViewBlockedState(it.manager) }
            ).collect {
                if(it == BlockedState.BLOCKED) reloadButton.disable()
                else reloadButton.enable()
            }
        }

        addAccountButton.setOnClickListener { addAccount() }

        mainActivity.subscribeProgressBar("clist import", accountViewModel.getClistImportProgress())
        mainActivity.addRepeatingJob(Lifecycle.State.STARTED){
            accountViewModel.getClistImportProgress().collect {
                if(it != null) addAccountButton.disable()
                else addAccountButton.enable()
            }
        }

        mainActivity.settingsUI.useRealColorsLiveData.observeUpdates(viewLifecycleOwner){ use ->
            showPanels()
        }


        mainActivity.addRepeatingJob(Lifecycle.State.STARTED){
            statusBarColorManager.getStatusBarColorFlow().collect(){ color ->
                mainActivity.window.statusBarColor = color
            }
        }

    }

    private val reloadButton by lazy { requireBottomPanel().findViewById<ImageButton>(R.id.navigation_accounts_reload) }
    private val addAccountButton by lazy { requireBottomPanel().findViewById<ImageButton>(R.id.navigation_accounts_add) }

    fun addAccount() {
        lifecycleScope.launch {
            BottomSheetDialog(mainActivity).apply {
                val view = layoutInflater.inflate(R.layout.dialog_add_account, null).apply {

                    findViewById<TextView>(R.id.dialog_add_account_cancel).setOnClickListener { dismiss() }

                    findViewById<ConstraintLayout>(R.id.dialog_add_account_clist).apply {
                        findViewById<TextView>(R.id.dialog_add_account_item_title).text = "import from clist.by"

                        setOnClickListener {
                            dismiss()
                            clistImport()
                        }

                        findViewById<ImageButton>(R.id.dialog_add_account_item_help).setOnClickListener {
                            startActivity(makeIntentOpenUrl("https://clist.by"))
                        }
                    }

                    val listView = findViewById<LinearLayout>(R.id.dialog_add_account_list)
                    panels.filter { it.isEmpty() }.forEach { panel ->
                        val itemView = layoutInflater.inflate(R.layout.dialog_add_account_item, null).apply {
                            findViewById<TextView>(R.id.dialog_add_account_item_title).text = panel.manager.managerName

                            setOnClickListener {
                                dismiss()
                                lifecycleScope.launch {
                                    mainActivity.chooseUserID(panel.manager)?.let { userInfo ->
                                        panel.manager.setSavedInfo(userInfo)
                                    }
                                }
                            }

                            findViewById<ImageButton>(R.id.dialog_add_account_item_help).setOnClickListener {
                                startActivity(makeIntentOpenUrl(panel.homeURL))
                            }
                        }
                        listView.addView(itemView)
                    }
                }
                setContentView(view)

            }.show()
        }
    }

    private fun clistImport() {
        lifecycleScope.launch {
            val clistUserInfo = with(CListAccountManager(mainActivity)){
                mainActivity.chooseUserID(emptyInfo(), this) as? CListAccountManager.CListUserInfo
            } ?: return@launch
            accountViewModel.clistImport(clistUserInfo, requireContext())
        }
    }


    val statusBarColorManager by lazy { StatusBarColorManager(mainActivity, panels.map { it.manager }) }

    fun showPanels() {
        lifecycleScope.launch {
            panels.forEach { it.show() }
        }
    }

    private fun reloadAccounts() = panels.forEach { it.reload() }

    fun getPanel(managerType: String): AccountPanel {
        return panels.find { panel ->
            panel.manager.managerName == managerType
        } ?: throw Exception("Unknown type of manager: $managerType")
    }

    fun getPanel(fragment: CPSFragment): AccountPanel {
        val managerType = fragment.requireArguments().getString("manager") ?: throw Exception("Unset type of manager")
        return getPanel(managerType)
    }

}