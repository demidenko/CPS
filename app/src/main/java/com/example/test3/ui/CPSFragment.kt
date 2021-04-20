package com.example.test3.ui

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.test3.MainActivity

open class CPSFragment : Fragment() {

    init {
        if(arguments==null) arguments = Bundle()
    }

    val mainActivity get() = requireActivity() as MainActivity

    companion object {
        private const val keyTitle = "cps_title"
        private const val keyId = "cps_id"
        private const val keyParentId = "cps_parent_id"
        private const val keyBottomPanelId = "cps_bottom_panel_id"
        private const val keyBottomPanelLayoutId = "cps_bottom_panel_layout_id"
    }

    var cpsTitle: String
        get() = requireArguments().getString(keyTitle, "")
        set(title) {
            requireArguments().putString(keyTitle, title)
            if(isVisible) showSubTitle()
        }

    var cpsId: Int
        get() = requireArguments().getInt(keyId)
        set(value) {
            requireArguments().putInt(keyId, value)
        }

    var cpsParentId: Int
        get() = requireArguments().getInt(keyParentId)
        set(value) {
            requireArguments().putInt(keyParentId, value)
        }

    private val bottomPanel: ConstraintLayout? by lazy {
        with(requireArguments()){
            if(!containsKey(keyBottomPanelId)) null
            else{
                val id = getInt(keyBottomPanelId)
                mainActivity.navigationSupport.findViewById<ConstraintLayout>(id)
                    ?: mainActivity.layoutInflater.inflate(getInt(keyBottomPanelLayoutId), mainActivity.navigationSupport)
                        .findViewById<ConstraintLayout>(id).apply { isGone = true }
            }
        }
    }

    fun requireBottomPanel(): ConstraintLayout {
        return bottomPanel ?: throw CPSFragmentException("bottom panel is not defined")
    }

    fun setBottomPanelId(id: Int, @LayoutRes layoutId: Int) {
        requireArguments().apply {
            putInt(keyBottomPanelId, id)
            putInt(keyBottomPanelLayoutId, layoutId)
        }
    }

    private fun showSubTitle() {
        mainActivity.setActionBarSubTitle(cpsTitle)
    }

    private fun showStuff() {
        showSubTitle()
        mainActivity.navigation.isVisible = bottomPanel?.let {
                it.isVisible = true
                true
            } ?: false
    }

    private fun hideStuff() {
        bottomPanel?.isVisible = false
    }

    override fun onResume() {
        if(!isHidden) showStuff()
        super.onResume()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if(hidden) hideStuff()
        else showStuff()
    }

    override fun onDetach() {
        bottomPanel?.let {
            mainActivity.navigationSupport.removeView(it)
        }
        super.onDetach()
    }

}


class CPSFragmentManager(activity: MainActivity, private val containerId: Int) {

    private val stacks = mutableMapOf<Int, MutableList<CPSFragment>>()
    private var lastStackId = -1
    private var currentStackId = -1

    private var lastFragmentId = -1


    private val fragmentManager = activity.supportFragmentManager.apply {
        val idMap = mutableMapOf<Int, CPSFragment>()
        val roots = mutableListOf<CPSFragment>()
        val next = mutableMapOf<Int, Int>()
        fragments.filterIsInstance<CPSFragment>().forEach { fragment ->
            val id = fragment.cpsId
            idMap[id] = fragment
            lastFragmentId = maxOf(lastFragmentId, id)
            val parentId = fragment.cpsParentId
            if(parentId < 0) roots.add(fragment)
            else next[parentId] = id
        }
        roots.forEach { rootFragment ->
            val stackId = -(rootFragment.cpsParentId+1)
            lastStackId = maxOf(lastStackId, stackId)
            val stack = mutableListOf(rootFragment)
            var fragment = rootFragment
            while(true){
                if(!fragment.isHidden) currentStackId = stackId
                val nextId = next[fragment.cpsId] ?: break
                fragment = idMap[nextId]!!
                stack.add(fragment)
            }
            stacks[stackId] = stack
        }
    }

    fun getCurrentStackId() = currentStackId

    fun switchToStack(stackId: Int) {
        if(stackId == currentStackId) return
        val targetLine = stacks[stackId] ?: throw CPSFragmentException("stack $stackId is not exist")
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
        lastStackId+=1
        val stackId = lastStackId
        lastFragmentId+=1
        val fragmentId = lastFragmentId
        rootFragment.cpsId = fragmentId
        rootFragment.cpsParentId = -(stackId+1)
        stacks[stackId] = mutableListOf(rootFragment)
        return stackId
    }

    fun getOrCreateStack(fragment: CPSFragment): Int {
        for((stackId, list) in stacks){
            if(list.contains(fragment)) return stackId
        }
        return createStack(fragment)
    }

    fun currentIsRoot(): Boolean {
        return stacks[currentStackId]!!.size == 1
    }

    fun pushBack(fragment: CPSFragment) {
        lastFragmentId+=1
        fragment.cpsId = lastFragmentId
        val currentLine = stacks[currentStackId]!!
        val transaction = fragmentManager.beginTransaction()
            .hide(currentLine.last().also { fragment.cpsParentId = it.cpsId })
            .add(containerId, fragment)
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

class CPSFragmentException(str: String): Exception("CPS Fragments: $str")