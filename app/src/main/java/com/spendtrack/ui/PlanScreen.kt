package com.spendtrack.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.spendtrack.SpendViewModel
import com.spendtrack.data.Categories
import com.spendtrack.data.Fmt
import com.spendtrack.data.Goal
import com.spendtrack.data.Stats

@Composable
fun PlanScreen(vm: SpendViewModel) {
    val txs by vm.uiTxs.collectAsState()
    val budgets by vm.budgets.collectAsState()
    val goals by vm.goals.collectAsState()

    val allBudget = budgets.firstOrNull { it.category == Categories.ALL_BUDGET }?.monthlyLimit
    val stats = remember(txs, allBudget) { Stats.month(txs, allBudget) }
    val spentByCategory = remember(stats) { stats.byCategory.toMap() }

    var budgetFor by remember { mutableStateOf<String?>(null) }
    var showGoal by remember { mutableStateOf(false) }
    var addFor by remember { mutableStateOf<Goal?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Plan", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Budgets keep you on track. Goals keep you saving.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            AppCard(onClick = { budgetFor = Categories.ALL_BUDGET }) {
                Text("💼 Monthly budget", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                if (allBudget == null) {
                    Text(
                        "Tap to set one. Finkar will then show how much you can safely spend every day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val frac = (stats.spent / allBudget).toFloat().coerceIn(0f, 1f)
                    Text(
                        Fmt.money(stats.spent) + " of " + Fmt.money(allBudget),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { frac },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = progressColor(frac),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${stats.daysLeft} days left · safe to spend " + Fmt.money(stats.safePerDay ?: 0.0) + " a day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { SectionTitle("Category limits") }
        item {
            AppCard {
                Categories.expense.forEach { info ->
                    val limit = budgets.firstOrNull { it.category == info.name }?.monthlyLimit
                    val spent = spentByCategory[info.name] ?: 0.0
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { budgetFor = info.name }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EmojiBadge(info.emoji, Color(info.color), 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(info.name, style = MaterialTheme.typography.titleSmall)
                            if (limit != null) {
                                val frac = (spent / limit).toFloat().coerceIn(0f, 1f)
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { frac },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = progressColor(frac),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Text(
                                    Fmt.money(spent) + " of " + Fmt.money(limit),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    "Spent " + Fmt.money(spent) + " · no limit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            if (limit == null) "Set" else "Edit",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item { SectionTitle("Savings goals", "＋ New") { showGoal = true } }
        if (goals.isEmpty()) {
            item {
                AppCard {
                    EmptyState("🎯", "Save for something", "A new phone, a trip, a laptop. Add a goal and watch it fill up.")
                }
            }
        } else {
            items(goals, key = { it.id }) { goal ->
                GoalCard(goal, onAdd = { addFor = goal }, onDelete = { vm.deleteGoal(goal.id) })
            }
        }
    }

    budgetFor?.let { key ->
        val current = budgets.firstOrNull { it.category == key }?.monthlyLimit
        BudgetDialog(
            title = if (key == Categories.ALL_BUDGET) "Monthly budget" else "$key limit",
            current = current,
            onDismiss = { budgetFor = null },
            onSave = { vm.setBudget(key, it); budgetFor = null },
            onRemove = if (current != null) ({ vm.removeBudget(key); budgetFor = null }) else null
        )
    }
    if (showGoal) {
        GoalDialog(
            onDismiss = { showGoal = false },
            onSave = { name, target -> vm.addGoal(name, target); showGoal = false }
        )
    }
    addFor?.let { goal ->
        AddMoneyDialog(
            goalName = goal.name,
            onDismiss = { addFor = null },
            onSave = { vm.addToGoal(goal, it); addFor = null }
        )
    }
}

@Composable
private fun GoalCard(goal: Goal, onAdd: () -> Unit, onDelete: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    val frac = (goal.saved / goal.target).toFloat().coerceIn(0f, 1f)
    AppCard {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("🎯 " + goal.name, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { if (confirm) onDelete() else confirm = true }) {
                Text(if (confirm) "Tap again" else "🗑", color = MaterialTheme.colorScheme.error)
            }
        }
        LinearProgressIndicator(
            progress = { frac },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
            color = Mint,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            Fmt.money(goal.saved) + " of " + Fmt.money(goal.target) + " (" + (frac * 100).toInt() + "%)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        if (goal.saved >= goal.target) {
            Text("🎉 Goal reached!", style = MaterialTheme.typography.titleSmall, color = Mint)
        } else {
            Button(onClick = onAdd) { Text("＋ Add money") }
        }
    }
}
