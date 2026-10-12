package com.spendtrack.capture

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Watches UPI app notifications and prompts when one is a completed debit. */
class PaymentNotificationListener : NotificationListenerService() {

    private val upiApps = setOf(
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",                        // PhonePe
        "net.one97.paytm",                        // Paytm
        "in.org.npci.upiapp",                     // BHIM
        "com.dreamplug.androidapp",               // CRED
        "in.amazon.mShop.android.shopping"        // Amazon Pay (UPI)
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in upiApps) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val full = "$title. $text".trim()

        PaymentParser.parse(full)?.let {
            PromptNotifier.show(this, it, source = sbn.packageName, rawText = full)
        }
    }
}
