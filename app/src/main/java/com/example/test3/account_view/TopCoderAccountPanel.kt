package com.example.test3.account_view

import android.widget.TextView
import androidx.core.view.isVisible
import com.example.test3.MainActivity
import com.example.test3.R
import com.example.test3.account_manager.NOT_RATED
import com.example.test3.account_manager.TopCoderAccountManager
import com.example.test3.account_manager.UserInfo

class TopCoderAccountPanel(
    mainActivity: MainActivity,
    override val manager: TopCoderAccountManager
): AccountPanel(mainActivity, manager)  {

    override fun show(info: UserInfo) {
        showMainRated(textMain, textAdditional, manager, info)
    }

    override val bigViewResource = R.layout.fragment_account_view_topcoder

    override suspend fun showBigView(fragment: AccountViewFragment) {
        val view = fragment.requireView()

        val info = manager.getSavedInfo() as TopCoderAccountManager.TopCoderUserInfo

        val handleView = view.findViewById<TextView>(R.id.account_view_handle)
        val ratingView = view.findViewById<TextView>(R.id.account_view_rating)
        val marathonRatingView = view.findViewById<TextView>(R.id.account_view_tc_marathon)

        showMainRated(handleView, ratingView, manager, info)

        marathonRatingView.apply {
            val marathonRatingViewTitle = view.findViewById<TextView>(R.id.account_view_tc_marathon_title)
            if(info.rating_marathon == NOT_RATED){
                isVisible = false
                marathonRatingViewTitle.isVisible = false
            }else {
                isVisible = true
                marathonRatingViewTitle.isVisible = true

                val marathonColor = manager.getHandleColorARGB(info.rating_marathon)
                setTextColor(marathonColor)
                text = "${info.rating_marathon}"
            }
        }

        RatingGraphView.showInAccountViewFragment(fragment, manager)
    }

}