package com.gaston.app.data

import androidx.room.withTransaction
import com.gaston.app.domain.*
import java.time.LocalDate
import java.util.UUID

class BagRepository(private val db: GastonDatabase) {
    val bags = db.bags().observe()
    suspend fun create(name: String, income: Long, day: Int, saving: Long, percent: Boolean,
                       costs: List<FixedCost>, opening: Long?, today: LocalDate): String {
        require(name.isNotBlank() && income > 0 && day in 1..31)
        require(saving >= 0 && (!percent || saving <= 10000))
        require(costs.all { it.cents > 0 && it.name.isNotBlank() })
        val reserved = Money.saving(income, saving, percent)
        val initial = income - reserved - costs.sumOf { it.cents }
        require(initial >= 0 && (opening == null || opening >= 0))
        val id = UUID.randomUUID().toString()
        val window = BudgetCalculator.window(today, day)
        db.withTransaction {
            db.bags().insertBag(Bag(id, name.trim(), income, day, saving, percent, today.toString()))
            db.bags().insertCosts(costs.map { it.copy(bagId = id) })
            db.bags().insertCycle(Cycle(id, today.toString(), window.end.toString(), opening ?: initial, reserved))
        }
        return id
    }
    suspend fun refresh(today: LocalDate) = db.withTransaction {
        for (data in db.bags().all()) {
            val bag = data.bag
            var end = data.cycles.maxOfOrNull { LocalDate.parse(it.end) } ?: continue
            val saving = Money.saving(bag.income, bag.savingValue, bag.savingPercent)
            val initial = bag.income - saving - data.costs.sumOf { it.cents }
            while (end <= today) {
                val next = BudgetCalculator.window(end, bag.payday).end
                db.bags().insertCycle(Cycle(bag.id, end.toString(), next.toString(), initial, saving))
                end = next
            }
        }
    }
    suspend fun spend(bagId: String, name: String, icon: String, cents: Long, today: LocalDate) {
        require(cents > 0 && name.isNotBlank())
        refresh(today)
        db.bags().insertExpense(Expense(UUID.randomUUID().toString(), bagId, today.toString(), name.trim(), icon, cents))
    }
    suspend fun undo(id: String) = db.bags().deleteExpense(id)
}
