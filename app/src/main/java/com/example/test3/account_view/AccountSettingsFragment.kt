package com.example.test3.account_view

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.setFragmentSubTitle
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
                    manager.setSavedInfo(userInfo)
                    userId.text = userInfo.userID
                    val accountViewFragment = (mainActivity.supportFragmentManager.findFragmentByTag(AccountViewFragment.tag) as AccountViewFragment)
                    panel.showBigView(accountViewFragment)
                }
            }
        }

        lifecycleScope.launch {
            userId.text = manager.getSavedInfo().userID
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        super.onCreateOptionsMenu(menu, inflater)
    }

}