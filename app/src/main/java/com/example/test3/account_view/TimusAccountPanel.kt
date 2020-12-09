package com.example.test3.account_view

import com.example.test3.MainActivity
import com.example.test3.account_manager.STATUS
import com.example.test3.account_manager.TimusAccountManager
import com.example.test3.account_manager.UserInfo

class TimusAccountPanel(
    mainActivity: MainActivity,
    manager: TimusAccountManager
): AccountPanel(mainActivity, manager)  {

    override fun show(info: UserInfo) { info as TimusAccountManager.TimusUserInfo
        with(info){
            if (status == STATUS.OK) {
                textMain.text = userName
                textAdditional.text = "Solved: $solvedTasks  Rank: $placeTasks"
            }else{
                textMain.text = id
                textAdditional.text = ""
            }
        }
        textMain.setTextColor(mainActivity.defaultTextColor)
        textAdditional.setTextColor(mainActivity.defaultTextColor)
    }

}