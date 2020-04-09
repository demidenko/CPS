package com.example.test3

import android.content.Intent
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.view.animation.*
import android.widget.*
import kotlinx.coroutines.launch


abstract class AccountPanel<A:AccountManager,I:UserInfo>(
    val activity: MainActivity,
    val manager: A,
    val type: String
){
    val layout = RelativeLayout(activity)
    val textMain = TextView(activity)
    val textAdditional = TextView(activity)
    val settingsButton = ImageButton(activity).apply {
        setImageDrawable(activity.getDrawable(R.drawable.ic_settings_white))
        setBackgroundColor(Color.TRANSPARENT)
        visibility = View.GONE
        setOnClickListener {
            val intent = Intent(activity, Settings::class.java).putExtra("manager", type)
            activity.startActivityForResult(intent, MainActivity.CALL_ACCOUNT_SETTINGS)
        }
    }
    val reloadButton = ImageButton(activity).apply {
        setImageDrawable(activity.getDrawable(R.drawable.ic_refresh_white))
        setBackgroundColor(Color.TRANSPARENT)
        visibility = View.GONE
        setOnClickListener {
            activity.scope.launch { reload() }
        }
    }


    fun buildAndAdd(textMainSize: Float, textAdditionalSize: Float){
        textMain.id = View.generateViewId()
        textMain.setTextSize(TypedValue.COMPLEX_UNIT_SP, textMainSize)
        layout.addView(textMain, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_START)
        })

        textAdditional.id = View.generateViewId()
        textAdditional.setTextSize(TypedValue.COMPLEX_UNIT_SP, textAdditionalSize)
        layout.addView(textAdditional, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.BELOW, textMain.id)
        })

        reloadButton.id = View.generateViewId()
        layout.addView(reloadButton, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        })

        layout.addView(settingsButton, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.LEFT_OF, reloadButton.id)
        })

        activity.findViewById<LinearLayout>(R.id.panels_layout).addView(layout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            val pix = 30
            setMargins(pix, 0, pix, pix)
        })



        additionalBuild()


        layout.setOnClickListener {
            if(reloadButton.isEnabled) {
                reloadButton.clearAnimation()
                reloadButton.animate().setStartDelay(0).setDuration(0).alpha(1f).withEndAction {
                    reloadButton.visibility = View.VISIBLE
                    reloadButton.animate().setStartDelay(5000).setDuration(2000).alpha(0f)
                        .withEndAction {
                            reloadButton.visibility = View.GONE
                        }
                }
            }
            if(settingsButton.isEnabled) {
                settingsButton.animate().setStartDelay(0).setDuration(0).alpha(1f).withEndAction {
                    settingsButton.visibility = View.VISIBLE
                    settingsButton.animate().setStartDelay(5000).setDuration(2000).alpha(0f)
                        .withEndAction {
                            settingsButton.visibility = View.GONE
                        }
                }
            }
        }
    }

    open fun additionalBuild(){ }

    abstract fun show(info: I)

    fun show(){
        show(manager.getSavedInfo() as I)
    }

    val rotateAnimation = RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
        duration = 1000
        repeatCount = Animation.INFINITE
        repeatMode = Animation.INFINITE
        interpolator = LinearInterpolator()
    }

    suspend fun reload(){
        settingsButton.isEnabled = false
        reloadButton.isEnabled = false
        activity.accountsFragment.toggleReload(manager.preferences_file_name)


        settingsButton.animate().setStartDelay(0).alpha(0f).setDuration(0).withEndAction {
            settingsButton.visibility = View.GONE
        }

        reloadButton.animate().setStartDelay(0).alpha(1f).setDuration(0).withStartAction {
            reloadButton.setColorFilter(activity.defaultTextColor)
            reloadButton.visibility = View.VISIBLE
        }
        reloadButton.startAnimation(rotateAnimation)

        textAdditional.text = "..."
        val savedInfo = manager.getSavedInfo() as I
        val info = manager.loadInfo(savedInfo.usedID) as I?

        if(info!=null){
            if (info != savedInfo) manager.saveInfo(info)
            show(info)
            reloadButton.animate().setStartDelay(0).setDuration(1000).alpha(0f).withEndAction {
                reloadButton.clearAnimation()
                reloadButton.visibility = View.GONE
            }
        }else{
            show(savedInfo)
            Toast.makeText(activity, "${manager.preferences_file_name} load error", Toast.LENGTH_LONG).show()
            reloadButton.clearAnimation()
            reloadButton.setColorFilter(Color.rgb(200,64,64))
        }


        activity.accountsFragment.toggleReload(manager.preferences_file_name)
        reloadButton.isEnabled = true
        settingsButton.isEnabled = true
    }
}
