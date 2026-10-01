package com.gaston.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TreasuryTest {
    private val first = Cycle("bag", "2026-06-01", "2026-07-01", 60000, 20000)
    private val second = Cycle("bag", "2026-07-01", "2026-08-01", 60000, 20000)
    private fun data(expenses: List<Expense>) = BagData(
        Bag("bag", "Banco", 80000, 1, 20000, false, "2026-06-01"),
        emptyList(), listOf(first, second), expenses
    )

    @Test fun chestStartsEmptyAndAccumulatesOnlyClosedCycles() {
        val data = data(listOf(
            Expense("one", "bag", "2026-06-30", "Compra", "", 55000),
            Expense("two", "bag", "2026-07-01", "Compra", "", 65000)
        ))
        assertEquals(0L, data.treasury(LocalDate.parse("2026-06-30")))
        assertEquals(25000L, data.treasury(LocalDate.parse("2026-07-01")))
        assertEquals(40000L, data.treasury(LocalDate.parse("2026-08-01")))
    }

    @Test fun editingOrRemovingOldExpensesUpdatesTheClosingBalance() {
        val expense = Expense("one", "bag", "2026-06-30", "Compra", "", 65000)
        val today = LocalDate.parse("2026-07-01")
        assertEquals(15000L, data(listOf(expense)).treasury(today))
        assertEquals(25000L, data(listOf(expense.copy(cents = 55000))).treasury(today))
        assertEquals(80000L, data(emptyList()).treasury(today))
    }
}
