package com.gaston.app.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

object Money {
    fun parse(text: String): Long? = try {
        val value = text.trim().replace(',', '.').toBigDecimal()
        if (value.signum() < 0 || value > BigDecimal("1000000000")) null
        else value.movePointRight(2).longValueExact()
    } catch (_: Exception) { null }

    fun saving(income: Long, value: Long, percent: Boolean, fixedCosts: Long = 0): Long =
        if (percent) BigDecimal((income - fixedCosts).coerceAtLeast(0)).multiply(BigDecimal(value))
            .divide(BigDecimal(10000), 0, RoundingMode.HALF_UP).longValueExact()
        else value
}

data class Window(val start: LocalDate, val end: LocalDate)
data class Spending(val date: LocalDate, val cents: Long)
data class Budget(val remaining: Long, val today: Long, val week: Long, val days: Long, val tomorrow: Long)

/** The target stays fixed; actual savings depend on the cycle's spending. */
data class CycleBalance(val usefulRemaining: Long, val savingRemaining: Long,
                        val savingConsumed: Long, val saved: Long, val deficit: Long,
                        val differenceFromTarget: Long)

object SavingsCalculator {
    fun calculate(initial: Long, target: Long, spent: Long): CycleBalance {
        val useful = initial - spent
        val balance = target + useful
        return CycleBalance(
            usefulRemaining = useful.coerceAtLeast(0),
            savingRemaining = balance.coerceIn(0, target),
            savingConsumed = (-useful).coerceIn(0, target),
            saved = balance.coerceAtLeast(0),
            deficit = (-balance).coerceAtLeast(0),
            differenceFromTarget = balance - target
        )
    }
}

object BudgetCalculator {
    /** Past days show their closing allowance; future days assume the projected allowance is spent. */
    fun calendarAllowance(initial: Long, window: Window, today: LocalDate, date: LocalDate,
                          expenses: List<Spending>): Long {
        require(date >= window.start && date < window.end)
        if (date <= today) return calculate(initial, window, date, expenses).today
        val first = maxOf(window.start, today.plusDays(1))
        val spent = expenses.filter { it.date >= window.start && it.date <= today && it.date < window.end }.sumOf { it.cents }
        val remaining = (initial - spent).coerceAtLeast(0)
        val days = ChronoUnit.DAYS.between(first, window.end)
        val index = ChronoUnit.DAYS.between(first, date)
        // Keep cents exact, assigning any remainder to the final days of the cycle.
        return Math.floorDiv(remaining, days) + if (index >= days - Math.floorMod(remaining, days)) 1 else 0
    }

    fun payday(month: YearMonth, day: Int): LocalDate {
        require(day in 1..31)
        val date = month.atDay(minOf(day, month.lengthOfMonth()))
        return when (date.dayOfWeek) {
            DayOfWeek.SATURDAY -> date.plusDays(2)
            DayOfWeek.SUNDAY -> date.plusDays(1)
            else -> date
        }
    }

    fun window(today: LocalDate, day: Int): Window {
        val month = YearMonth.from(today)
        val dates = (-2L..2L).map { payday(month.plusMonths(it), day) }
        return Window(dates.last { it <= today }, dates.first { it > today })
    }

    fun calculate(initial: Long, window: Window, today: LocalDate, expenses: List<Spending>): Budget {
        require(today >= window.start && today < window.end)
        val valid = expenses.filter { it.date >= window.start && it.date <= today }
        val prior = valid.filter { it.date < today }.sumOf { it.cents }
        val spentToday = valid.filter { it.date == today }.sumOf { it.cents }
        val days = ChronoUnit.DAYS.between(today, window.end)
        val opening = initial - prior
        val daily = Math.floorDiv(opening.coerceAtLeast(0), days)
        val remaining = opening - spentToday
        val daysUntilSunday = minOf(days, (8 - today.dayOfWeek.value).toLong())
        return Budget(remaining, daily - spentToday,
            Math.floorDiv(opening.coerceAtLeast(0) * daysUntilSunday, days) - spentToday, days,
            if (days > 1) Math.floorDiv(remaining.coerceAtLeast(0), days - 1) else 0)
    }
}
