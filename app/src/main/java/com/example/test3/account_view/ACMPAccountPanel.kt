package com.example.test3.account_view

import android.text.SpannableStringBuilder
import android.util.TypedValue
import android.widget.TextView
import androidx.core.text.color
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.account_manager.ACMPAccountManager
import com.example.test3.account_manager.STATUS
import com.example.test3.account_manager.UserInfo
import com.example.test3.utils.getColorFromResource

class ACMPAccountPanel(
    mainActivity: MainActivity,
    manager: ACMPAccountManager
): AccountPanel(mainActivity, manager)  {

    override val homeURL = "https://acmp.ru"

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
                    color(additionalColor){ append("  rating: ") }
                    append("$rating")
                    color(additionalColor){ append("  rank: ") }
                    append("$rank")
                }
            }else{
                textMain.text = id
                textAdditional.text = ""
            }
        }
        textMain.setTextColor(mainActivity.defaultTextColor)
        textAdditional.setTextColor(mainActivity.defaultTextColor)
    }

    override val bigViewResource = R.layout.fragment_account_view_acmp

    override suspend fun showBigView(fragment: AccountViewFragment) {
        val view = fragment.requireView()

        val info = manager.getSavedInfo() as ACMPAccountManager.ACMPUserInfo

        view.findViewById<TextView>(R.id.account_view_name).apply {
            text = info.userName
        }

        view.findViewById<TextView>(R.id.account_view_acmp_solved).apply {
            text = info.solvedTasks.toString()
        }

        view.findViewById<TextView>(R.id.account_view_acmp_rating).apply {
            text = info.rating.toString()
        }

        view.findViewById<TextView>(R.id.account_view_acmp_rank).apply {
            text = info.rank.toString()
        }
    }
}