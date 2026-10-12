package com.spendtrack.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendtrack.SpendViewModel
import com.spendtrack.data.Fmt
import com.spendtrack.data.FriendBalance
import com.spendtrack.data.Ledger
import com.spendtrack.data.LedgerEntry
import kotlin.math.abs

private val avatarColors = listOf(
    Color(0xFF6C5CE7), Color(0xFF00B894), Color(0xFFFF7675),
    Color(0xFF0984E3), Color(0xFFE84393), Color(0xFFF0A500)
)

@Composable
private fun Avatar(name: String, size: Dp = 46.dp) {
    val color = avatarColors[abs(name.hashCode()) % avatarColors.size]
    Box(
        Modifier.size(size).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.trim().take(1).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp
        )
    }
}

@Composable
fun FriendsScreen(vm: SpendViewModel, onAddFriend: () -> Unit) {
    val balances by vm.balances.collectAsState()
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    BackHandler(enabled = selectedId != null) { selectedId = null }

    val selected = balances.firstOrNull { it.friend.id == selectedId }
    if (selected != null) {
        FriendDetail(vm, selected, onBack = { selectedId = null })
    } else {
        FriendList(vm, balances, onAddFriend, onOpen = { selectedId = it })
    }
}

@Composable
private fun FriendList(
    vm: SpendViewModel,
    balances: List<FriendBalance>,
    onAddFriend: () -> Unit,
    onOpen: (Long) -> Unit
) {
    val upi by vm.upiId.collectAsState()
    val name by vm.userName.collectAsState()
    var showProfile by remember { mutableStateOf(false) }
    val totalOwed = balances.sumOf { maxOf(it.pending, 0.0) }
    val owing = balances.count { !it.settled }

    LazyColumn(
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Friends & splits", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "You pay, friends owe you. Ask for it later with full records.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Mint, Color(0xFF00CEC9))))
                    .padding(22.dp)
            ) {
                Column {
                    Text("Friends owe you", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                    Text(Fmt.money(totalOwed), color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (owing == 0) "Everyone is settled ✅" else "$owing friend" + (if (owing > 1) "s" else "") + " pending",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        item {
            AppCard(onClick = { showProfile = true }) {
                Text("🪪 Your UPI ID (added to reminders)", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (upi.isBlank()) "Tap to add, so friends know where to pay you" else upi,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Button(onClick = onAddFriend, modifier = Modifier.fillMaxWidth()) { Text("＋ Add friend") }
        }

        if (balances.isEmpty()) {
            item {
                AppCard {
                    EmptyState(
                        "👥", "No friends yet",
                        "Add friends, then open any payment and tap Split with friends to track who owes what."
                    )
                }
            }
        } else {
            items(balances, key = { it.friend.id }) { b ->
                AppCard(onClick = { onOpen(b.friend.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(b.friend.name)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(b.friend.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (b.settled) "All settled ✅" else "owes you",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!b.settled) {
                            Text(
                                Fmt.money(b.pending),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showProfile) {
        ProfileDialog(
            name = name, upi = upi,
            onDismiss = { showProfile = false },
            onSave = { n, u -> vm.saveProfile(n, u); showProfile = false }
        )
    }
}

@Composable
private fun FriendDetail(vm: SpendViewModel, b: FriendBalance, onBack: () -> Unit) {
    val context = LocalContext.current
    val txs by vm.transactions.collectAsState()
    val shares by vm.shares.collectAsState()
    val paybacks by vm.paybacks.collectAsState()
    val myName by vm.userName.collectAsState()
    val upi by vm.upiId.collectAsState()

    val entries = remember(txs, shares, paybacks, b.friend.id) {
        Ledger.entries(b.friend.id, txs, shares, paybacks)
    }
    val message = remember(entries, b.pending, myName, upi) {
        Ledger.reminderText(b.friend.name, myName, upi, entries, b.pending)
    }
    var showPayback by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TextButton(onClick = onBack) { Text("← Back") } }
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Purple, PurpleLight)))
                    .padding(22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(b.friend.name, 56.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(b.friend.name, color = Color.White, style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (b.settled) "All settled ✅" else "owes you " + Fmt.money(b.pending),
                            color = Color.White.copy(alpha = 0.92f),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Total shared " + Fmt.money(b.owed) + " · paid back " + Fmt.money(b.paidBack),
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (b.friend.phone.isBlank()) shareText(context, message)
                        else openWhatsApp(context, b.friend.phone, message)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (b.settled) "💬 Send statement" else "💬 Remind with all records") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { shareText(context, message) }, modifier = Modifier.weight(1f)) {
                        Text("📤 Share")
                    }
                    OutlinedButton(onClick = { showPayback = true }, modifier = Modifier.weight(1f)) {
                        Text("💵 Got paid")
                    }
                }
            }
        }
        item { SectionTitle("Records") }
        if (entries.isEmpty()) {
            item {
                AppCard {
                    EmptyState(
                        "🧾", "Nothing shared yet",
                        "Open a payment and tap Split with friends to add it here."
                    )
                }
            }
        } else {
            item {
                AppCard {
                    entries.forEach { LedgerRow(it) }
                }
            }
        }
        item {
            TextButton(onClick = {
                if (confirmDelete) { vm.deleteFriend(b.friend.id); onBack() } else confirmDelete = true
            }) {
                Text(
                    if (confirmDelete) "Tap again: delete friend and all records" else "🗑 Delete friend",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showPayback) {
        PaybackDialog(
            friendName = b.friend.name,
            pending = b.pending,
            onDismiss = { showPayback = false },
            onSave = { amount, note -> vm.recordPayback(b.friend.id, amount, note); showPayback = false }
        )
    }
}

@Composable
private fun LedgerRow(e: LedgerEntry) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(e.title, style = MaterialTheme.typography.titleSmall)
            Text(
                Fmt.dateTime(e.timestamp) + if (e.isPayback) " · paid back" else " · shared expense",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            (if (e.isPayback) "−" else "+") + Fmt.money(e.amount),
            style = MaterialTheme.typography.titleSmall,
            color = if (e.isPayback) Mint else MaterialTheme.colorScheme.onSurface
        )
    }
}
