package com.spendtrack.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spendtrack.SpendViewModel
import com.spendtrack.data.Categories
import com.spendtrack.data.Fmt
import com.spendtrack.data.Transaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(vm: SpendViewModel, onOpenTx: (Transaction) -> Unit) {
    val txs by vm.uiTxs.collectAsState()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<String?>(null) }

    val shown = remember(txs, query, filter) {
        txs.filter { item ->
            val q = query.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                item.tx.merchant.lowercase().contains(q) ||
                item.tx.note.lowercase().contains(q) ||
                item.tx.category.lowercase().contains(q)
            val matchesFilter = filter == null ||
                (filter == Categories.INCOME && item.tx.isIncome) ||
                item.tx.category == filter
            matchesQuery && matchesFilter
        }
    }
    val grouped = remember(shown) { shown.groupBy { Fmt.dayLabel(it.tx.timestamp) } }

    LazyColumn(
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text("All payments", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("🔍  Search by name, note or category") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filter == null,
                        onClick = { filter = null },
                        label = { Text("All") }
                    )
                }
                items(Categories.expense) { info ->
                    FilterChip(
                        selected = filter == info.name,
                        onClick = { filter = if (filter == info.name) null else info.name },
                        label = { Text(info.emoji + " " + info.name) }
                    )
                }
                item {
                    FilterChip(
                        selected = filter == Categories.INCOME,
                        onClick = { filter = if (filter == Categories.INCOME) null else Categories.INCOME },
                        label = { Text("💰 Income") }
                    )
                }
            }
        }

        if (shown.isEmpty()) {
            item {
                AppCard {
                    EmptyState(
                        "🧾",
                        if (txs.isEmpty()) "No payments yet" else "No matches",
                        if (txs.isEmpty()) "Tap ＋ Add to record one, or just pay with UPI and tap Record on the pop-up."
                        else "Try a different search or category."
                    )
                }
            }
        } else {
            grouped.forEach { (day, list) ->
                item(key = "header-$day") {
                    Text(
                        day,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                item(key = "group-$day") {
                    AppCard {
                        list.forEach { TxRow(it, onClick = { onOpenTx(it.tx) }) }
                    }
                }
            }
        }
    }
}
