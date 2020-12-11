package com.example.test3.account_view

import android.graphics.Typeface
import android.widget.TextView
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.account_manager.AtCoderAccountManager
import com.example.test3.account_manager.NOT_RATED
import com.example.test3.account_manager.STATUS
import com.example.test3.account_manager.UserInfo

class AtCoderAccountPanel(
    mainActivity: MainActivity,
    manager: AtCoderAccountManager
): AccountPanel(mainActivity, manager)  {

    override fun show(info: UserInfo) { info as AtCoderAccountManager.AtCoderUserInfo
        val color = manager.getColor(info) ?: mainActivity.defaultTextColor
        textMain.text = info.handle
        textMain.setTextColor(color)
        textAdditional.text = ""
        textAdditional.setTextColor(color)
        if(info.status == STATUS.OK){
            textMain.typeface = Typeface.DEFAULT_BOLD
            textAdditional.text = if(info.rating == NOT_RATED) "[not rated]" else "${info.rating}"
        }else{
            textMain.typeface = Typeface.DEFAULT
        }
    }

    override val bigViewResource = R.layout.fragment_account_view_atcoder

    override fun showBigView(fragment: AccountViewFragment) {
        val view = fragment.requireView()

        val info = manager.savedInfo as AtCoderAccountManager.AtCoderUserInfo
        val color = manager.getColor(info) ?: mainActivity.defaultTextColor

        val handleView = view.findViewById<TextView>(R.id.account_view_handle)
        val ratingView = view.findViewById<TextView>(R.id.account_view_rating)

        handleView.setTextColor(color)
        ratingView.setTextColor(color)
        handleView.text = info.handle
        if(info.status == STATUS.OK){
            handleView.typeface = Typeface.DEFAULT_BOLD
            ratingView.text = if(info.rating == NOT_RATED) "[not rated]" else "${info.rating}"
        }else{
            handleView.typeface = Typeface.DEFAULT
        }

    }

}