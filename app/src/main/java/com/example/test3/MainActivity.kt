package com.example.test3

import android.annotation.SuppressLint
import android.app.AlertDialog
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.example.test3.account_manager.AccountManager
import com.example.test3.account_manager.UserInfo
import com.example.test3.job_services.JobServicesCenter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlin.coroutines.suspendCoroutine


class MainActivity : AppCompatActivity(){

    val scope = CoroutineScope(Job() + Dispatchers.Main)

    val defaultTextColor by lazy { ContextCompat.getColor(this, R.color.textColor) }


    val accountsFragment: AccountsFragment by lazy { supportFragmentManager.fragments.find { it is AccountsFragment } as? AccountsFragment ?: AccountsFragment() }
    val newsFragment: NewsFragment by lazy { supportFragmentManager.fragments.find { it is NewsFragment } as? NewsFragment ?: NewsFragment() }
    val devFragment: TestFragment by lazy { supportFragmentManager.fragments.find { it is TestFragment } as? TestFragment ?: TestFragment() }


    override fun onCreate(savedInstanceState: Bundle?) {
        println("main create")
        println(savedInstanceState)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        setupActionBar()
        setActionBarTitle("Competitive Programming && Solving") //"Compete, Program, Solve"


        fun navigationSelectUpdateUI(fragment: Fragment){
            navigation_accounts.visibility = if(fragment == accountsFragment) View.VISIBLE else View.GONE
            navigation_news.visibility = if(fragment == newsFragment) View.VISIBLE else View.GONE
            navigation_develop.visibility = if(fragment == devFragment) View.VISIBLE else View.GONE

            setActionBarSubTitle(getFragmentSubTitle(fragment))
        }

        setFragmentSubTitle(accountsFragment, "::accounts")
        setFragmentSubTitle(newsFragment, "::news")
        setFragmentSubTitle(devFragment, "::develop")

        var activeFragment = supportFragmentManager.fragments.find { !it.isHidden } ?: accountsFragment
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


        PreferenceManager.setDefaultValues(this, R.xml.news_preferences, false)

        lifecycleScope.launchWhenStarted {
            JobServicesCenter.startJobServices(this@MainActivity)
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
        menu?.getItem(0)?.isChecked = getUseRealColors()
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
            R.id.app_info -> {
                AlertDialog.Builder(this)
                    .setTitle("CPS")
                    .setMessage("version = ${BuildConfig.VERSION_NAME}")
                    .create()
                    .show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    suspend fun chooseUserID(manager: AccountManager) = chooseUserID(manager.getSavedInfo(), manager)

    suspend fun chooseUserID(initialUserInfo: UserInfo, manager: AccountManager): UserInfo? {
        return withContext(scope.coroutineContext) {
            suspendCoroutine { cont ->
                val dialog = DialogAccountChooser(initialUserInfo, manager, cont)
                dialog.show(supportFragmentManager, "account_choose")
            }
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        println("main on save bundle")
        super.onSaveInstanceState(outState)
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
        scope.cancel()
        super.onDestroy()
    }


    override fun onBackPressed() {
        super.onBackPressed()
        supportFragmentManager.fragments.forEach {
            if(it.isVisible) setActionBarSubTitle(getFragmentSubTitle(it))
        }
    }


}

fun getFragmentSubTitle(fragment: Fragment): String = fragment.arguments?.getString("subtitle",null)?:""

fun setFragmentSubTitle(fragment: Fragment, title: String){
    if(fragment.arguments == null) fragment.arguments = Bundle()
    fragment.arguments?.putString("subtitle",title)
}

fun getColorFromResource(context: Context, resourceId: Int): Int {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        context.resources.getColor(resourceId, null)
    } else {
        context.resources.getColor(resourceId)
    }
}