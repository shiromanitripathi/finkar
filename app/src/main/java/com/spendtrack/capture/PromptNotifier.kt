package com.spendtrack.capture

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.spendtrack.MainActivity
import com.spendtrack.data.Categories
import com.spendtrack.data.Fmt

/** Shows the instant "Record this payment?" heads-up prompt. */
object PromptNotifier {

    private const val CHANNEL_ID = "payment_prompt"

    // SMS and the UPI app both fire for one payment; show only one prompt.
    private val recent = HashMap<String, Long>()
    private const val DEDUPE_WINDOW_MS = 2 * 60 * 1000L

    @Synchronized
    private fun isDuplicate(p: ParsedPayment): Boolean {
        val now = System.currentTimeMillis()
        recent.entries.removeAll { now - it.value > DEDUPE_WINDOW_MS }
        val key = "%.2f".format(p.amount)
        if (recent.containsKey(key)) return true
        recent[key] = now
        return false
    }

    fun show(context: Context, payment: ParsedPayment, source: String, rawText: String) {
        if (isDuplicate(payment)) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        createChannel(context)
        val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val info = Categories.info(Categories.guess(payment.merchant))

        fun Intent.withPayment(): Intent = apply {
            putExtra(PromptActionReceiver.EXTRA_NOTIFICATION_ID, id)
            putExtra(PromptActionReceiver.EXTRA_AMOUNT, payment.amount)
            putExtra(PromptActionReceiver.EXTRA_MERCHANT, payment.merchant)
            putExtra(PromptActionReceiver.EXTRA_SOURCE, source)
            putExtra(PromptActionReceiver.EXTRA_RAW, rawText)
        }

        fun broadcast(kind: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, PromptActionReceiver::class.java).apply {
                action = kind
            }.withPayment()
            return PendingIntent.getBroadcast(
                context, id + requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val splitIntent = Intent(context, MainActivity::class.java).apply {
            action = PromptActionReceiver.ACTION_SPLIT
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }.withPayment()
        val split = PendingIntent.getActivity(
            context, id + 3, splitIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Record " + Fmt.money(payment.amount) + " paid?")
            .setContentText("To ${payment.merchant} · ${info.emoji} ${info.name}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .addAction(0, "Record", broadcast(PromptActionReceiver.ACTION_RECORD, 1))
            .addAction(0, "Split", split)
            .addAction(0, "Ignore", broadcast(PromptActionReceiver.ACTION_IGNORE, 2))
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val channel = NotificationChannel(
            CHANNEL_ID, "Payment prompts", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Asks whether to record a detected payment" }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}
