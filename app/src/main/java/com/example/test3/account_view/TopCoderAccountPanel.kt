package com.example.test3.account_view

import android.graphics.Typeface
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
    manager: TopCoderAccountManager
): AccountPanel(mainActivity, manager)  {

    override fun show(info: UserInfo) { info as TopCoderAccountManager.TopCoderUserInfo
        val color = manager.getColor(info)
        textMain.text = info.handle
        textMain.setTextColor(color ?: mainActivity.defaultTextColor)
        textAdditional.text = ""
        textAdditional.setTextColor(color ?: mainActivity.defaultTextColor)
        if(info.status == STATUS.OK){
            textMain.typeface = Typeface.DEFAULT_BOLD
            textAdditional.text = if(info.rating_algorithm == NOT_RATED) "[not rated]" else "${info.rating_algorithm}"
        }else{
            textMain.typeface = Typeface.DEFAULT
        }
    }

    override val bigViewResource = R.layout.fragment_account_view_topcoder

    override fun showBigView(fragment: AccountViewFragment) {
        val view = fragment.requireView()

        val info = manager.savedInfo as TopCoderAccountManager.TopCoderUserInfo
        val color = manager.getColor(info) ?: mainActivity.defaultTextColor

        val handleView = view.findViewById<TextView>(R.id.account_view_handle)
        val ratingView = view.findViewById<TextView>(R.id.account_view_rating)
        val marathonRatingView = view.findViewById<TextView>(R.id.account_view_tc_marathon)

        handleView.setTextColor(color)
        ratingView.setTextColor(color)
        handleView.text = info.handle
        if(info.status == STATUS.OK){
            handleView.typeface = Typeface.DEFAULT_BOLD
            ratingView.text = if(info.rating_algorithm == NOT_RATED) "[not rated]" else "${info.rating_algorithm}"
        }else{
            handleView.typeface = Typeface.DEFAULT
        }

        marathonRatingView.apply {
            val marathonRatingViewTitle = view.findViewById<TextView>(R.id.account_view_tc_marathon_title)
            if(info.rating_marathon == NOT_RATED){
                visibility = View.GONE
                marathonRatingViewTitle.visibility = View.GONE
            }else {
                visibility = View.VISIBLE
                marathonRatingViewTitle.visibility = View.VISIBLE

                val marathonColor = TopCoderAccountManager.getHandleColor(info.rating_marathon).getARGB(TopCoderAccountManager.Companion)
                setTextColor(marathonColor)
                text = "${info.rating_marathon}"
            }
        }
    }

}