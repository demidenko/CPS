package com.example.test3

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.TypefaceSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.color
import androidx.fragment.app.Fragment
import com.example.test3.account_manager.ColoredHandles
import com.example.test3.account_manager.HandleColor
import com.example.test3.account_manager.useRealColors
import com.example.test3.job_services.JobServicesCenter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel


class MainActivity : AppCompatActivity(){

    val scope = CoroutineScope(Job() + Dispatchers.Main)

    val defaultTextColor by lazy { ContextCompat.getColor(this, R.color.textColor) }


    val accountsFragment: AccountsFragment by lazy {
        supportFragmentManager.fragments.find { it is AccountsFragment } as? AccountsFragment
            ?: AccountsFragment().apply { arguments = Bundle().apply { putString("subtitle", "::accounts") } }
    }
    val newsFragment: NewsFragment by lazy {
        supportFragmentManager.fragments.find { it is NewsFragment } as? NewsFragment
            ?: NewsFragment().apply { arguments = Bundle().apply { putString("subtitle", "::news") } }
    }
    val testFragment: TestFragment by lazy {
        supportFragmentManager.fragments.find { it is TestFragment } as? TestFragment
            ?: TestFragment().apply { arguments = Bundle().apply { putString("subtitle", "::develop") } }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        println("main create")
        println(savedInstanceState)
        super.onCreate(savedInstanceState)

        NotificationChannels.createNotificationChannels(this)
        JobServicesCenter.startJobServices(this)

        setContentView(R.layout.activity_main)

        supportActionBar?.title = "Competitive Programming & Solving" //"Compete, Program, Solve"


        fun navigationSelectUpdateUI(fragment: Fragment){

            setActionBarSubTitle(fragment.arguments?.getString("subtitle", null) ?: "")

            when(fragment){
                accountsFragment -> {
                    navigation_news.visibility = View.GONE
                    navigation_dev.visibility = View.GONE
                    navigation_accounts.visibility = View.VISIBLE
                }
                newsFragment -> {
                    navigation_accounts.visibility = View.GONE
                    navigation_dev.visibility = View.GONE
                    navigation_news.visibility = View.VISIBLE
                }
                testFragment -> {
                    navigation_accounts.visibility = View.GONE
                    navigation_news.visibility = View.GONE
                    navigation_dev.visibility = View.VISIBLE
                }
            }
        }

        var activeFragment = supportFragmentManager.fragments.find { it.isVisible } ?: accountsFragment
        if(!activeFragment.isAdded) supportFragmentManager.beginTransaction().add(R.id.container_fragment, activeFragment).commit()
        navigationSelectUpdateUI(activeFragment)

        navigation_main.setOnNavigationItemSelectedListener { item ->
            val id = item.itemId

            val selectedFragment =
                when(id){
                    R.id.navigation_accounts -> accountsFragment
                    R.id.navigation_news -> newsFragment
                    R.id.navigation_develop -> testFragment
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


        with(getPreferences(Context.MODE_PRIVATE)){
            useRealColors = getBoolean(use_real_colors, false)
        }

        configureNavigation()

    }

    fun configureNavigation(){

        navigation_accounts.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.navigation_accounts_reload -> accountsFragment.reloadAccounts()
                R.id.navigation_accounts_add -> accountsFragment.addAccount()
            }
            true
        }

        navigation_news.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.navigation_news_reload -> newsFragment.reloadTabs()
                R.id.navigation_news_settings -> {
                    supportFragmentManager.beginTransaction()
                        .hide(newsFragment)
                        .add(android.R.id.content, SettingsNewsFragment())
                        .addToBackStack(null)
                        .commit()

                    setActionBarSubTitle("::news.settings")
                }
            }
            true
        }
    }

    fun setActionBarSubTitle(text: String) {
        supportActionBar?.subtitle = SpannableStringBuilder().apply {
            append(text)
            setSpan(TypefaceSpan("monospace"), 0, length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.color_switcher -> {
                if(testFragment.isVisible){
                    showColorsTable()
                    return true
                }

                useRealColors = !useRealColors
                accountsFragment.panels.forEach { it.show() }
                if(newsFragment.isAdded) newsFragment.refresh()

                with(getPreferences(Context.MODE_PRIVATE).edit()){
                    putBoolean(use_real_colors, useRealColors)
                    apply()
                }
            }
            R.id.app_info -> {
                AlertDialog.Builder(this)
                    .setTitle("CPS")
                    .setMessage("version ${BuildConfig.VERSION_NAME}")
                    .create()
                    .show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun showColorsTable(){

        val table = findViewById<LinearLayout>(R.id.table_handle_colors)
        table.removeAllViews()

        fun addRow(row: ArrayList<CharSequence>) {
            val l = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.forEach { s->
                l.addView(
                    TextView(this).apply{ text = s },
                    LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT).apply { weight = 1f }
                )
            }
            table.addView(l)
        }

        addRow(arrayListOf(
            "app",
            "Codeforces",
            "AtCoder",
            "Topcoder"
        ))

        val backup = useRealColors
        for(color in HandleColor.values()){
            val row = arrayListOf<CharSequence>()

            useRealColors = false
            row.add(
                SpannableStringBuilder().bold {
                    color(color.getARGB(accountsFragment.codeforcesAccountManager)) { append(color.name) }
                }
            )
            useRealColors = true
            arrayOf<ColoredHandles>(
                accountsFragment.codeforcesAccountManager,
                accountsFragment.atcoderAccountManager,
                accountsFragment.topcoderAccountManager
            ).forEach {
                val s = SpannableStringBuilder().bold {
                    try {
                        color(color.getARGB(it)) { append(color.name) }
                    } catch (e: HandleColor.UnknownHandleColorException) {
                        append("")
                    }
                }
                row.add(s)
            }
            addRow(row)
        }

        useRealColors = backup
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


    companion object {
        const val use_real_colors = "use_real_colors"

        const val CALL_ACCOUNT_SETTINGS = 1
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        println("on result $resultCode")
        if(requestCode == CALL_ACCOUNT_SETTINGS && resultCode == Activity.RESULT_OK){
            val who: String = data!!.getStringExtra("manager")!!
            accountsFragment.panels.first { it.manager.PREFERENCES_FILE_NAME==who }.show()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }



}

