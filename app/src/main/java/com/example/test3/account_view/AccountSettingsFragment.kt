package com.example.test3.account_view

import android.os.Bundle
import android.view.*
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.setFragmentSubTitle
import com.example.test3.utils.setupSwitch
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AccountSettingsFragment(): Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_account_settings, container, false)
    }

    private val panel: AccountPanel by lazy {
        val managerType = arguments?.getString("manager") ?: throw Exception("Unset type of manager")
        (requireActivity() as MainActivity).accountsFragment.getPanel(managerType)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mainActivity = requireActivity() as MainActivity

        val manager = panel.manager

        val subtitle = "::accounts.${manager.managerName}.settings"
        setFragmentSubTitle(this, subtitle)
        mainActivity.setActionBarSubTitle(subtitle)
        mainActivity.navigation.visibility = View.GONE

        setHasOptionsMenu(true)

        val userIDView: View = view.findViewById(R.id.account_settings_userid)
        val userId: TextView = userIDView.findViewById(R.id.account_settings_userid_value)

        userIDView.apply {
            findViewById<TextView>(R.id.account_settings_userid_title).text = "${manager.userIDName}:"

            setOnClickListener {
                lifecycleScope.launch {
                    val userInfo = mainActivity.chooseUserID(manager) ?: return@launch
                    view.isEnabled = false
                    manager.setSavedInfo(userInfo)
                    userId.text = userInfo.userID
                    view.isEnabled = true
                }
            }
        }

        runBlocking {
            userId.text = manager.getSavedInfo().userID
            view.isEnabled = false
            panel.createSettingsView(this@AccountSettingsFragment)
            view.isEnabled = true
        }

    }

    fun createAndAddSwitch(
        title: String,
        checked: Boolean,
        description: String = "",
        onChangeCallback: (buttonView: CompoundButton, isChecked: Boolean) -> Unit
    ){
        val view = requireView().findViewById<LinearLayout>(R.id.layout)
        layoutInflater.inflate(R.layout.settings_switcher, view)
        setupSwitch(view[view.childCount-1] as ConstraintLayout, title, checked, description, onChangeCallback)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        super.onCreateOptionsMenu(menu, inflater)
    }

}