package com.gaston.app.data

import androidx.room.withTransaction
import com.gaston.app.domain.*
import java.time.LocalDate
import java.util.UUID

class BagRepository(private val db: GastonDatabase) {
    val bags = db.bags().observe()
    suspend fun update(id: String, name: String, income: Long, day: Int, saving: Long,
                       percent: Boolean, costs: List<FixedCost>, opening: Long? = null, today: LocalDate, details: OpeningInput? = null) = db.withTransaction {
        require(name.isNotBlank() && income > 0 && day in 1..31)
        require(saving >= 0 && (!percent || saving <= 10000))
        require(costs.all { it.cents > 0 && it.name.isNotBlank() })
        require(income - Money.saving(income, saving, percent) - costs.sumOf { it.cents } >= 0)
        require(opening == null || opening >= 0)
        // Close any elapsed cycles using the original settings before editing.
        refresh(today)
        val bagData = db.bags().all().first { it.bag.id == id }
        db.bags().updateBag(bagData.bag.copy(name = name.trim(), income = income, payday = day,
            savingValue = saving, savingPercent = percent))
        db.bags().deleteCosts(id)
        db.bags().insertCosts(costs.map { it.copy(bagId = id) })
        if (opening != null) {
            val activeCycle = bagData.cycles.find { today.toString() >= it.start && today.toString() < it.end }
            if (activeCycle != null) {
                db.bags().updateCycle(activeCycle.copy(initial = opening, accountOpening = details?.account, reservedCosts = details?.costs))
            }
        }
    }
    suspend fun delete(id: String) = db.bags().deleteBag(id)
    suspend fun create(name: String, income: Long, day: Int, saving: Long, percent: Boolean,
                       costs: List<FixedCost>, opening: Long?, today: LocalDate, details: OpeningInput? = null): String {
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
            db.bags().insertCycle(Cycle(id, today.toString(), window.end.toString(), opening ?: initial, reserved, details?.account ?: if (opening == null) income else null, details?.costs ?: if (opening == null) costs.sumOf { it.cents } else null))
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
                db.bags().insertCycle(Cycle(bag.id, end.toString(), next.toString(), initial, saving, bag.income, data.costs.sumOf { it.cents }))
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
    suspend fun editExpense(id: String, name: String, icon: String, cents: Long) {
        require(name.isNotBlank() && cents > 0)
        check(db.bags().editExpense(id, name.trim(), icon, cents) == 1)
    }
}
