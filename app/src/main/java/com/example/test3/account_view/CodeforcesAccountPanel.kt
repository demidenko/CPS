package com.example.test3.account_view

import com.example.test3.MainActivity
import com.example.test3.account_manager.CodeforcesAccountManager
import com.example.test3.account_manager.NOT_RATED
import com.example.test3.account_manager.STATUS
import com.example.test3.account_manager.UserInfo
import com.example.test3.utils.CodeforcesUtils

class CodeforcesAccountPanel(
    mainActivity: MainActivity,
    manager: CodeforcesAccountManager
): AccountPanel(mainActivity, manager) {

    override fun show(info: UserInfo) { info as CodeforcesAccountManager.CodeforcesUserInfo
        val color = manager.getColor(info)
        textMain.text = CodeforcesUtils.makeSpan(info)
        textAdditional.text = ""
        textAdditional.setTextColor(color ?: mainActivity.defaultTextColor)
        if(info.status == STATUS.OK){
            textAdditional.text = if(info.rating == NOT_RATED) "[not rated]" else "${info.rating}"
        }
    }

}