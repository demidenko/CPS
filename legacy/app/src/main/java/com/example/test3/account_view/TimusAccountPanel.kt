package com.example.test3.account_view

import android.text.SpannableStringBuilder
import android.util.TypedValue
import android.widget.TextView
import androidx.core.text.color
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.account_manager.STATUS
import com.example.test3.account_manager.TimusAccountManager
import com.example.test3.utils.getColorFromResource

class TimusAccountPanel(
    mainActivity: MainActivity,
    manager: TimusAccountManager
): AccountPanel<TimusAccountManager.TimusUserInfo>(mainActivity, manager)  {

    private val additionalColor = getColorFromResource(mainActivity, R.color.textColorAdditional)

    override fun show(info: TimusAccountManager.TimusUserInfo) {
        textMain.setTextSize(TypedValue.COMPLEX_UNIT_SP,17f) //TODO: bad spacing
        textAdditional.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        with(info){
            if (status == STATUS.OK) {
                textMain.text = userName
                textAdditional.text = SpannableStringBuilder().apply {
                    color(additionalColor){ append("solved: ") }
                    append("$solvedTasks")
                    color(additionalColor){ append("  rank: ") }
                    append("$rankTasks")
                }
            }else{
                textMain.text = id
                textAdditional.text = ""
            }
        }
        textMain.setTextColor(mainActivity.defaultTextColor)
        textAdditional.setTextColor(mainActivity.defaultTextColor)
    }

    override val bigViewResource = R.layout.fragment_account_view_timus

    override suspend fun showBigView(fragment: AccountViewFragment<TimusAccountManager.TimusUserInfo>) {
        val view = fragment.requireView()

        val info = manager.getSavedInfo()

        view.findViewById<TextView>(R.id.account_view_name).apply {
            text = info.userName
        }

        view.findViewById<TextView>(R.id.account_view_timus_solved).apply {
            text = info.solvedTasks.toString()
        }

        view.findViewById<TextView>(R.id.account_view_timus_rank).apply {
            text = info.rankTasks.toString()
        }
    }

}