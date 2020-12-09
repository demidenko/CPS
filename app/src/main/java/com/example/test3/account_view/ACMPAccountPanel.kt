package com.example.test3.account_view

import com.example.test3.MainActivity
import com.example.test3.account_manager.ACMPAccountManager
import com.example.test3.account_manager.STATUS
import com.example.test3.account_manager.UserInfo

class ACMPAccountPanel(
    mainActivity: MainActivity,
    manager: ACMPAccountManager
): AccountPanel(mainActivity, manager)  {

    override fun show(info: UserInfo) { info as ACMPAccountManager.ACMPUserInfo
        with(info){
            if (status == STATUS.OK) {
                textMain.text = userName
                textAdditional.text = "Solved: $solvedTasks  Rank: $place  Rating: $rating"
            }else{
                textMain.text = id
                textAdditional.text = ""
            }
        }
        textMain.setTextColor(activity.defaultTextColor)
        textAdditional.setTextColor(activity.defaultTextColor)
    }

}