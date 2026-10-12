package com.spendtrack.ui

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendtrack.SpendViewModel
import com.spendtrack.data.Categories
import com.spendtrack.data.Fmt
import com.spendtrack.data.Stats

@Composable
fun InsightsScreen(vm: SpendViewModel) {
    val context = LocalContext.current
    val txs by vm.uiTxs.collectAsState()
    val budgets by vm.budgets.collectAsState()
    val allBudget = budgets.firstOrNull { it.category == Categories.ALL_BUDGET }?.monthlyLimit
    val stats = remember(txs, allBudget) { Stats.month(txs, allBudget) }
    val weekTotal = stats.last7.sumOf { it.second }

    LazyColumn(
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Insights", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Your spending this month. Friends' shares are not counted as yours.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat("💸 Spent", Fmt.money(stats.spent), Modifier.weight(1f))
                MiniStat("💰 Income", Fmt.money(stats.income), Modifier.weight(1f))
            }
        }

        if (stats.byCategory.isEmpty()) {
            item {
                AppCard {
                    EmptyState("📊", "No spending yet", "Your charts will appear here after your first recorded payment.")
                }
            }
        } else {
            item {
                AppCard {
                    Text("Where your money went", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        DonutChart(
                            slices = stats.byCategory.map { Color(Categories.info(it.first).color) to it.second },
                            modifier = Modifier.size(180.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Spent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Fmt.money(stats.spent), style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    stats.byCategory.forEach { (cat, amount) ->
                        val info = Categories.info(cat)
                        val pct = if (stats.spent > 0) (amount / stats.spent * 100).toInt() else 0
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EmojiBadge(info.emoji, Color(info.color), 34.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(cat, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text("$pct%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(10.dp))
                            Text(Fmt.money(amount), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
        }

        item {
            AppCard {
                Text("Last 7 days", style = MaterialTheme.typography.titleMedium)
                Text(
                    Fmt.money(weekTotal) + " total · " + Fmt.money(weekTotal / 7.0) + " a day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                BarChart(
                    values = stats.last7.map { it.second },
                    labels = stats.last7.map { it.first.take(3) },
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (stats.topMerchants.isNotEmpty()) {
            item {
                AppCard {
                    Text("Top spots", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    stats.topMerchants.forEachIndexed { i, (name, amount) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${i + 1}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(24.dp))
                            Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(Fmt.money(amount), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { shareText(context, vm.exportCsv()) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("📤 Export all records (CSV)") }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier) {
    AppCard(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Start, fontSize = 22.sp)
    }
}
