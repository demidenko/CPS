package com.demich.cps.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.Html
import android.text.Spanned
import android.widget.Toast
import androidx.compose.ui.text.intl.Locale
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat

fun Context.showToast(title: String) = Toast.makeText(this, title, Toast.LENGTH_LONG).show()

private fun intentActionView(url: String) = Intent(Intent.ACTION_VIEW, url.toUri())

fun Context.openUrlInBrowser(url: String) = startActivity(intentActionView(url))

fun makePendingIntentOpenUrl(url: String, context: Context): PendingIntent =
    PendingIntent.getActivity(
        context,
        0,
        intentActionView(url),
        PendingIntent.FLAG_IMMUTABLE
    )

fun String.asHtmlToSpanned(): Spanned =
    Html.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY)

fun isRuSystemLanguage(): Boolean =
    Locale.current.language == "ru"