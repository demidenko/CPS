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
