package com.example.test3

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.color
import androidx.fragment.app.Fragment
import com.example.test3.account_manager.ColoredHandles
import com.example.test3.account_manager.HandleColor
import com.example.test3.account_manager.useRealColors
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel


class MainActivity : AppCompatActivity(){

    val scope = CoroutineScope(Job() + Dispatchers.Main)

    var defaultTextColor: Int = 0


    val accountsFragment = AccountsFragment()
    val newsFragment = NewsFragment()
    val testFragment = TestFragment()
    lateinit var activeFragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        println("main create")
        super.onCreate(savedInstanceState) //TODO
        setContentView(R.layout.activity_main)


        defaultTextColor = ContextCompat.getColor(this, R.color.textColor)

        supportActionBar?.title = "Competitive Programming & Solving"

        activeFragment = accountsFragment
        supportFragmentManager.beginTransaction().add(R.id.container_fragment, activeFragment).commit()

        navigation.setOnNavigationItemSelectedListener { item ->
            val id = item.itemId
            val selectedFragment =
                when(id){
                    R.id.navigation_accounts -> accountsFragment
                    R.id.navigation_news -> newsFragment
                    R.id.navigation_test -> testFragment
                    else -> throw Exception("unknown selected navigation bar item: $id")
                }

            if(selectedFragment!=activeFragment) {
                println("$id selected")
                supportFragmentManager.beginTransaction().hide(activeFragment).run {
                    if(selectedFragment.isAdded) show(selectedFragment)
                    else add(R.id.container_fragment, selectedFragment)
                }.commit()
                activeFragment = selectedFragment
            }

            true
        }

        with(getPreferences(Context.MODE_PRIVATE)){
            useRealColors = getBoolean(use_real_colors, false)
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
                    val s = SpannableStringBuilder("colors example\n")
                    val backup = useRealColors
                    for(color in HandleColor.values()){
                        s.bold {
                            useRealColors = false
                            color(color.getARGB(accountsFragment.codeforcesAccountManager)) { append(color.name+" ") }
                            useRealColors = true
                            arrayOf(
                                accountsFragment.codeforcesAccountManager,
                                accountsFragment.atcoderAccountManager,
                                accountsFragment.topcoderAccountManager
                            ).forEach {
                                try {
                                    color(color.getARGB(it as ColoredHandles)) { append(color.name + " ") }
                                }catch (e: HandleColor.UnknownHandleColorException){
                                    color(defaultTextColor){ append("--- ") }
                                }
                            }
                            append("\n")
                        }
                    }
                    testFragment.textView.text = s
                    useRealColors = backup
                    return true
                }

                useRealColors = !useRealColors
                accountsFragment.panels.forEach { it.show() }
                newsFragment.refresh()

                with(getPreferences(Context.MODE_PRIVATE).edit()){
                    putBoolean(use_real_colors, useRealColors)
                    apply()
                }

                return true
            }
        }
        return super.onOptionsItemSelected(item)
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
