package com.example.test3

import android.app.Service
import android.content.Intent
import android.hardware.SensorAdditionalInfo
import android.os.IBinder
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat



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
        setBackgroundColor(ContextCompat.getColor(activity, R.color.background))
        setOnClickListener {
            //activity.settingsCalled = type
            val intent = Intent(activity, Settings::class.java).putExtra("manager", type)
            //activity.startActivity(intent)
            activity.startActivityForResult(intent, MainActivity.CALL_SETTINGS)
        }
    }


    fun buildAndAdd(textMainSize: Float, textAdditionalSize: Float){
        var params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        layout.addView(settingsButton, params)

        textMain.setTextSize(TypedValue.COMPLEX_UNIT_SP, textMainSize)
        params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        params.addRule(RelativeLayout.ALIGN_PARENT_START)
        layout.addView(textMain, params)

        textAdditional.setTextSize(TypedValue.COMPLEX_UNIT_SP, textAdditionalSize)
        params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        textMain.id = View.generateViewId()
        params.addRule(RelativeLayout.BELOW, textMain.id)
        layout.addView(textAdditional, params)

        var p = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val pix = 30
        p.setMargins(pix, pix, pix,0)
        activity.findViewById<LinearLayout>(R.id.panels_layout).addView(layout, p)
    }

    abstract fun show(info: I)

    fun show(){
        show(manager.getSavedInfo() as I)
    }

    suspend fun reload(){
        settingsButton.isEnabled = false
        textAdditional.text = "..."
        val savedInfo = manager.getSavedInfo() as I
        val info = manager.loadInfo(savedInfo.usedID) as I?
        if(info!=null) {
            if (info != savedInfo) manager.saveInfo(info)
            show(info)
        }else{
            show(savedInfo)
            Toast.makeText(activity, "${manager.preferences_file_name} load error", Toast.LENGTH_LONG).show()
        }
        settingsButton.isEnabled = true
    }
}
