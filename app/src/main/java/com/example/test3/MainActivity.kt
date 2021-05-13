package com.example.test3

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.lifecycle.lifecycleScope
import com.example.test3.account_manager.AccountManager
import com.example.test3.account_manager.UserInfo
import com.example.test3.ui.*
import com.example.test3.utils.getColorFromResource
import com.example.test3.utils.onIff
import com.example.test3.workers.WorkersCenter
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.suspendCoroutine


class MainActivity : AppCompatActivity(){

    val defaultTextColor by lazy { getColorFromResource(this, R.color.textColor) }

    private inline fun<reified T: CPSFragment> findOrCreateFragment(create: () -> T): T {
        return supportFragmentManager.fragments.firstNotNullOfOrNull { it as? T } ?: create()
    }

    val accountsFragment by lazy { findOrCreateFragment { AccountsFragment() } }
    val newsFragment by lazy { findOrCreateFragment { NewsFragment() } }
    val devFragment by lazy { findOrCreateFragment { TestFragment() } }

    val cpsFragmentManager by lazy { CPSFragmentManager(this, R.id.container_fragment) }

    val navigation: LinearLayout by lazy { findViewById(R.id.navigation) }
    val navigationMain: BottomNavigationView by lazy { navigation.findViewById(R.id.navigation_main) }
    val navigationSupport: LinearLayout by lazy { navigation.findViewById(R.id.support_navigation) }

    val progressBarHolder: LinearLayout by lazy { findViewById(R.id.progress_bar_holder) }

    override fun onCreate(savedInstanceState: Bundle?) {
        println("main create")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupUIMode()
        setupActionBar()
        setActionBarTitle("Competitive Programming && Solving")
        savedInstanceState?.let {
            showUISettingsPanel = it.getBoolean(keyShowUIPanel)
        }

        addRepeatingJob(Lifecycle.State.STARTED){
            settingsDev.getDevEnabledFlow().collect { isChecked ->
                val item = navigationMain.menu.findItem(R.id.navigation_develop)
                item.isVisible = isChecked
            }
        }

        val accountsStackId = cpsFragmentManager.getOrCreateStack(accountsFragment)
        val newsStackId = cpsFragmentManager.getOrCreateStack(newsFragment)
        val devStackId = cpsFragmentManager.getOrCreateStack(devFragment)

        if(cpsFragmentManager.getCurrentStackId() == -1) cpsFragmentManager.switchToStack(accountsStackId)

        navigationMain.setOnNavigationItemSelectedListener { item ->
            val selectedStackId =
                when(val id = item.itemId){
                    R.id.navigation_accounts -> accountsStackId
                    R.id.navigation_news -> newsStackId
                    R.id.navigation_develop -> devStackId
                    else -> throw Exception("unknown selected navigation bar item: $id")
                }
            cpsFragmentManager.switchToStack(selectedStackId)
            true
        }

        lifecycleScope.launchWhenStarted {
            WorkersCenter.startWorkers(this@MainActivity)
        }
    }

    private var showUISettingsPanel: Boolean = false
        set(value) {
            field = value

            supportActionBar?.run {
                val titles: LinearLayout = findViewById(R.id.action_bar_titles)
                val panelUI: ConstraintLayout = findViewById(R.id.action_bar_ui_panel)
                titles.isVisible = !value
                panelUI.isVisible = value
            }
        }

    companion object {
        private const val keyShowUIPanel = "show_ui_panel"
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(keyShowUIPanel, showUISettingsPanel)
    }


    private fun setupActionBar(){
        supportActionBar?.run {
            setDisplayShowCustomEnabled(true)
            setDisplayShowTitleEnabled(false)
            setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM)
            setCustomView(R.layout.action_bar)
            customView?.apply {

                findViewById<ImageButton>(R.id.button_ui_close).apply {
                    setOnClickListener { showUISettingsPanel = false }
                }

                val buttonOriginColors = findViewById<ImageButton>(R.id.button_origin_colors).apply {
                    setOnClickListener {
                        lifecycleScope.launch {
                            val current = getUseRealColors()
                            setUseRealColors(!current)
                        }
                    }
                }

                val buttonColoredStatusBar = findViewById<ImageButton>(R.id.button_colored_status_bar).apply {
                    setOnClickListener {
                        lifecycleScope.launch {
                            val current = settingsUI.getUseStatusBar()
                            settingsUI.setUseStatusBar(!current)
                        }
                    }
                }

                findViewById<ImageButton>(R.id.button_ui_mode).apply {
                    setOnClickListener {
                        when(getUIMode()){
                            UIMode.DARK -> setUIMode(UIMode.LIGHT)
                            UIMode.LIGHT -> setUIMode(UIMode.DARK)
                        }
                    }
                }

                val buttonRecreate = findViewById<ImageButton>(R.id.button_recreate_app).apply {
                    setOnClickListener { recreate() }
                }

                addRepeatingJob(Lifecycle.State.STARTED){
                    listOf(
                        settingsUI.getUseRealColorsFlow().onEach { use -> buttonOriginColors.onIff(use) },
                        settingsUI.getUseStatusBarFlow().onEach { use -> buttonColoredStatusBar.onIff(use) },
                        settingsDev.getDevEnabledFlow().onEach { enabled -> buttonRecreate.isVisible = enabled }
                    ).forEach { it.launchIn(this) }
                }
            }
        }
    }



    fun setActionBarSubTitle(text: String) {
        supportActionBar?.customView?.let{
            it.findViewById<TextView>(R.id.action_bar_subtitle).text = text
        }
    }

    fun setActionBarTitle(text: String) {
        supportActionBar?.customView?.let{
            it.findViewById<TextView>(R.id.action_bar_title).text = text
        }
    }

    fun showToast(title: String){
        Toast.makeText(this, title, Toast.LENGTH_LONG).show()
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        (menu as? MenuBuilder)?.setOptionalIconsVisible(true)
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.settings_ui -> {
                showUISettingsPanel = true
            }
            R.id.app_info -> AboutDialog.showDialog(this)
        }
        return super.onOptionsItemSelected(item)
    }

    suspend fun<U: UserInfo> chooseUserIDFromSaved(manager: AccountManager<U>) = chooseUserID(manager.getSavedInfo(), manager)
    suspend fun<U: UserInfo> chooseUserID(manager: AccountManager<U>) = chooseUserID(manager.emptyInfo(), manager)

    private suspend fun<U: UserInfo> chooseUserID(initialUserInfo: U, manager: AccountManager<U>): U? {
        return withContext(lifecycleScope.coroutineContext) {
            suspendCoroutine { cont ->
                val dialog = DialogAccountChooser(initialUserInfo, manager, cont)
                dialog.show(supportFragmentManager, "account_choose")
            }
        }
    }

    override fun onResume() {
        println("main resume")
        super.onResume()
    }

    override fun onStop() {
        println("main stop")
        super.onStop()
    }

    override fun onDestroy() {
        println("main destroy")
        super.onDestroy()
    }


    override fun onBackPressed() {
        if(cpsFragmentManager.currentIsRoot()) super.onBackPressed()
        else cpsFragmentManager.backPressed()
    }


}
