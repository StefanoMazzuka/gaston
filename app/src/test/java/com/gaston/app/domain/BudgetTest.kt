package com.gaston.app.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class BudgetTest {
    @Test fun weeklyBudgetEndsOnSundayAndSubtractsTodaysSpending() {
        val monday = LocalDate.of(2026, 6, 1)
        val window = Window(monday, monday.plusDays(14))
        for (offset in 0L..6L) {
            val date = monday.plusDays(offset)
            val budget = BudgetCalculator.calculate((14 - offset) * 1000, window, date,
                listOf(Spending(date, 200)))
            assertEquals((7 - offset) * 1000 - 200, budget.week)
        }
    }

    @Test fun weeklyBudgetStopsAtCycleEndIfBeforeSunday() {
        val wednesday = LocalDate.of(2026, 6, 3)
        val budget = BudgetCalculator.calculate(3000, Window(wednesday, wednesday.plusDays(2)),
            wednesday, listOf(Spending(wednesday, 500)))
        assertEquals(2500L, budget.week)
    }

    @Test fun calendarUpdatesTodayAndRedistributesFutureDays() {
        val today = LocalDate.of(2026, 6, 1)
        val window = Window(today, today.plusDays(4))
        val spending = listOf(Spending(today, 400))
        assertEquals(-150L, BudgetCalculator.calendarAllowance(1000, window, today, today, spending))
        for (offset in 1L..3L) {
            assertEquals(200L, BudgetCalculator.calendarAllowance(1000, window, today, today.plusDays(offset), spending))
            assertEquals(333L + if (offset == 3L) 1L else 0L,
                BudgetCalculator.calendarAllowance(1000, window, today, today.plusDays(offset), emptyList()))
        }
    }

    @Test fun calendarPastIgnoresLaterExpenses() {
        val start = LocalDate.of(2026, 6, 1)
        val window = Window(start, start.plusDays(4))
        assertEquals(150L, BudgetCalculator.calendarAllowance(1000, window, start.plusDays(2), start,
            listOf(Spending(start, 100), Spending(start.plusDays(2), 800))))
    }

    @Test fun calendarFutureCycleExcludesPreviousCycleExpensesAndPreservesCents() {
        val today = LocalDate.of(2026, 6, 1)
        val start = today.plusDays(4)
        val window = Window(start, start.plusDays(3))
        val amounts = (0L..2L).map { BudgetCalculator.calendarAllowance(1000, window, today, start.plusDays(it), listOf(Spending(today, 900))) }
        assertEquals(listOf(333L, 333L, 334L), amounts)
    }

    @Test fun calendarStopsAllocatingUsefulMoneyWhenItIsExhausted() {
        val today = LocalDate.of(2026, 6, 1)
        val window = Window(today, today.plusDays(2))
        val spending = listOf(Spending(today, 1200))
        assertEquals(0L, BudgetCalculator.calendarAllowance(1000, window, today, today.plusDays(1), spending))
        assertEquals(0L, BudgetCalculator.calendarAllowance(1000, window, today.plusDays(1), today.plusDays(1), spending))
    }

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
    @Test fun savingsAreConsumedOnlyAfterUsefulMoney() {
        val belowBudget = SavingsCalculator.calculate(60000, 20000, 55000)
        assertEquals(5000L, belowBudget.usefulRemaining)
        assertEquals(20000L, belowBudget.savingRemaining)
        assertEquals(25000L, belowBudget.saved)
        assertEquals(5000L, belowBudget.differenceFromTarget)

        val aboveBudget = SavingsCalculator.calculate(60000, 20000, 65000)
        assertEquals(0L, aboveBudget.usefulRemaining)
        assertEquals(15000L, aboveBudget.savingRemaining)
        assertEquals(5000L, aboveBudget.savingConsumed)
        assertEquals(15000L, aboveBudget.saved)
        assertEquals(-5000L, aboveBudget.differenceFromTarget)
    }

    @Test fun exhaustedSavingsShowDeficitWithoutNegativeDeposit() {
        val exhausted = SavingsCalculator.calculate(60000, 20000, 80000)
        assertEquals(0L, exhausted.saved)
        assertEquals(0L, exhausted.deficit)
        val deficit = SavingsCalculator.calculate(60000, 20000, 85000)
        assertEquals(0L, deficit.savingRemaining)
        assertEquals(20000L, deficit.savingConsumed)
        assertEquals(0L, deficit.saved)
        assertEquals(5000L, deficit.deficit)
        assertEquals(-25000L, deficit.differenceFromTarget)
    }

    @Test fun tomorrowUsesRemainingUsefulMoneyAfterEveryExpense() {
        val start = LocalDate.of(2026, 6, 1)
        val window = Window(start, start.plusDays(5))
        for ((spent, expected) in listOf(0L to 2500L, 1000L to 2250L,
            3000L to 1750L, 11000L to 0L)) {
            val expenses = listOf(Spending(start, spent))
            assertEquals(expected, BudgetCalculator.calculate(10000, window, start, expenses).tomorrow)
            assertEquals(expected, BudgetCalculator.calculate(10000, window, start.plusDays(1), expenses).today)
        }
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
