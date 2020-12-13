package com.example.test3.account_view

import android.view.View
import android.widget.TextView
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.account_manager.NOT_RATED
import com.example.test3.account_manager.STATUS
import com.example.test3.account_manager.TopCoderAccountManager
import com.example.test3.account_manager.UserInfo

class TopCoderAccountPanel(
    mainActivity: MainActivity,
    override val manager: TopCoderAccountManager
): AccountPanel(mainActivity, manager)  {

    private fun showMain(handleView: TextView, ratingView: TextView, info: TopCoderAccountManager.TopCoderUserInfo) {
        val color = manager.getColor(info) ?: mainActivity.defaultTextColor
        ratingView.setTextColor(color)
        handleView.text = manager.makeSpan(info)
        if(info.status == STATUS.OK){
            ratingView.text = if(info.rating_algorithm == NOT_RATED) "[not rated]" else "${info.rating_algorithm}"
        }else{
            ratingView.text = ""
        }
    }

    override fun show(info: UserInfo) {
        showMain(textMain, textAdditional, info as TopCoderAccountManager.TopCoderUserInfo)
    }

    override val bigViewResource = R.layout.fragment_account_view_topcoder

    override suspend fun showBigView(fragment: AccountViewFragment) {
        val view = fragment.requireView()

        val info = manager.getSavedInfo() as TopCoderAccountManager.TopCoderUserInfo

        val handleView = view.findViewById<TextView>(R.id.account_view_handle)
        val ratingView = view.findViewById<TextView>(R.id.account_view_rating)
        val marathonRatingView = view.findViewById<TextView>(R.id.account_view_tc_marathon)

        showMain(handleView, ratingView, info)

        marathonRatingView.apply {
            val marathonRatingViewTitle = view.findViewById<TextView>(R.id.account_view_tc_marathon_title)
            if(info.rating_marathon == NOT_RATED){
                visibility = View.GONE
                marathonRatingViewTitle.visibility = View.GONE
            }else {
                visibility = View.VISIBLE
                marathonRatingViewTitle.visibility = View.VISIBLE

                val marathonColor = manager.getHandleColorARGB(info.rating_marathon)
                setTextColor(marathonColor)
                text = "${info.rating_marathon}"
            }
        }
    }

}