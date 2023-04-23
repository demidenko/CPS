package com.demich.cps.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

fun Context.showToast(title: String) = Toast.makeText(this, title, Toast.LENGTH_LONG).show()

private fun intentActionView(url: String) = Intent(Intent.ACTION_VIEW, Uri.parse(url))

fun Context.openUrlInBrowser(url: String) = startActivity(intentActionView(url))

fun makePendingIntentOpenUrl(url: String, context: Context): PendingIntent =
    PendingIntent.getActivity(
        context,
        0,
        intentActionView(url),
        PendingIntent.FLAG_IMMUTABLE
    )