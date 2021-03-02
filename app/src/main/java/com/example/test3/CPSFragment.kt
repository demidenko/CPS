package com.example.test3

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_main.*

open class CPSFragment : Fragment() {

    init {
        if(arguments==null) arguments = Bundle()
    }

    companion object {
        private const val keyTitle = "cps_title"
        private const val keyBottomPanelId = "cps_bottom_panel_id"
    }

    private fun getCPSTitle(): String = requireArguments().getString(keyTitle, "")

    fun setCPSTitle(title: String) {
        requireArguments().putString(keyTitle, title)
    }

    private val bottomPanel: ConstraintLayout? by lazy {
        with(requireArguments()){
            if(!containsKey(keyBottomPanelId)) null
            else getInt(keyBottomPanelId).let {
                (requireActivity() as MainActivity).support_navigation.findViewById<ConstraintLayout>(it)
            }
        }
    }

    fun setBottomPanelId(id: Int) {
        requireArguments().putInt(keyBottomPanelId, id)
    }

    private fun showStuff() {
        val mainActivity = (requireActivity() as MainActivity)
        mainActivity.setActionBarSubTitle(getCPSTitle())
        mainActivity.navigation.visibility = bottomPanel?.let {
                it.visibility = View.VISIBLE
                View.VISIBLE
            } ?: View.GONE
    }

    private fun hideStuff() {
        bottomPanel?.let {
            it.visibility = View.GONE
        }
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