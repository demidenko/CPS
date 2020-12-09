package com.example.test3.account_view

import android.graphics.Typeface
import com.example.test3.MainActivity
import com.example.test3.account_manager.NOT_RATED
import com.example.test3.account_manager.STATUS
import com.example.test3.account_manager.TopCoderAccountManager
import com.example.test3.account_manager.UserInfo

class TopCoderAccountPanel(
    mainActivity: MainActivity,
    manager: TopCoderAccountManager
): AccountPanel(mainActivity, manager)  {

    override fun show(info: UserInfo) { info as TopCoderAccountManager.TopCoderUserInfo
        val color = manager.getColor(info)
        textMain.text = info.handle
        textMain.setTextColor(color ?: mainActivity.defaultTextColor)
        textAdditional.text = ""
        textAdditional.setTextColor(color ?: mainActivity.defaultTextColor)
        if(info.status == STATUS.OK){
            textMain.typeface = Typeface.DEFAULT_BOLD
            textAdditional.text = if(info.rating_algorithm == NOT_RATED) "[not rated]" else "${info.rating_algorithm}"
        }else{
            textMain.typeface = Typeface.DEFAULT
        }
    }

}