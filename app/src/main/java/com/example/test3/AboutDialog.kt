package com.example.test3

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class AboutDialog: DialogFragment() {

    companion object {
        fun showDialog(activity: MainActivity) {
            AboutDialog().show(activity.supportFragmentManager, "about_dialog")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(requireContext())
            .setTitle("CPS")
            .setMessage("version = ${BuildConfig.VERSION_NAME}")

        return builder.create()
    }



}