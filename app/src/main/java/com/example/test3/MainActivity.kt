package com.example.test3

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity(){

    val scope = CoroutineScope(Job() + Dispatchers.Main)

    var defaultTextColor: Int = 0


    val accountsFragment = AccountsFragment()
    val newsFragment = NewsFragment()
    lateinit var activeFragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        println("main create")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        defaultTextColor = ContextCompat.getColor(this, R.color.textColor)

        supportActionBar?.title = "Competitive Programming & Solving"

        activeFragment = accountsFragment
        supportFragmentManager.beginTransaction().add(R.id.container_fragment, activeFragment).commit()

        navigation.setOnNavigationItemSelectedListener(object : BottomNavigationView.OnNavigationItemSelectedListener{
            val addedFragments = mutableSetOf(navigation.menu.getItem(0).itemId)
            override fun onNavigationItemSelected(item: MenuItem): Boolean {
                val id = item.itemId
                val selectedFragment =
                    when(id){
                        R.id.navigation_accounts -> accountsFragment
                        R.id.navigation_news -> newsFragment
                        else -> throw Exception("unknown selected navigation bar item: $id")
                    }

                if(selectedFragment!=activeFragment) {
                    println("$id selected")
                    supportFragmentManager.beginTransaction().hide(activeFragment).run {
                        if(addedFragments.contains(id)) show(selectedFragment)
                        else{
                            addedFragments.add(id)
                            add(R.id.container_fragment, selectedFragment)
                        }
                    }.commit()
                    activeFragment = selectedFragment
                }

                return true
            }

        })

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
