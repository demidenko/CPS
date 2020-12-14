package com.example.test3.account_view

import android.os.Bundle
import android.view.*
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.setFragmentSubTitle
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch

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

        val subtitle = "::accounts.${manager.PREFERENCES_FILE_NAME}.settings"
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
                    panel.resetRelatedData()
                    userId.text = userInfo.userID
                    view.isEnabled = true
                }
            }
        }

        lifecycleScope.launch {
            userId.text = manager.getSavedInfo().userID
            panel.createSettingsView(this@AccountSettingsFragment)
        }

    }

    fun createAndAddSwitch(title: String, checked: Boolean, onChangeCallback: (buttonView: CompoundButton, isChecked: Boolean)->Unit): View {
        val view = requireView() as LinearLayout
        layoutInflater.inflate(R.layout.account_settings_switcher, view)
        return view[view.childCount-1].apply {
            findViewById<TextView>(R.id.account_settings_switcher_title).text = title
            findViewById<SwitchMaterial>(R.id.account_settings_switcher_button).apply {
                isChecked = checked
                this.setOnCheckedChangeListener { buttonView, isChecked -> onChangeCallback(buttonView,isChecked) }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        super.onCreateOptionsMenu(menu, inflater)
    }

}