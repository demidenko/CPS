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
import androidx.datastore.preferences.core.edit
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

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

    private val KEY_ENABLED_PLATFORMS get() = stringPreferencesKey("enabled_platforms")

    val clistApiLogin = ItemNullable(stringPreferencesKey("clist_api_login"))
    val clistApiKey = ItemNullable(stringPreferencesKey("clist_api_key"))

    suspend fun getClistApiLoginAndKey(): Pair<String,String>? {
        val login = clistApiLogin() ?: return null
        val key = clistApiKey() ?: return null
        return login to key
    }

    fun flowOfEnabledPlatforms(): Flow<List<Contest.Platform>> = dataStore.data.map {
        val str = it[KEY_ENABLED_PLATFORMS] ?: return@map emptyList()
        jsonCPS.decodeFromString(str)
    }
    suspend fun getEnabledPlatforms(): List<Contest.Platform> = flowOfEnabledPlatforms().first()
    suspend fun setEnabledPlatforms(platforms: List<Contest.Platform>) {
        dataStore.edit {
            it[KEY_ENABLED_PLATFORMS] = jsonCPS.encodeToString(platforms)
        }
    }

}