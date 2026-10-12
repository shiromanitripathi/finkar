package com.spendtrack.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.spendtrack.SpendViewModel
import com.spendtrack.data.Categories
import com.spendtrack.data.Fmt
import com.spendtrack.data.Stats
import com.spendtrack.data.Transaction

@Composable
fun HomeScreen(
    vm: SpendViewModel,
    onSeeAll: () -> Unit,
    onOpenFriends: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenTx: (Transaction) -> Unit
) {
    val txs by vm.uiTxs.collectAsState()
    val budgets by vm.budgets.collectAsState()
    val balances by vm.balances.collectAsState()
    val name by vm.userName.collectAsState()

    val allBudget = budgets.firstOrNull { it.category == Categories.ALL_BUDGET }?.monthlyLimit
    val stats = remember(txs, allBudget) { Stats.month(txs, allBudget) }
    val owedToMe = balances.sumOf { maxOf(it.pending, 0.0) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    if (name.isBlank()) "Hello 👋" else "Hi, $name 👋",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    "Here is your money this month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { HeroCard(stats.spent, stats.budget, stats.left, stats.safePerDay, onOpenPlan) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    emoji = "💰", label = "Income", value = Fmt.money(stats.income),
                    modifier = Modifier.weight(1f), onClick = null
                )
                StatCard(
                    emoji = "👥", label = "Friends owe you", value = Fmt.money(owedToMe),
                    modifier = Modifier.weight(1f), onClick = onOpenFriends
                )
            }
        }

        item { SetupCard() }

        item { SectionTitle("Recent payments", "See all", onSeeAll) }

        val recent = txs.take(6)
        if (recent.isEmpty()) {
            item {
                AppCard {
                    EmptyState(
                        "✨", "Nothing here yet",
                        "When you pay with UPI or card, a pop-up will ask if you want to record it. You can also tap ＋ Add."
                    )
                }
            }
        } else {
            item {
                AppCard {
                    recent.forEach { TxRow(it, onClick = { onOpenTx(it.tx) }) }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    spent: Double,
    budget: Double?,
    left: Double?,
    safePerDay: Double?,
    onOpenPlan: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Purple, PurpleLight)))
            .padding(22.dp)
    ) {
        Column {
            Text("Spent this month", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
            Text(Fmt.money(spent), color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            if (budget != null && left != null) {
                val frac = (spent / budget).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { frac },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(if (left >= 0) "Left to spend" else "Over budget by", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                        Text(Fmt.money(kotlin.math.abs(left)), color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Safe to spend / day", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                        Text(Fmt.money(safePerDay ?: 0.0), color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                Text(
                    "Set a monthly budget to see how much you can spend each day.",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = onOpenPlan) { Text("Set budget →", color = Color.White) }
            }
        }
    }
}

@Composable
private fun StatCard(
    emoji: String,
    label: String,
    value: String,
    modifier: Modifier,
    onClick: (() -> Unit)?
) {
    AppCard(modifier = modifier, onClick = onClick) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun SetupCard() {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }

    val smsOk = remember(refresh) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED
    }
    val notifOk = remember(refresh) {
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
    val listenerOk = remember(refresh) {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
    if (smsOk && notifOk && listenerOk) return

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refresh++
    }

    AppCard {
        Text("⚡ Turn on automatic capture", style = MaterialTheme.typography.titleMedium)
        Text(
            "Finkar needs three permissions to notice your payments and ask you instantly.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        SetupRow("Bank SMS alerts", smsOk) {
            launcher.launch(arrayOf(Manifest.permission.RECEIVE_SMS))
        }
        SetupRow("Pop-up prompts", notifOk) {
            if (Build.VERSION.SDK_INT >= 33) {
                launcher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }
        SetupRow("UPI apps (GPay, PhonePe, Paytm…)", listenerOk) {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

@Composable
private fun SetupRow(label: String, done: Boolean, onFix: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text((if (done) "✅  " else "⬜  ") + label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (!done) {
            OutlinedButton(onClick = onFix, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)) {
                Text("Allow")
            }
        }
    }
}
