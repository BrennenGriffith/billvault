package com.brennensubs.billvault.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BillVaultApp() }
    }
}

private val purple = Color(0xFF6355DB)

@Composable
fun BillVaultApp(vm: VaultViewModel = viewModel()) {
    var dark by rememberSaveable { mutableStateOf(false) }
    MaterialTheme(
        colorScheme =
            if (dark) darkColorScheme(primary = Color(0xFFA89DFF))
            else lightColorScheme(primary = purple)
    ) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "BillVault.",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(onClick = { dark = !dark }) { Text(if (dark) "Light" else "Dark") }
                }
                if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                vm.error?.let {
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(it)
                            TextButton(onClick = vm::clearError) { Text("Dismiss") }
                        }
                    }
                }
                if (vm.profile == null) Onboarding(vm) else Workspace(vm)
            }
        }
    }
}

@Composable
private fun Onboarding(vm: VaultViewModel) {
    var modeName by rememberSaveable { mutableStateOf<String?>(null) }
    var url by rememberSaveable { mutableStateOf("") }
    var user by rememberSaveable { mutableStateOf("admin") }
    var secret by remember { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    var remember by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Your bills. Your choice.",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Choose where this vault lives. Connections keep their own records; switching never merges vaults."
        )
        if (modeName == null) {
            Choice(
                "Self-hosted web app",
                "Connect to your server URL and sign in. Sync reads and sends changes to that installation.",
            ) {
                modeName = VaultMode.SELF_HOSTED.name
            }
            Choice(
                "Standalone desktop",
                "Pair over your local network with a running desktop app. Tap Sync to exchange changes.",
            ) {
                modeName = VaultMode.DESKTOP.name
            }
            Choice(
                "Only on this phone",
                "Use BillVault offline with no server, desktop, or sign-in.",
            ) {
                vm.connect(VaultMode.LOCAL, "", "", "", "", false)
            }
            if (vm.savedProfiles.isNotEmpty()) {
                Text("Saved vaults", style = MaterialTheme.typography.titleMedium)
                vm.savedProfiles.forEach { p ->
                    OutlinedButton(onClick = { vm.selectSaved(p) }, enabled = !vm.busy) {
                        Text(p.label + " · " + p.url.ifBlank { "Offline" })
                    }
                }
            }
        } else {
            val mode = VaultMode.valueOf(modeName!!)
            Text(
                if (mode == VaultMode.DESKTOP) "Pair your desktop" else "Connect your server",
                style = MaterialTheme.typography.titleLarge,
            )
            if (mode == VaultMode.DESKTOP)
                Text(
                    "On desktop, open Storage & preferences → Android sync. Enable it, then copy a LAN address, certificate fingerprint, and one-time pairing code. Both devices must share a reachable local network."
                )
            if (mode == VaultMode.DESKTOP) {
                var pairingText by remember { mutableStateOf("") }
                Entry("Paste desktop pairing details (optional)", pairingText, { pairingText = it })
                TextButton(
                    onClick = {
                        runCatching {
                            val details = org.json.JSONObject(pairingText)
                            url = details.getString("url")
                            pin = details.getString("fingerprint")
                            secret = details.getString("code")
                        }
                            .onFailure {
                                vm.reportError("Paste the pairing details copied from your desktop")
                            }
                    }
                ) {
                    Text("Use pairing details")
                }
            }
            Entry("HTTPS address", url, { url = it })
            if (mode == VaultMode.SELF_HOSTED) {
                Entry("Username", user, { user = it })
                Entry("Password", secret, { secret = it }, password = true)
                Toggle("Remember sign-in encrypted on this device", remember) { remember = it }
            } else {
                Entry("Desktop certificate SHA256 fingerprint", pin, { pin = it })
                Entry("One-time pairing code", secret, { secret = it }, password = true)
                Text(
                    "Pairing tokens use Android Keystore encryption. Check the fingerprint against your desktop."
                )
            }
            Button(
                enabled = !vm.busy,
                onClick = { vm.connect(mode, url, user, secret, pin, remember) },
            ) {
                Text(if (mode == VaultMode.DESKTOP) "Pair and sync" else "Sign in and sync")
            }
            TextButton(
                onClick = {
                    modeName = null
                    secret = ""
                }
            ) {
                Text("Back")
            }
            if (BuildConfig.DEBUG)
                Text(
                    "Emulator development: http://10.0.2.2:8080 reaches a local web server. Release builds require HTTPS.",
                    style = MaterialTheme.typography.bodySmall,
                )
        }
    }
}

@Composable
private fun Choice(title: String, detail: String, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(detail)
        }
    }
}

@Composable
private fun Entry(
    label: String,
    value: String,
    change: (String) -> Unit,
    password: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = change,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        visualTransformation =
            if (password) PasswordVisualTransformation()
            else androidx.compose.ui.text.input.VisualTransformation.None,
    )
}

@Composable
private fun Toggle(label: String, checked: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = change)
    }
}

private fun money(currency: String, value: BigDecimal) =
    currency + " " + value.setScale(2, RoundingMode.HALF_UP).toPlainString()

@Composable
private fun ColumnScope.Workspace(vm: VaultViewModel) {
    val profile = vm.profile!!
    val data = vm.snapshot
    var tab by rememberSaveable { mutableStateOf("Overview") }
    var search by rememberSaveable { mutableStateOf("") }
    var editor by remember { mutableStateOf<Expense?>(null) }
    var adding by remember { mutableStateOf(false) }
    var paying by remember { mutableStateOf<Expense?>(null) }
    var discard by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(profile.label, fontWeight = FontWeight.SemiBold)
            Text(
                if (profile.mode == VaultMode.LOCAL) "Saved on this phone"
                else
                    vm.pending.size.toString() +
                        " pending · Last snapshot: " +
                        (data.generatedAt ?: "not synced"),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Button(enabled = !vm.busy, onClick = vm::sync) { Text("Sync") }
    }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp)
    ) {
        listOf("Overview", "Expenses", "Upcoming", "Payments", "Reports", "Settings").forEach {
            title ->
            TextButton(onClick = { tab = title }) {
                Text(title, fontWeight = if (tab == title) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
    vm.message?.let {
        Text(it, Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.bodySmall)
    }
    LazyColumn(
        Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (tab) {
            "Overview" -> {
                item {
                    Text(
                        "Your money, in view.",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                val active = data.expenses.filter { it.status == "ACTIVE" }
                if (active.isEmpty())
                    item { Text("Add your first bill or subscription. Downloads start empty.") }
                active
                    .groupBy { it.currency }
                    .forEach { (currency, expenses) ->
                        item {
                            val monthly =
                                expenses
                                    .map(Billing::monthly)
                                    .fold(BigDecimal.ZERO, BigDecimal::add)
                            Card(colors = CardDefaults.cardColors(containerColor = purple)) {
                                Column(
                                    Modifier.fillMaxWidth().padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text("Monthly commitment · " + currency, color = Color.White)
                                    Text(
                                        money(currency, monthly),
                                        color = Color.White,
                                        style = MaterialTheme.typography.headlineMedium,
                                    )
                                    Text(
                                        "Annual estimate: " +
                                            money(currency, monthly.multiply(BigDecimal(12))),
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                    }
                item {
                    Text(
                        active.size.toString() +
                            " active expenses · estimates are separate from actual payments"
                    )
                    Text("Coming up", style = MaterialTheme.typography.titleLarge)
                }
                items(active.sortedBy { it.nextDueDate }.take(8), key = { it.id }) {
                    ExpenseCard(it, data.categories, { editor = it }, { paying = it })
                }
            }
            "Expenses" -> {
                item {
                    Button(enabled = !vm.busy, onClick = { adding = true }) {
                        Text("+ Add expense")
                    }
                }
                item { Entry("Search expenses", search, { search = it }) }
                items(
                    data.expenses
                        .filter { it.name.contains(search, true) }
                        .sortedBy { it.name.lowercase() },
                    key = { it.id },
                ) {
                    ExpenseCard(it, data.categories, { editor = it }, { paying = it })
                }
            }
            "Upcoming" -> {
                item {
                    Text("Upcoming & overdue", style = MaterialTheme.typography.headlineSmall)
                    Text("Current next-due occurrences. Red dates are overdue.")
                }
                items(
                    data.expenses.filter { it.status == "ACTIVE" }.sortedBy { it.nextDueDate },
                    key = { it.id },
                ) {
                    ExpenseCard(it, data.categories, { editor = it }, { paying = it })
                }
            }
            "Payments" -> {
                item {
                    Text("Recorded payments", style = MaterialTheme.typography.headlineSmall)
                    Text("Queued payments appear after host confirmation.")
                }
                items(data.payments.sortedByDescending { it.paymentDate }, key = { it.id }) {
                    payment ->
                    Card {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(payment.expenseName, fontWeight = FontWeight.Bold)
                            Text(money(payment.currency, BigDecimal(payment.amount)))
                            Text(
                                "Paid " +
                                    payment.paymentDate +
                                    " · occurrence " +
                                    payment.occurrenceDate
                            )
                            if (payment.notes.isNotBlank()) Text(payment.notes)
                        }
                    }
                }
            }
            "Reports" -> {
                item {
                    Text("Actual payment totals", style = MaterialTheme.typography.headlineSmall)
                }
                data.payments
                    .groupBy { it.currency }
                    .forEach { (currency, payments) ->
                        item {
                            val month = YearMonth.now().toString()
                            val total =
                                payments
                                    .filter { it.paymentDate.startsWith(month) }
                                    .map { BigDecimal(it.amount) }
                                    .fold(BigDecimal.ZERO, BigDecimal::add)
                            val year =
                                payments
                                    .filter {
                                        it.paymentDate.startsWith(LocalDate.now().year.toString())
                                    }
                                    .map { BigDecimal(it.amount) }
                                    .fold(BigDecimal.ZERO, BigDecimal::add)
                            Card {
                                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                    Text(currency + " · " + month)
                                    Text("Month: " + money(currency, total))
                                    Text("Year: " + money(currency, year))
                                }
                            }
                        }
                    }
                item { VaultExports(vm) }
            }
            "Settings" -> {
                item {
                    Text("Connection & storage", style = MaterialTheme.typography.headlineSmall)
                    Text(profile.url.ifBlank { "No connection needed" })
                    Text("Switching vaults preserves each cache and its pending changes.")
                }
                if (profile.mode == VaultMode.SELF_HOSTED)
                    item {
                        var password by remember { mutableStateOf("") }
                        var keep by remember { mutableStateOf(false) }
                        Entry("Sign-in password", password, { password = it }, password = true)
                        Toggle("Remember encrypted on this device", keep) { keep = it }
                        Button(
                            enabled = !vm.busy,
                            onClick = {
                                vm.signIn(password, keep)
                                password = ""
                            },
                        ) {
                            Text("Sign in and sync")
                        }
                    }
                if (profile.mode == VaultMode.DESKTOP)
                    item {
                        Text(
                            "Keep desktop Android sync enabled on the same network. If its identity changes or your device is revoked, pair again with a fresh code."
                        )
                    }
                item {
                    OutlinedButton(enabled = !vm.busy, onClick = vm::switchVault) {
                        Text("Choose or add another vault")
                    }
                }
                item { VaultExports(vm) }
                if (profile.mode != VaultMode.LOCAL && vm.pending.isNotEmpty())
                    item {
                        OutlinedButton(enabled = !vm.busy, onClick = { discard = true }) {
                            Text("Discard pending changes and reload")
                        }
                    }
                item {
                    Text(
                        "Initial Android preview: expenses, payments, actual totals, backup/export, and manual sync. System notifications, category editing, and Android agent controls are not included yet.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
    if (adding || editor != null)
        ExpenseEditor(
            editor,
            data.categories,
            vm.busy,
            {
                adding = false
                editor = null
            },
            vm.error,
        ) { value ->
            vm.saveExpense(value, editor) {
                adding = false
                editor = null
            }
        }
    paying?.let { expense ->
        PaymentEditor(expense, vm.busy, { paying = null }, vm.error) { amount, date, notes ->
            vm.pay(expense, amount, date, notes) { paying = null }
        }
    }
    if (discard)
        AlertDialog(
            onDismissRequest = { discard = false },
            title = { Text("Discard pending changes?") },
            text = {
                Text(
                    "Unsent edits will be removed and host records reloaded. Changes already accepted by the host remain there."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.discardAndReload()
                        discard = false
                    }
                ) {
                    Text("Discard and reload")
                }
            },
            dismissButton = { TextButton(onClick = { discard = false }) { Text("Cancel") } },
        )
}

@Composable
private fun ExpenseCard(e: Expense, categories: List<Category>, edit: () -> Unit, pay: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(e.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                money(e.currency, BigDecimal(e.amount)) +
                    " · " +
                    e.frequency.lowercase() +
                    " · " +
                    e.status.lowercase()
            )
            Text(
                (categories.find { it.id == e.categoryId }?.name ?: "Category") +
                    " · due " +
                    e.nextDueDate,
                color =
                    if (e.nextDueDate < LocalDate.now().toString() && e.status == "ACTIVE")
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row {
                TextButton(onClick = edit) { Text("Edit") }
                if (e.status == "ACTIVE") TextButton(onClick = pay) { Text("Record payment") }
            }
        }
    }
}
