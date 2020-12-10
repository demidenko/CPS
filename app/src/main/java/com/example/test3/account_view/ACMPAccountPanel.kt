package com.example.test3.account_view

import android.text.SpannableStringBuilder
import android.util.TypedValue
import androidx.core.text.color
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.account_manager.ACMPAccountManager
import com.example.test3.account_manager.STATUS
import com.example.test3.account_manager.UserInfo
import com.example.test3.getColorFromResource

class ACMPAccountPanel(
    mainActivity: MainActivity,
    manager: ACMPAccountManager
): AccountPanel(mainActivity, manager)  {

    private val additionalColor = getColorFromResource(mainActivity, R.color.textColorAdditional)

    override fun show(info: UserInfo) { info as ACMPAccountManager.ACMPUserInfo
        textMain.setTextSize(TypedValue.COMPLEX_UNIT_SP,17f) //TODO: bad spacing
        textAdditional.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        with(info){
            if (status == STATUS.OK) {
                textMain.text = userName
                textAdditional.text = SpannableStringBuilder().apply {
                    color(additionalColor){ append("solved: ") }
                    append("$solvedTasks")
                    color(additionalColor){ append("  rank: ") }
                    append("$place")
                    color(additionalColor){ append("  rating: ") }
                    append("$rating")
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