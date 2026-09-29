package com.gaston.app.data

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BagRepositoryTest {
    @Test fun editingExpensePreservesDateAndBagAndRecalculatesBudget() = runBlocking {
        val id = create()
        val otherId = create("Otro")
        repository.spend(id, "Compra", "", 1200, today)
        repository.spend(otherId, "Otro gasto", "", 500, today)
        val before = db.bags().all()
        val original = before.single { it.bag.id == id }.expenses.single()
        repository.editExpense(original.id, " Compra corregida ", "cafe", 2000)
        val after = db.bags().all()
        val edited = after.single { it.bag.id == id }
        assertEquals(listOf(original.copy(name = "Compra corregida", icon = "cafe", cents = 2000)), edited.expenses)
        assertEquals(before.single { it.bag.id == otherId }, after.single { it.bag.id == otherId })
        assertEquals(before.single { it.bag.id == id }.cycles, edited.cycles)
        val cycle = edited.cycles.single()
        val budget = com.gaston.app.domain.BudgetCalculator.calculate(cycle.initial,
            com.gaston.app.domain.Window(LocalDate.parse(cycle.start), LocalDate.parse(cycle.end)), today,
            edited.expenses.map { com.gaston.app.domain.Spending(LocalDate.parse(it.date), it.cents) })
        assertEquals(38000L, budget.remaining)
        assertEquals(666L, budget.today)
    }

    @Test fun invalidExpenseEditDoesNotChangeStoredExpense() = runBlocking {
        val id = create()
        repository.spend(id, "Compra", "", 1200, today)
        val before = db.bags().all()
        val expense = before.single().expenses.single()
        for ((name, amount) in listOf("" to 100L, "Compra" to 0L, "Compra" to -1L)) {
            try {
                repository.editExpense(expense.id, name, "", amount)
                fail("Invalid expense must be rejected")
            } catch (_: IllegalArgumentException) { }
        }
        assertEquals(before, db.bags().all())
        repository.undo(expense.id)
        try {
            repository.editExpense(expense.id, "Compra", "", 100)
            fail("Deleted expense must not be recreated")
        } catch (_: IllegalStateException) { }
        assertTrue(db.bags().all().single().expenses.isEmpty())
    }

    private lateinit var db: GastonDatabase
    private lateinit var repository: BagRepository
    private val today = LocalDate.of(2026, 6, 10)

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), GastonDatabase::class.java)
            .allowMainThreadQueries().build()
        repository = BagRepository(db)
    }

    @After fun close() = db.close()

    private suspend fun create(name: String = "Banco") = repository.create(
        name, 100000, 25, 10000, false,
        listOf(FixedCost("cost-$name", "", "Alquiler", "", 20000)), 40000, today)

    @Test fun editPreservesCurrentCycleAndExpensesAndAppliesSettingsOnRenewal() = runBlocking {
        val id = create()
        repository.spend(id, "Compra", "", 1200, today)
        val before = db.bags().all().single()
        repository.update(id, "Nuevo nombre", 200000, 15, 1000, true,
            listOf(FixedCost("new-cost", "", "Alquiler", "", 30000)), today = today)
        val edited = db.bags().all().single()
        assertEquals("Nuevo nombre", edited.bag.name)
        assertEquals(before.bag.createdDate, edited.bag.createdDate)
        assertEquals(before.cycles, edited.cycles)
        assertEquals(before.expenses, edited.expenses)
        assertEquals(listOf("new-cost"), edited.costs.map { it.id })
        repository.refresh(LocalDate.of(2026, 6, 25))
        val next = db.bags().all().single().cycles.single { it.start == "2026-06-25" }
        assertEquals("2026-07-15", next.end)
        assertEquals(150000L, next.initial)
        assertEquals(20000L, next.saving)
    }

    @Test fun invalidEditLeavesExistingDataIntact() = runBlocking {
        val id = create()
        val before = db.bags().all()
        try {
            repository.update(id, "Incorrecto", 100, 25, 101, false, emptyList(), today = today)
            fail("Should reject reservations larger than income")
        } catch (_: IllegalArgumentException) { }
        assertEquals(before, db.bags().all())
    }

    @Test fun deletingBagCascadesWithoutAffectingOtherBags() = runBlocking {
        val id = create()
        val otherId = create("Otro")
        repository.spend(id, "Compra", "", 1200, today)
        val other = db.bags().all().single { it.bag.id == otherId }
        repository.delete(id)
        assertEquals(listOf(other), db.bags().all())
        for (table in listOf("fixed_costs", "cycles", "expenses")) {
            db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table WHERE bagId = ?", arrayOf(id)).use {
                assertTrue(it.moveToFirst())
                assertEquals(0, it.getInt(0))
            }
        }
    }

    @Test fun overdueCyclesKeepOriginalSettingsBeforeEdit() = runBlocking {
        val id = create()
        repository.update(id, "Banco", 200000, 15, 0, false, emptyList(), null, LocalDate.of(2026, 7, 1))
        val current = db.bags().all().single().cycles.single { it.start == "2026-06-25" }
        assertEquals(70000L, current.initial)
        assertEquals("2026-07-27", current.end)
    }

    @Test fun editCanAdjustCurrentCycleOpeningBalance() = runBlocking {
        val id = create()
        repository.update(id, "Banco", 100000, 25, 10000, false,
            listOf(FixedCost("cost-Banco", "", "Alquiler", "", 20000)), 50000, today)
        val currentCycle = db.bags().all().single().cycles.single { it.start <= "2026-06-10" && it.end > "2026-06-10" }
        assertEquals(50000L, currentCycle.initial)
    }
}
