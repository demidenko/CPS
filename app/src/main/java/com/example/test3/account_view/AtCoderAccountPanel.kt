package com.example.test3.account_view

import android.graphics.Typeface
import com.example.test3.MainActivity
import com.example.test3.account_manager.AtCoderAccountManager
import com.example.test3.account_manager.NOT_RATED
import com.example.test3.account_manager.STATUS
import com.example.test3.account_manager.UserInfo

class AtCoderAccountPanel(
    mainActivity: MainActivity,
    manager: AtCoderAccountManager
): AccountPanel(mainActivity, manager)  {

    override fun show(info: UserInfo) { info as AtCoderAccountManager.AtCoderUserInfo
        val color = manager.getColor(info)
        textMain.text = info.handle
        textMain.setTextColor(color ?: activity.defaultTextColor)
        textAdditional.text = ""
        textAdditional.setTextColor(color ?: activity.defaultTextColor)
        if(info.status == STATUS.OK){
            textMain.typeface = Typeface.DEFAULT_BOLD
            textAdditional.text = if(info.rating == NOT_RATED) "[not rated]" else "${info.rating}"
        }else{
            textMain.typeface = Typeface.DEFAULT
        }
    }

}