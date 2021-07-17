package com.example.test3.contests

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.test3.R
import com.example.test3.makeIntentOpenUrl
import com.example.test3.ui.CPSDataStoreDelegate
import com.example.test3.ui.CPSFragment
import com.example.test3.utils.CPSDataStore
import com.example.test3.utils.getStringNotBlank
import com.example.test3.utils.jsonCPS
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.runBlocking

class ContestsSettingsFragment: CPSFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_contests_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cpsTitle = "::contests.settings"
        setHasOptionsMenu(true)

        view.findViewById<TextView>(R.id.contests_settings_clistid).apply {
            setOnClickListener {
                val dialogView = mainActivity.layoutInflater.inflate(R.layout.dialog_clist_api, null)
                val context = requireContext()
                dialogView.findViewById<ImageButton>(R.id.dialog_clist_api_info).setOnClickListener {
                    startActivity(makeIntentOpenUrl("https://clist.by/api/v2/doc/"))
                }
                val loginTextField = dialogView.findViewById<TextInputLayout>(R.id.dialog_clist_api_login).apply {
                    editText?.setText(runBlocking { context.settingsContests.clistApiLogin() ?: "" })
                }
                val apikeyTextField = dialogView.findViewById<TextInputLayout>(R.id.dialog_clist_api_key).apply {
                    editText?.setText(runBlocking { context.settingsContests.clistApiKey() ?: "" })
                }
                AlertDialog.Builder(mainActivity).apply {
                    setView(dialogView)
                    setPositiveButton("Save") { _, i ->
                        runBlocking {
                            context.settingsContests.clistApiLogin(loginTextField.editText?.getStringNotBlank())
                            context.settingsContests.clistApiKey(apikeyTextField.editText?.getStringNotBlank())
                        }
                    }
                }.create().show()
            }
        }

        view.findViewById<TextView>(R.id.contests_settings_platforms).apply {
            setOnClickListener {
                mainActivity.cpsFragmentManager.pushBack(ContestsSelectPlatformsFragment())
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
        super.onPrepareOptionsMenu(menu)
    }

}

val Context.settingsContests by CPSDataStoreDelegate { ContestsSettingsDataStore(it) }

class ContestsSettingsDataStore(context: Context): CPSDataStore(context.contests_settings_dataStore) {

    companion object {
        private val Context.contests_settings_dataStore by preferencesDataStore("contests_settings")
    }

    val enabledPlatforms = jsonCPS.itemStringConvertible<List<Contest.Platform>>("enabled_platforms", emptyList())
    val clistApiLogin = ItemNullable(stringPreferencesKey("clist_api_login"))
    val clistApiKey = ItemNullable(stringPreferencesKey("clist_api_key"))

    suspend fun getClistApiLoginAndKey(): Pair<String,String>? {
        val login = clistApiLogin() ?: return null
        val key = clistApiKey() ?: return null
        return login to key
    }

}