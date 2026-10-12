package com.spendtrack.capture

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.spendtrack.data.AppDatabase
import com.spendtrack.data.Categories
import com.spendtrack.data.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles the Record / Ignore buttons on the prompt. (Split opens the app instead.) */
class PromptActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        context.getSystemService(NotificationManager::class.java).cancel(id)

        if (intent.action != ACTION_RECORD) return

        val merchant = intent.getStringExtra(EXTRA_MERCHANT) ?: "Unknown"
        val tx = Transaction(
            amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0),
            merchant = merchant,
            category = Categories.guess(merchant),
            source = intent.getStringExtra(EXTRA_SOURCE) ?: "",
            rawText = intent.getStringExtra(EXTRA_RAW) ?: ""
        )
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.get(context).dao().insert(tx)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_RECORD = "com.spendtrack.RECORD"
        const val ACTION_IGNORE = "com.spendtrack.IGNORE"
        const val ACTION_SPLIT = "com.spendtrack.SPLIT"
        const val EXTRA_NOTIFICATION_ID = "nid"
        const val EXTRA_AMOUNT = "amount"
        const val EXTRA_MERCHANT = "merchant"
        const val EXTRA_SOURCE = "source"
        const val EXTRA_RAW = "raw"
    }
}
