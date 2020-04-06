package com.example.test3

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.TypedValue
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
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
        setOnClickListener {
            val intent = Intent(activity, Settings::class.java).putExtra("manager", type)
            activity.startActivityForResult(intent, MainActivity.CALL_SETTINGS)
        }
    }
    val reloadButton = ImageButton(activity).apply {
        setImageDrawable(activity.getDrawable(R.drawable.ic_refresh_white))
        setBackgroundColor(Color.TRANSPARENT)
        setOnClickListener {
            activity.scope.launch { reload() }
        }
    }


    fun buildAndAdd(textMainSize: Float, textAdditionalSize: Float){
        settingsButton.id = View.generateViewId()
        var params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        layout.addView(settingsButton, params)

        params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        params.addRule(RelativeLayout.LEFT_OF, settingsButton.id)
        layout.addView(reloadButton, params)

        textMain.id = View.generateViewId()
        textMain.setTextSize(TypedValue.COMPLEX_UNIT_SP, textMainSize)
        params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        params.addRule(RelativeLayout.ALIGN_PARENT_START)
        layout.addView(textMain, params)

        textAdditional.id = View.generateViewId()
        textAdditional.setTextSize(TypedValue.COMPLEX_UNIT_SP, textAdditionalSize)
        params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        params.addRule(RelativeLayout.BELOW, textMain.id)
        layout.addView(textAdditional, params)

        var p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val pix = 30
        p.setMargins(pix, pix, pix,0)
        activity.findViewById<LinearLayout>(R.id.panels_layout).addView(layout, p)

        additionalBuild()
    }

    open fun additionalBuild(){ }

    abstract fun show(info: I)

    fun show(){
        show(manager.getSavedInfo() as I)
    }

    val animation = RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
        duration = 1000
        repeatCount = Animation.INFINITE
        repeatMode = Animation.INFINITE
        interpolator = LinearInterpolator()
    }
    suspend fun reload(){
        settingsButton.isEnabled = false
        reloadButton.isEnabled = false
        activity.toggleReload(manager.preferences_file_name)
        reloadButton.startAnimation(animation)
        reloadButton.setColorFilter(activity.defaultTextColor)

        textAdditional.text = "..."
        val savedInfo = manager.getSavedInfo() as I
        val info = manager.loadInfo(savedInfo.usedID) as I?
        if(info!=null){
            if (info != savedInfo) manager.saveInfo(info)
            show(info)
        }else{
            show(savedInfo)
            Toast.makeText(activity, "${manager.preferences_file_name} load error", Toast.LENGTH_LONG).show()
            reloadButton.setColorFilter(Color.rgb(200,64,64))
        }

        reloadButton.clearAnimation()
        activity.toggleReload(manager.preferences_file_name)
        reloadButton.isEnabled = true
        settingsButton.isEnabled = true
    }
}
