package com.example.test3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class AccountsFragment: CPSFragment() {

    val accountViewModel: AccountViewModel by viewModels()

    private val panels by lazy {
        val context = requireContext()
        listOf(
            CodeforcesAccountPanel(mainActivity, CodeforcesAccountManager(context)),
            AtCoderAccountPanel(mainActivity, AtCoderAccountManager(context)),
            TopCoderAccountPanel(mainActivity, TopCoderAccountManager(context)),
            ACMPAccountPanel(mainActivity, ACMPAccountManager(context)),
            TimusAccountPanel(mainActivity, TimusAccountManager(context))
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
                        panel.manager.flowOfInfo().collect { (manager, info) ->
                            panel.show()
                            if(info.isEmpty()) notEmptyPanels.remove(manager.managerName)
                            else notEmptyPanels.add(manager.managerName)
                        }
                    }
                }
                launch {
                    val welcomeTextView = view.findViewById<TextView>(R.id.accounts_welcome_text)
                    notEmptyPanels.sizeStateFlow.collect { count -> welcomeTextView.isGone = count>0 }
                }
            }
        }

        reloadButton.setOnClickListener { reloadAccounts() }
        addAccountButton.setOnClickListener { addAccount() }

        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.STARTED){
            BlockedState.combineBlockedStatesFlows(
                panels.map { accountViewModel.getAccountSmallViewBlockedState(it.manager) }
            ).onEach {
                reloadButton.enableIff(it != BlockedState.BLOCKED)
            }.launchIn(this)

            mainActivity.settingsUI.flowOfUseRealColors().ignoreFirst().onEach { showPanels() }.launchIn(this)

            statusBarColorManager.getStatusBarColorFlow().onEach { color ->
                mainActivity.window.statusBarColor = color
            }.launchIn(this)

            accountViewModel.getClistImportProgress().onEach {
                addAccountButton.enableIff(it == null)
            }.launchIn(this)
        }

        mainActivity.subscribeProgressBar("clist import", accountViewModel.getClistImportProgress())

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
                            startActivity(makeIntentOpenUrl(CListAccountManager(requireContext()).homeURL))
                        }
                    }

                    val listView = findViewById<LinearLayout>(R.id.dialog_add_account_list)
                    panels.filter { it.isEmpty() }.forEach { panel ->
                        val itemView = layoutInflater.inflate(R.layout.dialog_add_account_item, null).apply {
                            findViewById<TextView>(R.id.dialog_add_account_item_title).text = panel.manager.managerName

                            setOnClickListener {
                                dismiss()
                                lifecycleScope.launch {
                                    panel.manager.chooseAndSave(mainActivity)
                                }
                            }

                            findViewById<ImageButton>(R.id.dialog_add_account_item_help).setOnClickListener {
                                startActivity(makeIntentOpenUrl(panel.manager.homeURL))
                            }
                        }
                        listView.addView(itemView)
                    }
                }
                setContentView(view)

            }.show()
        }
    }

    private suspend fun<U: UserInfo> AccountManager<U>.chooseAndSave(mainActivity: MainActivity) {
        mainActivity.chooseUserIDFromSaved(this)?.let { info ->
            setSavedInfo(info)
        }
    }

    private fun clistImport() {
        lifecycleScope.launch {
            val clistUserInfo = mainActivity.chooseUserID(CListAccountManager(mainActivity)) ?: return@launch
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

    fun<U: UserInfo> getPanel(managerType: String): AccountPanel<U> {
        return panels.find { panel ->
            panel.manager.managerName == managerType
        } as? AccountPanel<U> ?: throw Exception("Unknown type of manager: $managerType")
    }

    fun<U: UserInfo> getPanel(fragment: CPSFragment): AccountPanel<U> {
        val managerType = fragment.requireArguments().getString("manager") ?: throw Exception("Unset type of manager")
        return getPanel(managerType)
    }

}