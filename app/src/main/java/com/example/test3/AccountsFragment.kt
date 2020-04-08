package com.example.test3

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.coroutines.launch
import java.util.*

class AccountsFragment(): Fragment() {

    private lateinit var buttonReload: Button

    lateinit var codeforcesAccountManager: CodeforcesAccountManager
    lateinit var atcoderAccountManager: AtCoderAccountManager
    lateinit var topcoderAccountManager: TopCoderAccountManager
    lateinit var acmpAccountManager: ACMPAccountManager

    lateinit var panels: List<AccountPanel<out AccountManager, out UserInfo>>
    lateinit var codeforcesPanel: AccountPanel<CodeforcesAccountManager, CodeforcesAccountManager.CodeforcesUserInfo>
    lateinit var atcoderPanel: AccountPanel<AtCoderAccountManager, AtCoderAccountManager.AtCoderUserInfo>
    lateinit var topcoderPanel: AccountPanel<TopCoderAccountManager, TopCoderAccountManager.TopCoderUserInfo>
    lateinit var acmpPanel: AccountPanel<ACMPAccountManager, ACMPAccountManager.ACMPUserInfo>



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_accounts, container, false)
        val activity = activity!! as MainActivity //:)


        codeforcesAccountManager = CodeforcesAccountManager(activity)
        codeforcesPanel = object : AccountPanel<CodeforcesAccountManager, CodeforcesAccountManager.CodeforcesUserInfo>(activity, codeforcesAccountManager, CodeforcesAccountManager::class.java.name){
            override fun show(info: CodeforcesAccountManager.CodeforcesUserInfo) {
                textMain.text = info.handle
                textAdditional.text = ""
                val color = manager.getColor(info)
                when (info.rating) {
                    CodeforcesAccountManager.NOT_FOUND -> {
                        textMain.typeface = Typeface.DEFAULT
                    }
                    CodeforcesAccountManager.NOT_RATED -> {
                        textMain.typeface = Typeface.DEFAULT
                    }
                    else -> {
                        textMain.typeface = Typeface.DEFAULT_BOLD
                        textAdditional.text = "${info.rating}"
                    }
                }
                textMain.setTextColor(color ?: activity.defaultTextColor)
                textAdditional.setTextColor(color ?: activity.defaultTextColor)
                activity.window.statusBarColor = color ?: Color.TRANSPARENT
            }
        }

        acmpAccountManager = ACMPAccountManager(activity)
        acmpPanel = object : AccountPanel<ACMPAccountManager, ACMPAccountManager.ACMPUserInfo>(activity, acmpAccountManager, ACMPAccountManager::class.java.name){
            override fun show(info: ACMPAccountManager.ACMPUserInfo) {
                with(info){
                    textMain.text = userName
                    textAdditional.text = "Задач: $solvedTasks  Рейтинг: $rating"
                }
                textMain.setTextColor(activity.defaultTextColor)
                textAdditional.setTextColor(activity.defaultTextColor)
            }
        }

        atcoderAccountManager = AtCoderAccountManager(activity)
        atcoderPanel = object : AccountPanel<AtCoderAccountManager, AtCoderAccountManager.AtCoderUserInfo>(activity, atcoderAccountManager, AtCoderAccountManager::class.java.name){
            override fun show(info: AtCoderAccountManager.AtCoderUserInfo) {
                textMain.text = info.handle
                textAdditional.text = ""
                val color = manager.getColor(info)
                when (info.rating) {
                    AtCoderAccountManager.NOT_FOUND -> {
                        textMain.typeface = Typeface.DEFAULT
                    }
                    AtCoderAccountManager.NOT_RATED -> {
                        textMain.typeface = Typeface.DEFAULT
                    }
                    else -> {
                        textMain.typeface = Typeface.DEFAULT_BOLD
                        textAdditional.text = "${info.rating}"
                    }
                }
                textMain.setTextColor(color ?: activity.defaultTextColor)
                textAdditional.setTextColor(color ?: activity.defaultTextColor)
            }
        }

        topcoderAccountManager = TopCoderAccountManager(activity)
        topcoderPanel = object : AccountPanel<TopCoderAccountManager, TopCoderAccountManager.TopCoderUserInfo>(activity, topcoderAccountManager, TopCoderAccountManager::class.java.name){
            override fun show(info: TopCoderAccountManager.TopCoderUserInfo) {
                textMain.text = info.handle
                textAdditional.text = ""
                val color = manager.getColor(info)
                when (info.rating_algorithm) {
                    TopCoderAccountManager.NOT_FOUND -> {
                        textMain.typeface = Typeface.DEFAULT
                    }
                    TopCoderAccountManager.NOT_RATED -> {
                        textMain.typeface = Typeface.DEFAULT
                    }
                    else -> {
                        textMain.typeface = Typeface.DEFAULT_BOLD
                        textAdditional.text = "${info.rating_algorithm}"
                    }
                }
                textMain.setTextColor(color ?: activity.defaultTextColor)
                textAdditional.setTextColor(color ?: activity.defaultTextColor)

                /*if(color!=null) {
                    circle.color = color
                    circle.invalidate()
                }*/
            }

            /*lateinit var circle: TopCoderCircle

            override fun additionalBuild() {
                circle = TopCoderCircle(activity)
                circle.id = View.generateViewId()

                val kostil = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    id = View.generateViewId()
                    gravity = Gravity.CENTER
                }

                layout.removeView(textMain)

                val params = LinearLayout.LayoutParams(50, 50)
                kostil.addView(circle, params)
                kostil.addView(textMain)

                layout.addView(kostil)

                (textAdditional.layoutParams as RelativeLayout.LayoutParams).apply {
                    addRule(RelativeLayout.BELOW, kostil.id)
                }
            }

            inner class TopCoderCircle(context: Context) : View(context) {
                var color: Int = defaultTextColor
                val shape = ShapeDrawable(OvalShape())
                override fun onDraw(canvas: Canvas) {
                    println("onDraw")
                    shape.apply {
                        println(color)
                        paint.color = color
                        setBounds(0, 0, 50, 50)
                    }.draw(canvas)
                }
            }*/
        }


        panels = listOf(
            codeforcesPanel,
            atcoderPanel,
            topcoderPanel,
            acmpPanel
        )

        assert( panels.map { it.manager.preferences_file_name }.toSet().size == panels.size)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = activity!! as MainActivity //:)


        codeforcesPanel.buildAndAdd(30F, 25F, view)
        atcoderPanel.buildAndAdd(30F, 25F, view)
        topcoderPanel.buildAndAdd(30F, 25F, view)
        acmpPanel.buildAndAdd(17F, 14F, view)

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