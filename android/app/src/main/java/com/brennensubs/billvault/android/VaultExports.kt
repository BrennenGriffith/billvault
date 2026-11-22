package com.brennensubs.billvault.android

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
fun VaultExports(vm: VaultViewModel) {
    val context = LocalContext.current
    var restore by remember { mutableStateOf<String?>(null) }
    val backup =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri != null)
                runCatching {
                    val text = vm.exportBackup()
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(text.toByteArray())
                    }
                }
                    .onFailure { vm.reportError(it.message ?: "Backup failed") }
        }
    val csv =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri
            ->
            if (uri != null)
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(paymentCsv(vm.snapshot.payments).toByteArray())
                    }
                }
                    .onFailure { vm.reportError(it.message ?: "Export failed") }
        }
    val importer =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null)
                runCatching {
                    context.contentResolver.openInputStream(uri)!!.use { input ->
                        val out = java.io.ByteArrayOutputStream()
                        val bytes = ByteArray(8192)
                        var count: Int
                        while (input.read(bytes).also { count = it } > 0) {
                            require(out.size() + count <= 5 * 1024 * 1024) {
                                "Backup exceeds 5 MB"
                            }
                            out.write(bytes, 0, count)
                        }
                        restore = out.toString("UTF-8")
                    }
                }
                    .onFailure { vm.reportError(it.message ?: "Import failed") }
        }
    Column {
        OutlinedButton(onClick = { csv.launch("billvault-payments.csv") }) {
            Text("Export payment CSV")
        }
        OutlinedButton(
            enabled = vm.pending.isEmpty(),
            onClick = { backup.launch("billvault-backup.json") },
        ) {
            Text("Export vault backup")
        }
        if (vm.profile?.mode == VaultMode.LOCAL)
            OutlinedButton(
                onClick = { importer.launch(arrayOf("application/json", "text/plain")) }
            ) {
                Text("Restore local backup")
            }
    }
    restore?.let { text ->
        AlertDialog(
            onDismissRequest = { restore = null },
            title = { Text("Replace this phone vault?") },
            text = {
                Text(
                    "Restore replaces the selected phone-only vault. Export a backup first if you need its current records."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.restoreBackup(text)
                        restore = null
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = { TextButton(onClick = { restore = null }) { Text("Cancel") } },
        )
    }
}

fun paymentCsv(payments: List<Payment>): String {
    fun quote(value: String): String {
        val safe =
            if (value.trimStart().firstOrNull() in listOf('=', '+', '-', '@', '\t', '\r'))
                "'" + value
            else value
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }
    return "Expense,Occurrence,Payment date,Amount,Currency,Notes\r\n" +
        payments
            .sortedBy { it.paymentDate }
            .joinToString("\r\n") {
                listOf(
                        it.expenseName,
                        it.occurrenceDate,
                        it.paymentDate,
                        it.amount,
                        it.currency,
                        it.notes,
                    )
                    .joinToString(",", transform = ::quote)
            }
}
