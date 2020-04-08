package com.example.test3

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity(){

    val scope = CoroutineScope(Job() + Dispatchers.Main)

    var defaultTextColor: Int = 0


    val accountsFragment = AccountsFragment()
    val newsFragment = NewsFragment()
    var activeFragment: Fragment = accountsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        println("main create")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        defaultTextColor = ContextCompat.getColor(this, R.color.textColor)

        supportActionBar?.title = "Competitive Programming & Solving"

        supportFragmentManager.beginTransaction().add(R.id.container_fragment, newsFragment, "news_fragment").hide(newsFragment).commit()
        supportFragmentManager.beginTransaction().add(R.id.container_fragment, accountsFragment, "accounts_fragment").commit()

        navigation.setOnNavigationItemSelectedListener{
            val selectedFragment =
                when(it.itemId){
                    R.id.navigation_accounts -> accountsFragment
                    R.id.navigation_news -> newsFragment
                    else -> throw Exception("unknown selected navigation bar item: ${it.itemId}")
                }

            if(activeFragment!=selectedFragment) {
                println("${it.itemId} selected")
                supportFragmentManager.beginTransaction().hide(activeFragment).show(selectedFragment).commit()
                activeFragment = selectedFragment
            }

            true
        }

    }

    override fun onResume() {
        println("main resume")
        super.onResume()
    }



    override fun onDestroy() {
        println("main destroy")
        scope.cancel()
        super.onDestroy()
    }


    companion object {
        const val CALL_ACCOUNT_SETTINGS = 1
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        println("on result $resultCode")
        if(requestCode == CALL_ACCOUNT_SETTINGS && resultCode == Activity.RESULT_OK){
            val who: String = data!!.getStringExtra("manager")!!
            accountsFragment.panels.first { it.manager.preferences_file_name==who }.show()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

}
