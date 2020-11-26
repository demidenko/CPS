package com.example.test3

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.test3.account_manager.STATUS
import kotlinx.android.synthetic.main.activity_main.*

class AccountViewFragment(): Fragment() {

    companion object {
        const val tag = "account_view"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_account_view, container, false)
    }

    private val panel: AccountPanel by lazy {
        val managerType = arguments?.getString("manager") ?: throw Exception("Unset type of manager")
        (requireActivity() as MainActivity).accountsFragment.getPanel(managerType)
    }

    private val userInfoTextView by lazy {
        requireView().findViewById<TextView>(R.id.account_user_info)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as MainActivity

        val manager = panel.manager

        val subtitle = "::accounts.${manager.PREFERENCES_FILE_NAME}"
        setFragmentSubTitle(this, subtitle)
        activity.setActionBarSubTitle(subtitle)
        activity.navigation.visibility = View.GONE

        val linkButton: ImageButton = view.findViewById<ImageButton>(R.id.account_panel_link_button).apply {
            setOnClickListener {
                val info = manager.savedInfo
                if(info.status == STATUS.OK){
                    activity.startActivity(makeIntentOpenUrl(info.link()))
                }
            }
        }

        setHasOptionsMenu(true)

        with(manager.savedInfo){
            userInfoTextView.text = toString()
            if(userID.isEmpty()) openSettings()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            menu.setGroupDividerEnabled(true)
        }
        inflater.inflate(R.menu.menu_account_view, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menu_account_delete_button -> deleteAccount()
            R.id.menu_account_settings_button -> openSettings()
        }
        return super.onOptionsItemSelected(item)
    }

    fun show(){
        userInfoTextView.text = panel.manager.savedInfo.toString()
    }

    private fun deleteAccount(){
        AlertDialog.Builder(requireActivity())
            .setMessage("Delete ${panel.manager.PREFERENCES_FILE_NAME} account?")
            .setPositiveButton("YES"){ _, _ ->
                panel.manager.savedInfo = panel.manager.emptyInfo()
                panel.show()
                requireActivity().onBackPressed()
            }
            .setNegativeButton("NO"){ _, _ -> }
            .create()
            .show()
    }

    private fun openSettings(){
        val activity = requireActivity() as MainActivity
        val managerType = panel.manager.PREFERENCES_FILE_NAME
        activity.supportFragmentManager.beginTransaction()
            .hide(this)
            .add(android.R.id.content, AccountSettingsFragment().apply {
                arguments = Bundle().apply { putString("manager", managerType) }
            })
            .addToBackStack(null)
            .commit()
    }

}