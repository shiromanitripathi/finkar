package com.spendtrack.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spendtrack.data.Categories
import com.spendtrack.data.Fmt
import com.spendtrack.data.Friend
import com.spendtrack.data.Transaction

private fun numText(d: Double): String =
    if (d % 1.0 == 0.0) d.toLong().toString() else Fmt.round2(d).toString()

@Composable
private fun TextInput(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TxEditorDialog(
    initial: Transaction?,
    onDismiss: () -> Unit,
    onSave: (amount: Double, merchant: String, category: String, isIncome: Boolean, note: String) -> Unit,
    onDelete: (() -> Unit)? = null,
    onSplit: (() -> Unit)? = null,
    splitInfo: String? = null
) {
    var isIncome by remember { mutableStateOf(initial?.isIncome ?: false) }
    var amount by remember { mutableStateOf(initial?.let { numText(it.amount) } ?: "") }
    var merchant by remember { mutableStateOf(initial?.merchant ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: Categories.OTHERS) }
    var touched by remember { mutableStateOf(initial != null) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }
    val amt = amount.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add transaction" else "Transaction") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        label = { Text("💸 Expense") }
                    )
                    FilterChip(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        label = { Text("💰 Income") }
                    )
                }
                AmountField(amount, { amount = it })
                TextInput(
                    value = merchant,
                    onChange = {
                        merchant = it
                        if (!touched && !isIncome) category = Categories.guess(it)
                    },
                    label = if (isIncome) "From (e.g. Pocket money)" else "Paid to"
                )
                if (!isIncome) {
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Categories.expense.forEach { info ->
                            FilterChip(
                                selected = category == info.name,
                                onClick = { category = info.name; touched = true },
                                label = { Text(info.emoji + " " + info.name) }
                            )
                        }
                    }
                }
                TextInput(note, { note = it }, "Note (optional)")
                if (onSplit != null && !isIncome) {
                    if (splitInfo != null) {
                        Text(
                            splitInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    OutlinedButton(onClick = onSplit, modifier = Modifier.fillMaxWidth()) {
                        Text("👥 Split with friends")
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = { if (confirmDelete) onDelete() else confirmDelete = true }) {
                        Text(
                            if (confirmDelete) "Tap again to delete" else "🗑 Delete",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = amt > 0.0 && merchant.isNotBlank(),
                onClick = {
                    onSave(
                        amt, merchant.trim(),
                        if (isIncome) Categories.INCOME else category,
                        isIncome, note.trim()
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SplitDialog(
    tx: Transaction,
    friends: List<Friend>,
    existing: Map<Long, Double>,
    onAddFriend: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (Map<Long, Double>) -> Unit
) {
    var selected by remember { mutableStateOf(existing.keys.toSet()) }
    var includeMe by remember {
        mutableStateOf(existing.isEmpty() || existing.values.sum() < tx.amount - 0.01)
    }
    var custom by remember { mutableStateOf(false) }
    val customText = remember {
        mutableStateMapOf<Long, String>().apply { existing.forEach { (k, v) -> put(k, numText(v)) } }
    }

    val people = selected.size + if (includeMe) 1 else 0
    val equalEach = if (people > 0) tx.amount / people else 0.0
    val customTotal = selected.sumOf { customText[it]?.toDoubleOrNull() ?: 0.0 }
    val overLimit = custom && customTotal > tx.amount + 0.01

    fun result(): Map<Long, Double> =
        if (custom) selected.associateWith { customText[it]?.toDoubleOrNull() ?: 0.0 }
        else selected.associateWith { Fmt.round2(equalEach) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Split " + Fmt.money(tx.amount)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "You paid for ${tx.merchant}. Pick who shared it and Finkar will track what each friend owes you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Who shared it?", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    friends.forEach { f ->
                        FilterChip(
                            selected = f.id in selected,
                            onClick = {
                                selected = if (f.id in selected) selected - f.id else selected + f.id
                            },
                            label = { Text(f.name) }
                        )
                    }
                    AssistChip(onClick = onAddFriend, label = { Text("＋ New friend") })
                }

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("I'm part of it too", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = includeMe, onCheckedChange = { includeMe = it })
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !custom, onClick = { custom = false }, label = { Text("Equal") })
                    FilterChip(selected = custom, onClick = { custom = true }, label = { Text("Custom amounts") })
                }

                if (!custom) {
                    if (people > 0 && selected.isNotEmpty()) {
                        Text(
                            "Each pays " + Fmt.money(equalEach) + " ($people people)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    friends.filter { it.id in selected }.forEach { f ->
                        AmountField(
                            value = customText[f.id] ?: "",
                            onChange = { customText[f.id] = it },
                            label = f.name + " owes"
                        )
                    }
                    if (selected.isNotEmpty()) {
                        Text(
                            if (overLimit) "Friends' total is more than " + Fmt.money(tx.amount)
                            else "Friends owe " + Fmt.money(customTotal) +
                                ", your share " + Fmt.money(tx.amount - customTotal),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (overLimit) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !overLimit && (selected.isNotEmpty() || existing.isNotEmpty()),
                onClick = { onSave(result()) }
            ) { Text(if (selected.isEmpty()) "Clear split" else "Save split") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddFriendDialog(onDismiss: () -> Unit, onSave: (name: String, phone: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add friend") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextInput(name, { name = it }, "Name")
                TextInput(phone, { phone = it.filter { c -> c.isDigit() || c == '+' } }, "Phone (optional, for WhatsApp)", KeyboardType.Phone)
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = { onSave(name.trim(), phone.trim()) }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PaybackDialog(
    friendName: String,
    pending: Double,
    onDismiss: () -> Unit,
    onSave: (amount: Double, note: String) -> Unit
) {
    var amount by remember { mutableStateOf(if (pending > 0) numText(pending) else "") }
    var note by remember { mutableStateOf("") }
    val amt = amount.toDoubleOrNull() ?: 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$friendName paid you back") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (pending > 0) {
                    Text(
                        "Pending: " + Fmt.money(pending),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AmountField(amount, { amount = it })
                TextInput(note, { note = it }, "Note (e.g. UPI, cash)")
            }
        },
        confirmButton = {
            Button(enabled = amt > 0.0, onClick = { onSave(amt, note) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun BudgetDialog(
    title: String,
    current: Double?,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    onRemove: (() -> Unit)?
) {
    var amount by remember { mutableStateOf(current?.let { numText(it) } ?: "") }
    val amt = amount.toDoubleOrNull() ?: 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { AmountField(amount, { amount = it }, label = "Monthly limit") },
        confirmButton = {
            Button(enabled = amt > 0.0, onClick = { onSave(amt) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onRemove != null) {
                    TextButton(onClick = onRemove) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
fun GoalDialog(onDismiss: () -> Unit, onSave: (name: String, target: Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    val amt = target.toDoubleOrNull() ?: 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New savings goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextInput(name, { name = it }, "Goal (e.g. New laptop, Trip)")
                AmountField(target, { target = it }, label = "Target amount")
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank() && amt > 0.0, onClick = { onSave(name.trim(), amt) }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddMoneyDialog(goalName: String, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }
    val amt = amount.toDoubleOrNull() ?: 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to $goalName") },
        text = { AmountField(amount, { amount = it }) },
        confirmButton = { Button(enabled = amt > 0.0, onClick = { onSave(amt) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ProfileDialog(
    name: String,
    upi: String,
    onDismiss: () -> Unit,
    onSave: (name: String, upi: String) -> Unit
) {
    var n by remember { mutableStateOf(name) }
    var u by remember { mutableStateOf(upi) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Your UPI ID is added to reminders so friends know where to pay you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextInput(n, { n = it }, "Your name")
                TextInput(u, { u = it }, "Your UPI ID (e.g. name@okaxis)")
            }
        },
        confirmButton = { Button(onClick = { onSave(n, u) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
