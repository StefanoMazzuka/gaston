package com.gaston.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.gaston.app.domain.CycleBalance
import com.gaston.app.domain.SavingsCalculator
import java.time.LocalDate

@Entity(tableName = "bags")
data class Bag(
    @PrimaryKey val id: String,
    val name: String,
    val income: Long,
    val payday: Int,
    val savingValue: Long,
    val savingPercent: Boolean,
    val createdDate: String
)
@Entity(tableName = "fixed_costs", indices = [Index("bagId")], foreignKeys = [ForeignKey(entity = Bag::class, parentColumns = ["id"], childColumns = ["bagId"], onDelete = ForeignKey.CASCADE)])
data class FixedCost(@PrimaryKey val id: String, val bagId: String, val name: String, val icon: String, val cents: Long)
@Entity(tableName = "cycles", primaryKeys = ["bagId", "start"], foreignKeys = [ForeignKey(entity = Bag::class, parentColumns = ["id"], childColumns = ["bagId"], onDelete = ForeignKey.CASCADE)])
data class Cycle(val bagId: String, val start: String, val end: String, val initial: Long, val saving: Long)
@Entity(tableName = "expenses", indices = [Index("bagId")], foreignKeys = [ForeignKey(entity = Bag::class, parentColumns = ["id"], childColumns = ["bagId"], onDelete = ForeignKey.CASCADE)])
data class Expense(@PrimaryKey val id: String, val bagId: String, val date: String, val name: String, val icon: String, val cents: Long)

data class BagData(
    @Embedded val bag: Bag,
    @Relation(parentColumn = "id", entityColumn = "bagId") val costs: List<FixedCost>,
    @Relation(parentColumn = "id", entityColumn = "bagId") val cycles: List<Cycle>,
    @Relation(parentColumn = "id", entityColumn = "bagId") val expenses: List<Expense>
)
/** Derived from recorded expenses so edits and undo also update closed cycles. */
fun BagData.balance(cycle: Cycle, through: LocalDate): CycleBalance =
    SavingsCalculator.calculate(cycle.initial, cycle.saving, expenses.filter {
        it.date >= cycle.start && it.date < cycle.end && it.date <= through.toString()
    }.sumOf { it.cents })

fun BagData.treasury(today: LocalDate): Long = cycles
    .filter { it.end <= today.toString() }
    .sumOf { balance(it, today).saved }

@Dao
interface BagDao {
    @Transaction @Query("SELECT * FROM bags ORDER BY rowid") fun observe(): Flow<List<BagData>>
    @Transaction @Query("SELECT * FROM bags") suspend fun all(): List<BagData>
    @Insert suspend fun insertBag(bag: Bag)
    @Update suspend fun updateBag(bag: Bag)
    @Query("DELETE FROM bags WHERE id = :id") suspend fun deleteBag(id: String)
    @Query("DELETE FROM fixed_costs WHERE bagId = :bagId") suspend fun deleteCosts(bagId: String)
    @Insert suspend fun insertCosts(costs: List<FixedCost>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertCycle(cycle: Cycle)
    @Update suspend fun updateCycle(cycle: Cycle)
    @Insert suspend fun insertExpense(expense: Expense)
    @Query("UPDATE expenses SET name = :name, icon = :icon, cents = :cents WHERE id = :id")
    suspend fun editExpense(id: String, name: String, icon: String, cents: Long): Int
    @Query("DELETE FROM expenses WHERE id = :id") suspend fun deleteExpense(id: String)
}
@Database(entities = [Bag::class, FixedCost::class, Cycle::class, Expense::class], version = 1, exportSchema = true)
abstract class GastonDatabase : RoomDatabase() { abstract fun bags(): BagDao }
