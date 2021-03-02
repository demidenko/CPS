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


class CPSFragmentManager(activity: MainActivity, private val containerId: Int) {

    class CPSException(str: String): Exception("CPS Fragments: $str")

    private val stacks = mutableMapOf<Int, MutableList<CPSFragment>>()
    private var stacksCount = 0
    private var currentStackId = -1



    private val fragmentManager = activity.supportFragmentManager.apply {
        //TODO init stacks on restore
        fragments.filterIsInstance<CPSFragment>().forEach {
        }
    }

    fun switchToStack(stackId: Int) {
        if(stackId == currentStackId) return
        val targetLine = stacks[stackId] ?: throw CPSException("stack $stackId is not exist")
        val transaction = fragmentManager.beginTransaction()
        if(currentStackId!=-1){
            val currentLine = stacks[currentStackId]!!
            transaction.hide(currentLine.last())
        }
        with(targetLine.last()){
            if(isAdded) transaction.show(this)
            else transaction.add(containerId, this)
        }
        transaction.commit()
        currentStackId = stackId
    }

    fun createStack(rootFragment: CPSFragment): Int {
        val stackId = stacksCount
        stacksCount+=1
        stacks[stackId] = mutableListOf(rootFragment)
        return stackId
    }

    fun createStackAndSwitch(rootFragment: CPSFragment): Int {
        val stackId = createStack(rootFragment)
        switchToStack(stackId)
        return stackId
    }

    fun currentIsRoot(): Boolean {
        return stacks[currentStackId]!!.size == 1
    }

    fun pushBack(fragment: CPSFragment) {
        val currentLine = stacks[currentStackId]!!
        val transaction = fragmentManager.beginTransaction().hide(currentLine.last()).add(containerId, fragment)
        currentLine.add(fragment)
        transaction.commit()
    }

    fun backPressed() {
        val currentLine = stacks[currentStackId] ?: return
        val transaction = fragmentManager.beginTransaction().remove(currentLine.removeLast())
        if(currentLine.isNotEmpty()) transaction.show(currentLine.last())
        transaction.commit()
    }

}