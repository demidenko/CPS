package com.example.test3

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
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

        val activity = requireActivity() as MainActivity

        val manager = panel.manager

        val subtitle = "::accounts.${manager.PREFERENCES_FILE_NAME}.settings"
        setFragmentSubTitle(this, subtitle)
        activity.setActionBarSubTitle(subtitle)
        activity.navigation.visibility = View.GONE

        setHasOptionsMenu(true)

        val textView: TextView = view.findViewById(R.id.account_settings_userid)

        textView.setOnClickListener {
            activity.scope.launch {
                val currentUserID = manager.savedInfo.userID
                val userInfo = activity.chooseUserID(manager)
                if(userInfo==null){
                    if(currentUserID.isEmpty()){
                        activity.onBackPressed()
                        activity.onBackPressed()
                    }
                }else{
                    manager.savedInfo = userInfo
                    textView.text = userInfo.userID
                    panel.show()
                    (activity.supportFragmentManager.findFragmentByTag(AccountViewFragment.tag) as AccountViewFragment).show()
                }
            }
        }

        with(manager.savedInfo.userID){
            textView.text = this
            if(isEmpty()) textView.callOnClick()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        super.onCreateOptionsMenu(menu, inflater)
    }

}