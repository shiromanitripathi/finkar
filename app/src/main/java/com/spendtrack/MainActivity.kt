package com.spendtrack

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.spendtrack.capture.PromptActionReceiver
import com.spendtrack.ui.AppRoot
import com.spendtrack.ui.SpendTrackTheme

class MainActivity : ComponentActivity() {

    private val vm: SpendViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handlePromptIntent(intent)
        setContent { SpendTrackTheme { AppRoot(vm) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePromptIntent(intent)
    }

    /** The "Split" button on the payment pop-up opens the app here. */
    private fun handlePromptIntent(i: Intent?) {
        if (i == null || i.action != PromptActionReceiver.ACTION_SPLIT) return
        val amount = i.getDoubleExtra(PromptActionReceiver.EXTRA_AMOUNT, 0.0)
        if (amount <= 0.0) return
        getSystemService(NotificationManager::class.java)
            .cancel(i.getIntExtra(PromptActionReceiver.EXTRA_NOTIFICATION_ID, 0))
        vm.fromPrompt(
            amount = amount,
            merchant = i.getStringExtra(PromptActionReceiver.EXTRA_MERCHANT) ?: "Unknown",
            source = i.getStringExtra(PromptActionReceiver.EXTRA_SOURCE) ?: "",
            raw = i.getStringExtra(PromptActionReceiver.EXTRA_RAW) ?: ""
        )
        i.action = null
    }
}
