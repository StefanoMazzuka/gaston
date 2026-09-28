package com.gaston.app.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class BudgetTest {
    @Test fun weekendMovesToMonday() {
        assertEquals(LocalDate.of(2026, 4, 27), BudgetCalculator.payday(YearMonth.of(2026, 4), 25))
        assertEquals(LocalDate.of(2026, 1, 26), BudgetCalculator.payday(YearMonth.of(2026, 1), 25))
    }
    @Test fun shortMonthAndLeapYear() {
        assertEquals(LocalDate.of(2024, 2, 29), BudgetCalculator.payday(YearMonth.of(2024, 2), 31))
        assertEquals(LocalDate.of(2026, 3, 2), BudgetCalculator.payday(YearMonth.of(2026, 2), 31))
    }
    @Test fun monthOverflowDoesNotResetEarly() {
        val window = BudgetCalculator.window(LocalDate.of(2026, 3, 1), 31)
        assertEquals(LocalDate.of(2026, 2, 2), window.start)
        assertEquals(LocalDate.of(2026, 3, 2), window.end)
    }
    @Test fun paydayStartsNewCycle() {
        val today = LocalDate.of(2026, 4, 27)
        assertEquals(today, BudgetCalculator.window(today, 25).start)
        assertEquals(LocalDate.of(2026, 5, 25), BudgetCalculator.window(today, 25).end)
    }
    @Test fun dailySpendingDoesNotRedistributeTodaysAllowance() {
        val start = LocalDate.of(2026, 6, 1)
        val window = Window(start, start.plusDays(30))
        val expenses = listOf(Spending(start, 1200))
        val first = BudgetCalculator.calculate(60000, window, start, expenses)
        assertEquals(800L, first.today)
        assertEquals(58800L, first.remaining)
        assertEquals(12800L, first.week)
        assertEquals(2027L, BudgetCalculator.calculate(60000, window, start.plusDays(1), expenses).today)
    }
    @Test fun oldAndFutureExpensesAreExcluded() {
        val start = LocalDate.of(2026, 6, 1)
        val result = BudgetCalculator.calculate(1000, Window(start, start.plusDays(1)), start,
            listOf(Spending(start.minusDays(1), 500), Spending(start.plusDays(1), 600)))
        assertEquals(1000L, result.remaining)
        assertEquals(0L, result.tomorrow)
    }
    @Test fun overspendingRemainsVisible() {
        val start = LocalDate.of(2026, 6, 1)
        val result = BudgetCalculator.calculate(1000, Window(start, start.plusDays(2)), start, listOf(Spending(start, 1200)))
        assertEquals(-700L, result.today)
        assertEquals(-200L, result.remaining)
    }
    @Test fun moneyAndPercentageAreExact() {
        assertEquals(12345L, Money.parse("123,45"))
        assertNull(Money.parse("1.234"))
        assertNull(Money.parse("-1"))
        assertNull(Money.parse("abc"))
        assertEquals(22500L, Money.saving(180000, 1250, true))
        assertEquals(25000L, Money.saving(180000, 25000, false))
    }
}
