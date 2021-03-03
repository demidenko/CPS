package com.example.test3

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.example.test3.account_manager.*
import com.example.test3.account_view.*
import com.example.test3.utils.CListUtils
import com.example.test3.utils.SharedReloadButton
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.*

class AccountsFragment: CPSFragment() {

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
            //check data file names
            if(this.map { it.manager.managerName }.distinct().size != this.size)
                throw Exception("not different file names in panels managers")

            //check ranks of colors
            mapNotNull { it.manager as? RatedAccountManager }.apply {
                map { ratedManager ->
                    val colors = ratedManager.rankedHandleColorsList
                    colors.forEachIndexed { index, handleColor ->
                        ratedManager.getColor(handleColor)

                        if(index>0 && handleColor < colors[index-1])
                            throw Exception("${ratedManager.managerName}: color list is not sorted")
                    }
                    colors.size
                }.apply {
                    if(distinct().size != 1)
                        throw Exception("different sizes for color lists")
                }
            }
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
        super.onViewCreated(view, savedInstanceState)

        cpsTitle = "::accounts"
        setBottomPanelId(R.id.support_navigation_accounts)

        view.findViewById<LinearLayout>(R.id.panels_layout).apply {
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

            panels.forEach { panel ->
                addView(panel.createSmallView(), params)
                panel.manager.dataStoreLive.observe(viewLifecycleOwner){
                    lifecycleScope.launch { panel.show() }
                }
            }
        }

        reloadButton.setOnClickListener { reloadAccounts() }
        addAccountButton.setOnClickListener { addAccount() }

        mainActivity.settingsUI.useRealColorsLiveData.observeUpdates(viewLifecycleOwner){ use ->
            showPanels()
        }
    }

    private val reloadButton by lazy { requireBottomPanel().findViewById<ImageButton>(R.id.navigation_accounts_reload) }
    private val addAccountButton by lazy { requireBottomPanel().findViewById<ImageButton>(R.id.navigation_accounts_add) }

    fun addAccount() {
        lifecycleScope.launch {
            BottomSheetDialog(requireContext()).apply {
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
            val clistUserInfo = mainActivity.chooseUserID(CListAccountManager(mainActivity)) as? CListAccountManager.CListUserInfo ?: return@launch

            addAccountButton.isEnabled = false

            val supported = clistUserInfo.accounts.mapNotNull { (resource, userData) ->
                CListUtils.getManager(resource, userData.first, userData.second, mainActivity)
            }

            val progressInfo = BottomProgressInfo("clist import", mainActivity).start(supported.size)

            supported.map { (manager, userID) ->
                val panel = getPanel(manager.managerName)
                async {
                    while(panel.isBlocked()) delay(300)
                    panel.block()
                    val userInfo = manager.loadInfo(userID)
                    manager.setSavedInfo(userInfo)
                    panel.unblock()
                    progressInfo.increment()
                }
            }.awaitAll()

            progressInfo.finish()
            addAccountButton.isEnabled = true
        }
    }


    val statusBarColorManager by lazy { StatusBarColorManager(this) }

    class StatusBarColorManager(
        private val accountsFragment: AccountsFragment
    ) {

        data class ColorInfo(
            val color: Int = Color.TRANSPARENT,
            val order: Double = -1e9
        )

        private val infoByPanel = mutableMapOf<String,ColorInfo>()
        private var current: String? = null

        private fun getListOfColors(): List<ColorInfo> {
            if(current != null) return listOfNotNull(infoByPanel[current])
            return infoByPanel.values.toList()
        }

        private fun updateStatusBar() {

            val (best, allEmpty) =
                getListOfColors().maxByOrNull { it.order }
                    ?.let { it to false }
                    ?: ColorInfo() to true

            with(accountsFragment){
                view?.findViewById<TextView>(R.id.accounts_welcome_text)?.visibility = if(allEmpty) View.VISIBLE else View.GONE
                mainActivity.window.statusBarColor = best.color
            }
        }

        fun setCurrent(panel: AccountPanel?) {
            current = panel?.manager?.managerName
            updateStatusBar()
        }

        suspend fun updatePanel(panel: AccountPanel) {
            val name = panel.manager.managerName
            if(!panel.isEmpty()) {
                infoByPanel[name] = ColorInfo()
                (panel.manager as? RatedAccountManager)?.let { manager ->
                    val info = manager.getSavedInfo()
                    manager.getColor(info)?.let {
                        infoByPanel[name] = ColorInfo(it, manager.getOrder(info))
                    }
                }
            } else {
                infoByPanel.remove(name)
            }
            updateStatusBar()
        }
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
            panel.manager.managerName == managerType
        } ?: throw Exception("Unknown type of manager: $managerType")
    }

    fun getPanel(fragment: CPSFragment): AccountPanel {
        val managerType = fragment.requireArguments().getString("manager") ?: throw Exception("Unset type of manager")
        return getPanel(managerType)
    }

    val sharedReloadButton by lazy { SharedReloadButton(reloadButton) }


}