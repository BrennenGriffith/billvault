package com.brennensubs.billvault.android

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

class VaultViewModel(application: Application) : AndroidViewModel(application) {
    private val db = VaultStorage(application)
    private val secrets = SecretStorage(application)
    private val gate = Mutex()
    private val sessionSecrets = mutableMapOf<String, String>()
    var profile by mutableStateOf<Profile?>(null)
        private set

    var savedProfiles by mutableStateOf<List<Profile>>(emptyList())
        private set

    var snapshot by mutableStateOf(Snapshot.empty())
        private set

    var pending by mutableStateOf<List<Pending>>(emptyList())
        private set

    var busy by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var message by mutableStateOf<String?>(null)
        private set

    init {
        action {
            profile = db.active()
            reload()
        }
    }

    private fun localId() =
        -(UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE).coerceAtLeast(1)

    private fun reload() {
        savedProfiles = db.profiles()
        profile?.let {
            snapshot = db.snapshot(it.id)
            pending = db.pending(it.id)
        }
    }

    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            gate.withLock {
                busy = true
                error = null
                try {
                    withContext(Dispatchers.IO) { block() }
                } catch (e: Exception) {
                    error = e.message ?: "Operation failed; records were preserved"
                } finally {
                    busy = false
                }
            }
        }
    }

    fun clearError() {
        error = null
    }

    fun reportError(text: String) {
        error = text
    }

    fun selectSaved(value: Profile) {
        action {
            db.select(value)
            profile = value
            reload()
        }
    }

    fun connect(
        mode: VaultMode,
        url: String,
        user: String,
        secret: String,
        fingerprint: String,
        remember: Boolean,
    ) {
        action {
            val p =
                Profile(
                    mode = mode,
                    label =
                        when (mode) {
                            VaultMode.LOCAL -> "Phone-only vault"
                            VaultMode.DESKTOP -> "Desktop vault"
                            else -> "Self-hosted vault"
                        },
                    url = url.trim(),
                    username = user.trim(),
                    fingerprint = fingerprint.trim(),
                )
            var initial = Snapshot.empty()
            var actualSecret = secret
            if (mode != VaultMode.LOCAL) {
                val remote = RemoteVault(p, if (mode == VaultMode.DESKTOP) "" else secret)
                if (mode == VaultMode.DESKTOP) actualSecret = remote.pair(secret.trim())
                else remote.login()
                initial =
                    if (mode == VaultMode.DESKTOP) RemoteVault(p, actualSecret).snapshot()
                    else remote.snapshot()
            }
            db.select(p)
            db.save(p.id, initial)
            sessionSecrets[p.id] = actualSecret
            if (mode == VaultMode.DESKTOP || remember) secrets.put(p.id, actualSecret)
            profile = p
            reload()
            message = "Connected to ${p.label}"
        }
    }

    fun signIn(secret: String, remember: Boolean) {
        profile?.let {
            sessionSecrets[it.id] = secret
            if (remember) secrets.put(it.id, secret) else secrets.remove(it.id)
        }
        sync()
    }

    fun switchVault() {
        action {
            profile = null
            db.clearSelection()
            snapshot = Snapshot.empty()
            pending = emptyList()
        }
    }

    fun sync() {
        action {
            val p = profile ?: return@action
            if (p.mode == VaultMode.LOCAL) {
                reload()
                message = "Local records are up to date"
                return@action
            }
            val secret =
                sessionSecrets[p.id]
                    ?: secrets.get(p.id)
                    ?: error("Sign in or pair again in settings before syncing")
            val remote = RemoteVault(p, secret)
            remote.login()
            for (command in db.pending(p.id)) {
                val result = remote.command(command)
                val current = db.snapshot(p.id)
                val expense = Expense.parse(result.getJSONObject("expense"))
                val payments =
                    if (result.isNull("payment")) current.payments
                    else
                        current.payments.filterNot {
                            it.id == result.getJSONObject("payment").getLong("id")
                        } + Payment.parse(result.getJSONObject("payment"))
                db.save(
                    p.id,
                    current.copy(
                        expenses =
                            current.expenses.filterNot {
                                it.id == command.localId || it.id == expense.id
                            } + expense,
                        payments = payments,
                    ),
                    acknowledge = command.id,
                )
                reload()
            }
            db.save(p.id, remote.snapshot())
            reload()
            message = "Synced ${Instant.now()}"
        }
    }

    fun saveExpense(value: Expense, existing: Expense?, onSaved: () -> Unit = {}) {
        action {
            val p = profile ?: error("Choose a vault")
            value.validate(snapshot.categories)
            if (existing != null)
                require(pending.none { it.localId == existing.id }) {
                    "Sync this expense's pending change before editing it again"
                }
            val expense =
                value.copy(id = existing?.id ?: localId(), version = existing?.version ?: 0)
            val operation = if (existing == null) "CREATE" else "UPDATE"
            val queued =
                if (p.mode == VaultMode.LOCAL) null
                else
                    Pending(
                        UUID.randomUUID().toString(),
                        operation,
                        expense.id,
                        existing?.id,
                        expense.request(),
                    )
            db.save(
                p.id,
                snapshot.copy(
                    expenses = snapshot.expenses.filterNot { it.id == expense.id } + expense
                ),
                queued,
            )
            reload()
            message =
                if (queued == null) "Expense saved"
                else "Expense saved locally; tap Sync to send it"
            withContext(Dispatchers.Main) { onSaved() }
        }
    }

    fun pay(
        expense: Expense,
        amount: String,
        date: String,
        notes: String,
        onSaved: () -> Unit = {},
    ) {
        action {
            val p = profile ?: error("Choose a vault")
            require(expense.status == "ACTIVE") { "Only active expenses can be paid" }
            require(pending.none { it.localId == expense.id }) {
                "Sync this expense's pending change before recording a payment"
            }
            require(
                amount.matches(Regex("[0-9]{1,10}\\.[0-9]{2}")) &&
                    BigDecimal(amount) > BigDecimal.ZERO
            ) {
                "Enter a positive amount with two decimal places"
            }
            require(!LocalDate.parse(date).isAfter(LocalDate.now())) {
                "Payment date cannot be in the future"
            }
            require(notes.length <= 2000) { "Notes are too long" }
            require(
                snapshot.payments.none {
                    it.expenseId == expense.id && it.occurrenceDate == expense.nextDueDate
                }
            ) {
                "This occurrence has already been paid"
            }
            val next = Billing.next(expense)
            val ended = expense.endDate?.let { next.isAfter(LocalDate.parse(it)) } ?: false
            val updated =
                expense.copy(
                    nextDueDate = if (ended) expense.nextDueDate else next.toString(),
                    status = if (ended) "CANCELED" else expense.status,
                )
            val payment =
                Payment(
                    localId(),
                    expense.id,
                    expense.name,
                    expense.nextDueDate,
                    date,
                    amount,
                    expense.currency,
                    notes,
                )
            val payload =
                JSONObject()
                    .put("occurrenceDate", expense.nextDueDate)
                    .put("paymentDate", date)
                    .put("amount", amount)
                    .put("notes", notes)
            val queued =
                if (p.mode == VaultMode.LOCAL) null
                else Pending(UUID.randomUUID().toString(), "PAY", expense.id, expense.id, payload)
            // Remote payment rows are added after host acknowledgement to prevent duplicate cached
            // history.
            db.save(
                p.id,
                snapshot.copy(
                    expenses = snapshot.expenses.filterNot { it.id == expense.id } + updated,
                    payments =
                        if (queued == null) snapshot.payments + payment else snapshot.payments,
                ),
                queued,
            )
            reload()
            message =
                if (queued == null) "Payment recorded"
                else "Payment queued; tap Sync to confirm with host"
            withContext(Dispatchers.Main) { onSaved() }
        }
    }

    fun discardAndReload() {
        action {
            val p = profile ?: return@action
            require(p.mode != VaultMode.LOCAL) { "No remote changes to discard" }
            val secret = sessionSecrets[p.id] ?: secrets.get(p.id) ?: error("Sign in first")
            val remote = RemoteVault(p, secret)
            remote.login()
            val fresh = remote.snapshot()
            db.discard(p.id)
            db.save(p.id, fresh)
            reload()
            message = "Pending edits discarded; host records reloaded"
        }
    }

    fun exportBackup(): String {
        require(pending.isEmpty()) { "Sync pending changes before exporting" }
        return snapshot.json().toString(2)
    }

    fun restoreBackup(text: String) {
        action {
            val p = profile ?: return@action
            require(p.mode == VaultMode.LOCAL) { "Restore into a phone-only vault" }
            val restored = Snapshot.parse(JSONObject(text))
            restored.validate()
            db.save(p.id, restored)
            reload()
            message = "Backup restored"
        }
    }

    override fun onCleared() {
        db.close()
    }
}
