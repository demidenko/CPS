package com.example.test3.account_view

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.test3.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch

class AccountViewFragment(): Fragment() {

    val panel: AccountPanel by lazy {
        val managerType = arguments?.getString("manager") ?: throw Exception("Unset type of manager")
        (requireActivity() as MainActivity).accountsFragment.getPanel(managerType)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(panel.bigViewResource, container, false)
    }




    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        val mainActivity = requireActivity() as MainActivity

        val manager = panel.manager

        val subtitle = "::accounts.${manager.PREFERENCES_FILE_NAME}"
        setFragmentSubTitle(this, subtitle)
        mainActivity.setActionBarSubTitle(subtitle)
        mainActivity.navigation.visibility = View.GONE

        fun showBigView() = lifecycleScope.launch { panel.showBigView(this@AccountViewFragment) }

        manager.dataStoreLive.observe(viewLifecycleOwner){
            showBigView()
        }

        mainActivity.settingsUI.useRealColorsLiveData.observeUpdates(viewLifecycleOwner){ use ->
            showBigView()
        }

        mainActivity.accountsFragment.statusBarColorManager.setCurrent(panel)
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
            R.id.menu_account_open_button -> lifecycleScope.launch { startActivity(makeIntentOpenUrl(panel.manager.getSavedInfo().link())) }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun deleteAccount(){
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete ${panel.manager.PREFERENCES_FILE_NAME} account?")
            .setPositiveButton("YES"){ _, _ ->
                lifecycleScope.launch {
                    panel.manager.setSavedInfo(panel.manager.emptyInfo())
                    requireActivity().onBackPressed()
                }
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

    override fun onDestroy() {
        with(requireActivity() as MainActivity){
            accountsFragment.statusBarColorManager.setCurrent(null)
        }
        super.onDestroy()
    }

}