package com.example.test3

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.lifecycle.lifecycleScope
import com.example.test3.account_manager.AccountManager
import com.example.test3.account_manager.UserInfo
import com.example.test3.workers.WorkersCenter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlin.coroutines.suspendCoroutine


class MainActivity : AppCompatActivity(){

    val defaultTextColor by lazy { getColorFromResource(this, R.color.textColor) }


    val accountsFragment: AccountsFragment by lazy { supportFragmentManager.fragments.find { it is AccountsFragment } as? AccountsFragment ?: AccountsFragment() }
    val newsFragment: NewsFragment by lazy { supportFragmentManager.fragments.find { it is NewsFragment } as? NewsFragment ?: NewsFragment() }
    val devFragment: TestFragment by lazy { supportFragmentManager.fragments.find { it is TestFragment } as? TestFragment ?: TestFragment() }


    override fun onCreate(savedInstanceState: Bundle?) {
        println("main create")
        super.onCreate(null)

        setContentView(R.layout.activity_main)

        setupActionBar()
        setActionBarTitle("Competitive Programming && Solving") //"Compete, Program, Solve"

        settingsDev.getDevEnabledLiveData().observe(this){ isChecked ->
            val item = navigation_main.menu.findItem(R.id.navigation_develop)
            item.isVisible = isChecked
        }

        fun navigationSelectUpdateUI(fragment: CPSFragment){
            navigation_accounts.visibility = if(fragment == accountsFragment) View.VISIBLE else View.GONE
            navigation_news.visibility = if(fragment == newsFragment) View.VISIBLE else View.GONE
            navigation_develop.visibility = if(fragment == devFragment) View.VISIBLE else View.GONE
        }

        accountsFragment.setCPSTitle("::accounts")
        newsFragment.setCPSTitle("::news")
        devFragment.setCPSTitle("::develop")

        var activeFragment = supportFragmentManager.fragments.find { !it.isHidden } as? CPSFragment ?: accountsFragment
        if(!activeFragment.isAdded) supportFragmentManager.beginTransaction().add(R.id.container_fragment, activeFragment).commit()
        navigationSelectUpdateUI(activeFragment)

        navigation_main.setOnNavigationItemSelectedListener { item ->
            val id = item.itemId

            val selectedFragment =
                when(id){
                    R.id.navigation_accounts -> accountsFragment
                    R.id.navigation_news -> newsFragment
                    R.id.navigation_develop -> devFragment
                    else -> throw Exception("unknown selected navigation bar item: $id")
                }

            if(selectedFragment!=activeFragment) {
                println("$id selected")
                supportFragmentManager.beginTransaction().hide(activeFragment).run {
                    if(selectedFragment.isAdded) show(selectedFragment)
                    else add(R.id.container_fragment, selectedFragment)
                }.commit()
                activeFragment = selectedFragment
                navigationSelectUpdateUI(selectedFragment)
            }

            true
        }


        lifecycleScope.launchWhenStarted {
            WorkersCenter.startWorkers(this@MainActivity)
        }
    }

    private fun setupActionBar(){
        supportActionBar?.run {
            setDisplayShowCustomEnabled(true)
            setDisplayShowTitleEnabled(false)
            setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM)
            setCustomView(R.layout.action_bar)
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
        menu?.findItem(R.id.color_switcher)?.isChecked = getUseRealColors()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.color_switcher -> {
                val use = !item.isChecked
                item.isChecked = use
                lifecycleScope.launch {
                    setUseRealColors(use)
                }
            }
            R.id.app_info -> AboutDialog.showDialog(this)
        }
        return super.onOptionsItemSelected(item)
    }

    suspend fun chooseUserID(manager: AccountManager) = chooseUserID(manager.getSavedInfo(), manager)

    suspend fun chooseUserID(initialUserInfo: UserInfo, manager: AccountManager): UserInfo? {
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
        super.onBackPressed()
    }


}


fun getColorFromResource(context: Context, resourceId: Int): Int {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        context.resources.getColor(resourceId, null)
    } else {
        context.resources.getColor(resourceId)
    }
}