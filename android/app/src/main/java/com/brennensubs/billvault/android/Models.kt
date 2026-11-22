package com.brennensubs.billvault.android

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

enum class VaultMode {
    LOCAL,
    SELF_HOSTED,
    DESKTOP,
}

data class Profile(
    val id: String = UUID.randomUUID().toString(),
    val mode: VaultMode,
    val label: String,
    val url: String = "",
    val username: String = "",
    val fingerprint: String = "",
) {
    fun json() =
        JSONObject()
            .put("id", id)
            .put("mode", mode.name)
            .put("label", label)
            .put("url", url)
            .put("username", username)
            .put("fingerprint", fingerprint)

    companion object {
        fun parse(j: JSONObject) =
            Profile(
                j.getString("id"),
                VaultMode.valueOf(j.getString("mode")),
                j.getString("label"),
                j.optString("url"),
                j.optString("username"),
                j.optString("fingerprint"),
            )
    }
}

data class Category(val id: Long, val name: String, val color: String = "#6355DB") {
    fun json() = JSONObject().put("id", id).put("name", name).put("color", color)

    companion object {
        fun parse(j: JSONObject) =
            Category(j.getLong("id"), j.getString("name"), j.optString("color", "#6355DB"))
    }
}

data class Expense(
    val id: Long,
    val name: String,
    val kind: String,
    val categoryId: Long,
    val amount: String,
    val currency: String,
    val frequency: String,
    val startDate: String,
    val nextDueDate: String,
    val status: String = "ACTIVE",
    val variableAmount: Boolean = false,
    val version: Long = 0,
    val description: String = "",
    val paymentMethod: String = "",
    val websiteUrl: String = "",
    val notes: String = "",
    val endDate: String? = null,
    val customInterval: Int? = null,
    val customUnit: String? = null,
) {
    fun json() =
        JSONObject()
            .put("id", id)
            .put("name", name)
            .put("kind", kind)
            .put("categoryId", categoryId)
            .put("amount", amount)
            .put("currency", currency)
            .put("frequency", frequency)
            .put("startDate", startDate)
            .put("nextDueDate", nextDueDate)
            .put("status", status)
            .put("variableAmount", variableAmount)
            .put("version", version)
            .put("description", description)
            .put("paymentMethod", paymentMethod)
            .put("websiteUrl", websiteUrl)
            .put("notes", notes)
            .put("endDate", endDate ?: JSONObject.NULL)
            .put("customInterval", customInterval ?: JSONObject.NULL)
            .put("customUnit", customUnit ?: JSONObject.NULL)

    fun request() = json().apply { remove("id") }

    fun validate(categories: List<Category>) {
        require(name.isNotBlank() && name.length <= 120) { "Name must contain 1–120 characters" }
        require(kind in listOf("BILL", "SUBSCRIPTION")) { "Choose bill or subscription" }
        require(categories.any { it.id == categoryId }) { "Choose a category" }
        require(
            amount.matches(Regex("[0-9]{1,10}\\.[0-9]{2}")) && BigDecimal(amount) > BigDecimal.ZERO
        ) {
            "Enter a positive amount with two decimal places"
        }
        require(currency.matches(Regex("[A-Z]{3}"))) { "Use a three-letter currency" }
        java.util.Currency.getInstance(currency)
        val start = LocalDate.parse(startDate)
        val due = LocalDate.parse(nextDueDate)
        require(!due.isBefore(start)) { "Due date must be on or after start date" }
        require(endDate == null || !LocalDate.parse(endDate).isBefore(due)) {
            "End date must be on or after due date"
        }
        require(frequency in listOf("WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY", "CUSTOM")) {
            "Choose a frequency"
        }
        require(
            frequency != "CUSTOM" ||
                (customInterval != null &&
                    customInterval in 1..3650 &&
                    customUnit in listOf("DAYS", "WEEKS", "MONTHS", "YEARS"))
        ) {
            "Custom frequency needs an interval and unit"
        }
        require(status in listOf("ACTIVE", "PAUSED", "CANCELED")) { "Choose a status" }
        require(
            description.length <= 1000 &&
                notes.length <= 2000 &&
                paymentMethod.length <= 120 &&
                websiteUrl.length <= 500
        ) {
            "Text field is too long"
        }
        require(websiteUrl.isBlank() || websiteUrl.matches(Regex("https?://[^\\s]+"))) {
            "Website must use http or https"
        }
    }

    companion object {
        fun parse(j: JSONObject) =
            Expense(
                j.getLong("id"),
                j.getString("name"),
                j.getString("kind"),
                j.getLong("categoryId"),
                j.get("amount").toString(),
                j.getString("currency"),
                j.getString("frequency"),
                j.getString("startDate"),
                j.getString("nextDueDate"),
                j.optString("status", "ACTIVE"),
                j.optBoolean("variableAmount"),
                j.optLong("version"),
                j.optString("description").nullText(),
                j.optString("paymentMethod").nullText(),
                j.optString("websiteUrl").nullText(),
                j.optString("notes").nullText(),
                j.optString("endDate").nullText().ifBlank { null },
                if (j.isNull("customInterval")) null else j.getInt("customInterval"),
                j.optString("customUnit").nullText().ifBlank { null },
            )
    }
}

private fun String.nullText() = if (this == "null") "" else this

data class Payment(
    val id: Long,
    val expenseId: Long,
    val expenseName: String,
    val occurrenceDate: String,
    val paymentDate: String,
    val amount: String,
    val currency: String,
    val notes: String = "",
) {
    fun json() =
        JSONObject()
            .put("id", id)
            .put("expenseId", expenseId)
            .put("expenseName", expenseName)
            .put("occurrenceDate", occurrenceDate)
            .put("paymentDate", paymentDate)
            .put("amount", amount)
            .put("currency", currency)
            .put("notes", notes)

    companion object {
        fun parse(j: JSONObject) =
            Payment(
                j.getLong("id"),
                j.getLong("expenseId"),
                j.getString("expenseName"),
                j.getString("occurrenceDate"),
                j.getString("paymentDate"),
                j.get("amount").toString(),
                j.getString("currency"),
                j.optString("notes").nullText(),
            )
    }
}

data class Snapshot(
    val categories: List<Category>,
    val expenses: List<Expense> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val generatedAt: String? = null,
) {
    fun json() =
        JSONObject()
            .put("schemaVersion", 1)
            .put("generatedAt", generatedAt ?: JSONObject.NULL)
            .put("categories", JSONArray(categories.map { it.json() }))
            .put("expenses", JSONArray(expenses.map { it.json() }))
            .put("payments", JSONArray(payments.map { it.json() }))

    fun validate() {
        require(categories.isNotEmpty() && categories.size <= 1000 && expenses.size <= 10000 && payments.size <= 100000) {
            "Backup exceeds supported limits"
        }
        require(categories.map { it.id }.distinct().size == categories.size) {
            "Duplicate category IDs"
        }
        require(
            categories.all {
                it.name.isNotBlank() &&
                    it.name.length <= 80 &&
                    it.color.matches(Regex("#[0-9a-fA-F]{6}"))
            }
        ) {
            "Invalid category"
        }
        require(expenses.map { it.id }.distinct().size == expenses.size) { "Duplicate expense IDs" }
        expenses.forEach { it.validate(categories) }
        require(payments.map { it.id }.distinct().size == payments.size) { "Duplicate payment IDs" }
        require(
            payments.map { it.expenseId to it.occurrenceDate }.distinct().size == payments.size
        ) {
            "Duplicate paid occurrences"
        }
        payments.forEach { payment ->
            require(expenses.any { it.id == payment.expenseId }) {
                "Payment references an unknown expense"
            }
            require(
                BigDecimal(payment.amount) > BigDecimal.ZERO &&
                    BigDecimal(payment.amount).scale() <= 2
            ) {
                "Invalid payment amount"
            }
            java.util.Currency.getInstance(payment.currency)
            LocalDate.parse(payment.occurrenceDate)
            require(!LocalDate.parse(payment.paymentDate).isAfter(LocalDate.now())) {
                "Payment date is in the future"
            }
            require(payment.notes.length <= 2000) { "Payment notes are too long" }
        }
    }

    companion object {
        fun empty() =
            Snapshot(
                listOf("Software", "Entertainment", "Utilities", "Housing", "Insurance", "Other")
                    .mapIndexed { i, n -> Category(i + 1L, n) }
            )

        fun parse(j: JSONObject): Snapshot {
            require(j.getInt("schemaVersion") == 1) { "Unsupported snapshot version" }
            fun <T> array(name: String, read: (JSONObject) -> T): List<T> {
                val a = j.getJSONArray(name)
                return (0 until a.length()).map { read(a.getJSONObject(it)) }
            }
            return Snapshot(
                array("categories", Category::parse),
                array("expenses", Expense::parse),
                array("payments", Payment::parse),
                j.optString("generatedAt").nullText().ifBlank { null },
            )
        }
    }
}

object Billing {
    fun next(e: Expense): LocalDate {
        val due = LocalDate.parse(e.nextDueDate)
        val anchor = LocalDate.parse(e.startDate)
        fun months(n: Long): LocalDate {
            val target = YearMonth.from(due).plusMonths(n)
            return target.atDay(minOf(anchor.dayOfMonth, target.lengthOfMonth()))
        }
        return when (e.frequency) {
            "WEEKLY" -> due.plusWeeks(1)
            "MONTHLY" -> months(1)
            "QUARTERLY" -> months(3)
            "YEARLY" ->
                YearMonth.of(due.year + 1, anchor.month).let {
                    it.atDay(minOf(anchor.dayOfMonth, it.lengthOfMonth()))
                }
            "CUSTOM" ->
                when (e.customUnit) {
                    "DAYS" -> due.plusDays(e.customInterval!!.toLong())
                    "WEEKS" -> due.plusWeeks(e.customInterval!!.toLong())
                    "MONTHS" -> months(e.customInterval!!.toLong())
                    "YEARS" ->
                        YearMonth.of(due.year + e.customInterval!!, anchor.month).let {
                            it.atDay(minOf(anchor.dayOfMonth, it.lengthOfMonth()))
                        }
                    else -> error("Invalid interval")
                }
            else -> error("Invalid frequency")
        }
    }

    fun monthly(e: Expense): BigDecimal {
        fun divide(n: String, d: Long) =
            BigDecimal(n).divide(BigDecimal.valueOf(d), 12, RoundingMode.HALF_UP)
        val count = (e.customInterval ?: 1).toLong()
        val factor =
            when (e.frequency) {
                "WEEKLY" -> divide("52", 12)
                "MONTHLY" -> BigDecimal.ONE
                "QUARTERLY" -> divide("1", 3)
                "YEARLY" -> divide("1", 12)
                "CUSTOM" ->
                    when (e.customUnit) {
                        "DAYS" ->
                            divide("365.2425", 12)
                                .divide(BigDecimal.valueOf(count), 12, RoundingMode.HALF_UP)
                        "WEEKS" ->
                            divide("52", 12)
                                .divide(BigDecimal.valueOf(count), 12, RoundingMode.HALF_UP)
                        "MONTHS" -> divide("1", count)
                        "YEARS" -> divide("1", 12 * count)
                        else -> error("Invalid interval")
                    }
                else -> error("Invalid frequency")
            }
        return BigDecimal(e.amount).multiply(factor)
    }
}
