package com.example.test3

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.util.*


class MainActivity : AppCompatActivity(){

    val scope = CoroutineScope(Job() + Dispatchers.Main)

    var defaultTextColor: Int = 0

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("main create")

        defaultTextColor = ContextCompat.getColor(this, R.color.textColor)

        setContentView(R.layout.activity_main)

        supportActionBar?.title = "Competitive Programming & Solving"

        codeforcesAccountManager = CodeforcesAccountManager(this)
        codeforcesPanel = object : AccountPanel<CodeforcesAccountManager, CodeforcesAccountManager.CodeforcesUserInfo>(this, codeforcesAccountManager, CodeforcesAccountManager::class.java.name){
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
                textMain.setTextColor(color ?: defaultTextColor)
                textAdditional.setTextColor(color ?: defaultTextColor)
                window.statusBarColor = color ?: Color.TRANSPARENT
            }
        }

        acmpAccountManager = ACMPAccountManager(this)
        acmpPanel = object : AccountPanel<ACMPAccountManager, ACMPAccountManager.ACMPUserInfo>(this, acmpAccountManager, ACMPAccountManager::class.java.name){
            override fun show(info: ACMPAccountManager.ACMPUserInfo) {
                with(info){
                    textMain.text = userName
                    textAdditional.text = "Задач: $solvedTasks  Рейтинг: $rating"
                }
                textMain.setTextColor(defaultTextColor)
                textAdditional.setTextColor(defaultTextColor)
            }
        }

        atcoderAccountManager = AtCoderAccountManager(this)
        atcoderPanel = object : AccountPanel<AtCoderAccountManager, AtCoderAccountManager.AtCoderUserInfo>(this, atcoderAccountManager, AtCoderAccountManager::class.java.name){
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
                textMain.setTextColor(color ?: defaultTextColor)
                textAdditional.setTextColor(color ?: defaultTextColor)
            }
        }

        topcoderAccountManager = TopCoderAccountManager(this)
        topcoderPanel = object : AccountPanel<TopCoderAccountManager, TopCoderAccountManager.TopCoderUserInfo>(this, topcoderAccountManager, TopCoderAccountManager::class.java.name){
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
                textMain.setTextColor(color ?: defaultTextColor)
                textAdditional.setTextColor(color ?: defaultTextColor)

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

        codeforcesPanel.buildAndAdd(30F, 25F)
        atcoderPanel.buildAndAdd(30F, 25F)
        topcoderPanel.buildAndAdd(30F, 25F)
        acmpPanel.buildAndAdd(17F, 14F)


        panels.forEach { it.show() }


        buttonReload = findViewById(R.id.buttonReload)
        buttonReload.setOnClickListener {
            scope.launch {
                panels.forEach {
                    launch { it.reload() }
                }
            }
        }


        ///stuff
        /*val layoutCols = LinearLayout(this)

        mapOf(
            "CodeForces" to arrayOf("808080", "008000", "03A89E", "0000FF", "AA00AA", "FF8C00", "FF0000"),
            "AtCoder" to arrayOf("FF0000", "FF8000", "C0C000", "0000FF", "00C0C0", "008000", "804000", "808080").reversedArray(),
            "TopCoder" to arrayOf("999999", "00A900", "6666FE", "DDCC00", "EE0000")
        ).forEach { (s, a) ->
            val layoutRows = LinearLayout(this)
            layoutRows.orientation = LinearLayout.VERTICAL
            for(c in a){
                val t = TextView(this)
                t.text = s
                t.setTextColor(Color.parseColor("#FF$c"))
                t.typeface = Typeface.DEFAULT_BOLD
                layoutRows.addView(t)
            }
            layoutCols.addView(layoutRows)
        }

        findViewById<LinearLayout>(R.id.main_layout).addView(layoutCols)*/

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
        println("main resume")
        super.onResume()
    }



    override fun onDestroy() {
        println("main destroy")
        scope.cancel()
        super.onDestroy()
    }


    companion object {
        const val CALL_SETTINGS = 1
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        println("on result $resultCode")
        if(requestCode == CALL_SETTINGS && resultCode == Activity.RESULT_OK){
            val who: String = data!!.getStringExtra("manager")!!
            panels.first { it.manager.preferences_file_name==who }.show()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

}

