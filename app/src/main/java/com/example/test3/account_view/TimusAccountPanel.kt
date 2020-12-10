package com.example.test3.account_view

import android.text.SpannableStringBuilder
import android.util.TypedValue
import androidx.core.text.color
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.account_manager.STATUS
import com.example.test3.account_manager.TimusAccountManager
import com.example.test3.account_manager.UserInfo
import com.example.test3.getColorFromResource

class TimusAccountPanel(
    mainActivity: MainActivity,
    manager: TimusAccountManager
): AccountPanel(mainActivity, manager)  {

    private val additionalColor = getColorFromResource(mainActivity, R.color.textColorAdditional)

    override fun show(info: UserInfo) { info as TimusAccountManager.TimusUserInfo
        textMain.setTextSize(TypedValue.COMPLEX_UNIT_SP,17f) //TODO: bad spacing
        textAdditional.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        with(info){
            if (status == STATUS.OK) {
                textMain.text = userName
                textAdditional.text = SpannableStringBuilder().apply {
                    color(additionalColor){ append("solved: ") }
                    append("$solvedTasks")
                    color(additionalColor){ append("  rank: ") }
                    append("$placeTasks")
                }
            }else{
                textMain.text = id
                textAdditional.text = ""
            }
        }
        textMain.setTextColor(mainActivity.defaultTextColor)
        textAdditional.setTextColor(mainActivity.defaultTextColor)
    }

}