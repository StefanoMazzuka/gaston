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

    fun saving(income: Long, value: Long, percent: Boolean): Long =
        if (percent) BigDecimal(income).multiply(BigDecimal(value))
            .divide(BigDecimal(10000), 0, RoundingMode.HALF_UP).longValueExact()
        else value
}

data class Window(val start: LocalDate, val end: LocalDate)
data class Spending(val date: LocalDate, val cents: Long)
data class Budget(val remaining: Long, val today: Long, val week: Long, val days: Long, val tomorrow: Long)

object BudgetCalculator {
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
        val daily = Math.floorDiv(opening, days)
        val remaining = opening - spentToday
        return Budget(remaining, daily - spentToday,
            Math.floorDiv(opening * minOf(days, 7), days) - spentToday, days,
            if (days > 1) Math.floorDiv(remaining, days - 1) else 0)
    }
}
