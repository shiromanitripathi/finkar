package com.spendtrack.ui

import android.content.Context
import android.content.Intent
import android.net.Uri

fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Share via").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Opens a WhatsApp chat with the number (10-digit numbers are treated as Indian). */
fun openWhatsApp(context: Context, phone: String, text: String) {
    val digits = phone.filter { it.isDigit() }
    val full = if (digits.length == 10) "91$digits" else digits
    val uri = Uri.parse("https://wa.me/$full?text=" + Uri.encode(text))
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        shareText(context, text)
    }
}
