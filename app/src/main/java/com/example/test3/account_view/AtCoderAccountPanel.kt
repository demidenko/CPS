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

    private fun showMain(handleView: TextView, ratingView: TextView, info: AtCoderAccountManager.AtCoderUserInfo) {
        val color = manager.getColor(info) ?: mainActivity.defaultTextColor
        handleView.setTextColor(color)
        ratingView.setTextColor(color)
        handleView.text = info.handle
        if(info.status == STATUS.OK){
            handleView.typeface = Typeface.DEFAULT_BOLD
            ratingView.text = if(info.rating == NOT_RATED) "[not rated]" else "${info.rating}"
        }else{
            handleView.typeface = Typeface.DEFAULT
            ratingView.text = ""
        }
    }

    override fun show(info: UserInfo) {
        showMain(textMain, textAdditional, info as AtCoderAccountManager.AtCoderUserInfo)
    }

    override val bigViewResource = R.layout.fragment_account_view_atcoder

    override suspend fun showBigView(fragment: AccountViewFragment) {
        val view = fragment.requireView()

        val info = manager.getSavedInfo() as AtCoderAccountManager.AtCoderUserInfo

        val handleView = view.findViewById<TextView>(R.id.account_view_handle)
        val ratingView = view.findViewById<TextView>(R.id.account_view_rating)

        showMain(handleView, ratingView, info)

    }

}