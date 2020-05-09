package com.example.test3

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.test3.account_manager.*
import kotlinx.coroutines.launch
import java.util.*

class AccountsFragment: Fragment() {

    private lateinit var buttonReload: Button

    lateinit var codeforcesAccountManager: CodeforcesAccountManager
    lateinit var atcoderAccountManager: AtCoderAccountManager
    lateinit var topcoderAccountManager: TopCoderAccountManager
    lateinit var acmpAccountManager: ACMPAccountManager

    lateinit var panels: List<AccountPanel>
    lateinit var codeforcesPanel: AccountPanel
    lateinit var atcoderPanel: AccountPanel
    lateinit var topcoderPanel: AccountPanel
    lateinit var acmpPanel: AccountPanel



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        println("fragment accounts onCreateView $savedInstanceState")
        val view = inflater.inflate(R.layout.fragment_accounts, container, false)
        val activity = requireActivity() as MainActivity


        codeforcesAccountManager = CodeforcesAccountManager(activity)
        codeforcesPanel = object : AccountPanel(activity, codeforcesAccountManager){
            override fun show(info: UserInfo) { info as CodeforcesAccountManager.CodeforcesUserInfo
                textMain.text = codeforcesAccountManager.makeSpan(info)
                textAdditional.text = ""
                val color = manager.getColor(info)
                if(info.status == STATUS.OK){
                    textMain.typeface = Typeface.DEFAULT_BOLD
                    textAdditional.text = if(info.rating == NOT_RATED) "[not rated]" else "${info.rating}"
                }else{
                    textMain.typeface = Typeface.DEFAULT
                }
                textMain.setTextColor(color ?: activity.defaultTextColor)
                textAdditional.setTextColor(color ?: activity.defaultTextColor)
                activity.window.statusBarColor = color ?: Color.TRANSPARENT
            }
        }

        atcoderAccountManager = AtCoderAccountManager(activity)
        atcoderPanel = object : AccountPanel(activity, atcoderAccountManager){
            override fun show(info: UserInfo) { info as AtCoderAccountManager.AtCoderUserInfo
                textMain.text = info.handle
                textAdditional.text = ""
                val color = manager.getColor(info)
                if(info.status == STATUS.OK){
                    textMain.typeface = Typeface.DEFAULT_BOLD
                    textAdditional.text = if(info.rating == NOT_RATED) "[not rated]" else "${info.rating}"
                }else{
                    textMain.typeface = Typeface.DEFAULT
                }
                textMain.setTextColor(color ?: activity.defaultTextColor)
                textAdditional.setTextColor(color ?: activity.defaultTextColor)
            }
        }

        topcoderAccountManager = TopCoderAccountManager(activity)
        topcoderPanel = object : AccountPanel(activity, topcoderAccountManager){
            override fun show(info: UserInfo) { info as TopCoderAccountManager.TopCoderUserInfo
                textMain.text = info.handle
                textAdditional.text = ""
                val color = manager.getColor(info)
                if(info.status == STATUS.OK){
                    textMain.typeface = Typeface.DEFAULT_BOLD
                    textAdditional.text = if(info.rating_algorithm == NOT_RATED) "[not rated]" else "${info.rating_algorithm}"
                }else{
                    textMain.typeface = Typeface.DEFAULT
                }
                textMain.setTextColor(color ?: activity.defaultTextColor)
                textAdditional.setTextColor(color ?: activity.defaultTextColor)
            }
        }

        acmpAccountManager = ACMPAccountManager(activity)
        acmpPanel = object : AccountPanel(activity, acmpAccountManager){
            override fun show(info: UserInfo) { info as ACMPAccountManager.ACMPUserInfo
                with(info){
                    if (status == STATUS.OK) {
                        textMain.text = userName
                        textAdditional.text = "Задач: $solvedTasks  Рейтинг: $rating  Место: $place"
                    }else{
                        textMain.text = id
                        textAdditional.text = ""
                    }
                }
                textMain.setTextColor(activity.defaultTextColor)
                textAdditional.setTextColor(activity.defaultTextColor)
            }
        }

        panels = listOf(
            codeforcesPanel,
            atcoderPanel,
            topcoderPanel,
            acmpPanel
        )

        if(panels.map { it.manager.PREFERENCES_FILE_NAME }.distinct().size != panels.size) throw Exception("not different file names in panels managers")


        codeforcesPanel.buildAndAdd(30F, 25F, view)
        atcoderPanel.buildAndAdd(30F, 25F, view)
        topcoderPanel.buildAndAdd(30F, 25F, view)
        acmpPanel.buildAndAdd(17F, 14F, view)




        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        println("fragment accounts onViewCreated "+savedInstanceState)
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as MainActivity

        panels.forEach { it.show() }

        buttonReload = view.findViewById(R.id.buttonReload)
        buttonReload.setOnClickListener {
            activity.scope.launch {
                panels.forEach {
                    launch { it.reload() }
                }
            }
        }
    }

    override fun onDestroyView() {
        println("fragment accounts onDestroyView")
        super.onDestroyView()
    }

    val toggleSet = TreeSet<String>()
    fun toggleReload(s: String){
        if(toggleSet.contains(s)){
            toggleSet.remove(s)
            if(toggleSet.isEmpty()) buttonReload.isEnabled = true
        }else{
            if(toggleSet.isEmpty()) buttonReload.isEnabled = false
            toggleSet.add(s)
        }
    }

    override fun onResume() {
        super.onResume()
        println("AccountsFragment onResume")
    }

}