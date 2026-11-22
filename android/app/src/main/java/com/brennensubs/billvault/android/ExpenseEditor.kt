package com.brennensubs.billvault.android

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate

@Composable
fun ExpenseEditor(
    existing: Expense?,
    categories: List<Category>,
    busy: Boolean,
    onDismiss: () -> Unit,
    operationError: String? = null,
    onSave: (Expense) -> Unit,
) {
    val initial =
        existing
            ?: Expense(
                0,
                "",
                "SUBSCRIPTION",
                categories.firstOrNull()?.id ?: 1,
                "0.00",
                "USD",
                "MONTHLY",
                LocalDate.now().toString(),
                LocalDate.now().toString(),
            )
    var name by remember { mutableStateOf(initial.name) }
    var amount by remember { mutableStateOf(initial.amount) }
    var currency by remember { mutableStateOf(initial.currency) }
    var kind by remember { mutableStateOf(initial.kind) }
    var category by remember { mutableStateOf(initial.categoryId) }
    var frequency by remember { mutableStateOf(initial.frequency) }
    var start by remember { mutableStateOf(initial.startDate) }
    var due by remember { mutableStateOf(initial.nextDueDate) }
    var end by remember { mutableStateOf(initial.endDate ?: "") }
    var status by remember { mutableStateOf(initial.status) }
    var variable by remember { mutableStateOf(initial.variableAmount) }
    var interval by remember { mutableStateOf((initial.customInterval ?: 1).toString()) }
    var unit by remember { mutableStateOf(initial.customUnit ?: "MONTHS") }
    var notes by remember { mutableStateOf(initial.notes) }
    var method by remember { mutableStateOf(initial.paymentMethod) }
    var website by remember { mutableStateOf(initial.websiteUrl) }
    var description by remember { mutableStateOf(initial.description) }
    var validation by remember { mutableStateOf<String?>(null) }
    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    if (existing == null) "Add expense" else "Edit expense",
                    style = MaterialTheme.typography.headlineSmall,
                )
                validation?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                operationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                EditorField("Name", name) { name = it }
                Select("Kind", kind, listOf("BILL", "SUBSCRIPTION")) { kind = it }
                Select(
                    "Category",
                    category.toString(),
                    categories.map { it.id.toString() },
                    labelFor = { id -> categories.first { it.id.toString() == id }.name },
                ) {
                    category = it.toLong()
                }
                EditorField("Amount (two decimal places)", amount) { amount = it }
                EditorField("Currency", currency) { currency = it.uppercase() }
                Select(
                    "Frequency",
                    frequency,
                    listOf("WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY", "CUSTOM"),
                ) {
                    frequency = it
                }
                if (frequency == "CUSTOM") {
                    EditorField("Interval 1–3650", interval) { interval = it }
                    Select("Unit", unit, listOf("DAYS", "WEEKS", "MONTHS", "YEARS")) { unit = it }
                }
                EditorField("Start date YYYY-MM-DD", start) { start = it }
                EditorField("Next due date YYYY-MM-DD", due) { due = it }
                EditorField("End date (optional)", end) { end = it }
                Select("Status", status, listOf("ACTIVE", "PAUSED", "CANCELED")) { status = it }
                Row {
                    Text("Variable amount", Modifier.weight(1f))
                    Switch(variable, { variable = it })
                }
                EditorField("Description", description) { description = it }
                EditorField("Payment label (no account/card numbers)", method) { method = it }
                EditorField("Website URL", website) { website = it }
                EditorField("Notes", notes) { notes = it }
                Button(
                    enabled = !busy,
                    onClick = {
                        try {
                            val value =
                                initial.copy(
                                    name = name.trim(),
                                    amount = amount.trim(),
                                    currency = currency.trim(),
                                    kind = kind,
                                    categoryId = category,
                                    frequency = frequency,
                                    startDate = start.trim(),
                                    nextDueDate = due.trim(),
                                    endDate = end.trim().ifBlank { null },
                                    status = status,
                                    variableAmount = variable,
                                    customInterval =
                                        if (frequency == "CUSTOM") interval.toInt() else null,
                                    customUnit = if (frequency == "CUSTOM") unit else null,
                                    notes = notes,
                                    paymentMethod = method,
                                    websiteUrl = website,
                                    description = description,
                                )
                            value.validate(categories)
                            validation = null
                            onSave(value)
                        } catch (e: Exception) {
                            validation = e.message ?: "Check the expense fields"
                        }
                    },
                ) {
                    Text(if (busy) "Saving…" else "Save expense")
                }
                TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun EditorField(label: String, value: String, change: (String) -> Unit) {
    OutlinedTextField(value, change, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}

@Composable
fun Select(
    label: String,
    value: String,
    options: List<String>,
    labelFor: (String) -> String = { it.lowercase().replace('_', ' ') },
    change: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { open = true }) { Text(labelFor(value)) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(labelFor(option)) },
                    onClick = {
                        change(option)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
fun PaymentEditor(
    expense: Expense,
    busy: Boolean,
    dismiss: () -> Unit,
    operationError: String? = null,
    save: (String, String, String) -> Unit,
) {
    var amount by remember { mutableStateOf(expense.amount) }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!busy) dismiss() },
        title = { Text("Record " + expense.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Occurrence " + expense.nextDueDate + ". Recording does not transfer money.")
                operationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                EditorField("Actual amount", amount) { amount = it }
                EditorField("Paid date YYYY-MM-DD", date) { date = it }
                EditorField("Notes", notes) { notes = it }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = { save(amount, date, notes) }) {
                Text("Record payment")
            }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = dismiss) { Text("Cancel") } },
    )
}
