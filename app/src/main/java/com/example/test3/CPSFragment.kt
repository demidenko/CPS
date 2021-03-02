package com.example.test3

import android.os.Bundle
import androidx.fragment.app.Fragment

open class CPSFragment : Fragment() {

    init {
        if(arguments==null) arguments = Bundle()
    }

    companion object {
        private const val keyTitle = "cps_title"
    }

    fun getCPSTitle(): String = requireArguments().getString(keyTitle, "")

    fun setCPSTitle(title: String) {
        requireArguments().putString(keyTitle, title)
    }

    private fun showStuff() {
        //TODO()
        (requireActivity() as MainActivity).setActionBarSubTitle(getCPSTitle())
    }

    private fun hideStuff() {
        //TODO()
    }

    override fun onResume() {
        if(!isHidden) showStuff()
        super.onResume()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if(hidden){
            hideStuff()
        } else {
            showStuff()
        }
    }

}