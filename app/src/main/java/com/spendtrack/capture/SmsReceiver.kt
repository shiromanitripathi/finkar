package com.spendtrack.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/** Reads incoming bank SMS and prompts when one is a debit. */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        // Long SMS arrive in parts; join parts from the same sender.
        messages.groupBy { it.originatingAddress.orEmpty() }.forEach { (sender, parts) ->
            val body = parts.joinToString("") { it.messageBody.orEmpty() }
            PaymentParser.parse(body)?.let {
                PromptNotifier.show(context, it, source = "SMS:$sender", rawText = body)
            }
        }
    }
}
