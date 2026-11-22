package com.brennensubs.billvault.android

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class AndroidRulesTest {
    private fun expense() =
        Expense(
            1,
            "Fictional stream",
            "SUBSCRIPTION",
            1,
            "14.99",
            "USD",
            "MONTHLY",
            "2026-01-31",
            "2026-01-31",
        )

    @Test
    fun monthEndReturnsToOriginalAnchor() {
        val feb = Billing.next(expense())
        assertEquals(LocalDate.of(2026, 2, 28), feb)
        assertEquals(
            LocalDate.of(2026, 3, 31),
            Billing.next(expense().copy(nextDueDate = feb.toString())),
        )
    }

    @Test
    fun leapYearRestoresLeapDay() {
        val e =
            expense()
                .copy(frequency = "YEARLY", startDate = "2024-02-29", nextDueDate = "2027-02-28")
        assertEquals(LocalDate.of(2028, 2, 29), Billing.next(e))
    }

    @Test
    fun customDaysUsesSameNormalizedFactorAsServer() {
        val e = expense().copy(frequency = "CUSTOM", customInterval = 10, customUnit = "DAYS")
        assertEquals(0, BigDecimal("45.624875625").compareTo(Billing.monthly(e)))
    }

    @Test
    fun inputRejectsFloatingPrecisionAndUnknownCurrency() {
        val categories = listOf(Category(1, "Other"))
        expense().validate(categories)
        assertThrows(IllegalArgumentException::class.java) {
            expense().copy(amount = "14.999").validate(categories)
        }
        assertThrows(IllegalArgumentException::class.java) {
            expense().copy(currency = "ZZZ").validate(categories)
        }
    }

    @Test
    fun transportRejectsUnencryptedRemoteAndEmbeddedCredentials() {
        assertThrows(IllegalArgumentException::class.java) {
            RemoteVault.validatedUrl("http://192.168.1.2:8766", VaultMode.DESKTOP, true)
        }
        assertThrows(IllegalArgumentException::class.java) {
            RemoteVault.validatedUrl(
                "https://user:secret@example.com",
                VaultMode.SELF_HOSTED,
                false,
            )
        }
        assertEquals(
            "http://10.0.2.2:8080",
            RemoteVault.validatedUrl("http://10.0.2.2:8080", VaultMode.SELF_HOSTED, true),
        )
        assertThrows(IllegalArgumentException::class.java) {
            RemoteVault.validatedUrl("http://10.0.2.2:8080", VaultMode.SELF_HOSTED, false)
        }
    }

    @Test
    fun csvEscapesFormulasAndQuotes() {
        val payment =
            Payment(
                1,
                1,
                "=Fictional",
                "2026-01-01",
                "2026-01-01",
                "14.99",
                "USD",
                "A, \"quoted\" note",
            )
        val csv = paymentCsv(listOf(payment))
        assertTrue(csv.contains("'=Fictional"))
        assertTrue(csv.contains("\"\"quoted\"\""))
    }

    @Test
    fun restoreRejectsOrphanAndDuplicatePayments() {
        val e = expense()
        val payment = Payment(1, 1, e.name, e.nextDueDate, "2026-01-31", "14.99", "USD")
        val categories = listOf(Category(1, "Other"))
        assertThrows(IllegalArgumentException::class.java) {
            Snapshot(categories, listOf(e), listOf(payment, payment.copy(id = 2))).validate()
        }
        assertThrows(IllegalArgumentException::class.java) {
            Snapshot(categories, listOf(e), listOf(payment.copy(expenseId = 99))).validate()
        }
    }

    @Test
    fun snapshotRoundTripPreservesFields() {
        val original =
            Snapshot(
                listOf(Category(1, "Other")),
                listOf(expense().copy(notes = "Fictional note", customInterval = null)),
                emptyList(),
            )
        val parsed = Snapshot.parse(original.json())
        assertEquals(original, parsed)
    }
}
