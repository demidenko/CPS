package com.example.test3.contests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.get
import com.example.test3.R
import com.example.test3.ui.CPSFragment
import com.google.android.material.checkbox.MaterialCheckBox
import kotlinx.coroutines.runBlocking

class ContestsSelectPlatformsFragment: CPSFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_contests_platforms, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cpsTitle = "::contests.settings.platforms"

        setHasOptionsMenu(true)

        val listView = view.findViewById<LinearLayout>(R.id.contests_platforms_listview).apply {
            removeAllViews()
        }

        runBlocking {
            view.isEnabled = false
            createPlatformsList(listView)
            view.isEnabled = true
        }
    }

    private suspend fun createPlatformsList(listView: LinearLayout) {
        val enabledPlatforms = requireContext().settingsContests.enabledPlatforms().toMutableSet()
        Contest.Platform.getAll().forEach { platform ->
            layoutInflater.inflate(R.layout.contest_select_platform_item, listView)
            listView[listView.childCount-1].apply {
                findViewById<TextView>(R.id.contest_select_title).text = platform.name
                findViewById<ImageView>(R.id.contest_select_icon).setImageResource(platform.getIcon())
                findViewById<MaterialCheckBox>(R.id.contest_select_checkbox).apply {
                    isSaveEnabled = false
                    isChecked = platform in enabledPlatforms
                    jumpDrawablesToCurrentState()
                    setOnCheckedChangeListener { buttonView, isChecked ->
                        buttonView.isEnabled = false
                        if (isChecked) enabledPlatforms.add(platform)
                        else enabledPlatforms.remove(platform)
                        runBlocking {
                            requireContext().settingsContests.enabledPlatforms(enabledPlatforms)
                        }
                        buttonView.isEnabled = true
                    }
                }
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
        super.onPrepareOptionsMenu(menu)
    }
}