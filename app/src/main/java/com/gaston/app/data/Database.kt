package com.gaston.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

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
@Dao
interface BagDao {
    @Transaction @Query("SELECT * FROM bags ORDER BY rowid") fun observe(): Flow<List<BagData>>
    @Transaction @Query("SELECT * FROM bags") suspend fun all(): List<BagData>
    @Insert suspend fun insertBag(bag: Bag)
    @Insert suspend fun insertCosts(costs: List<FixedCost>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertCycle(cycle: Cycle)
    @Insert suspend fun insertExpense(expense: Expense)
    @Query("DELETE FROM expenses WHERE id = :id") suspend fun deleteExpense(id: String)
}
@Database(entities = [Bag::class, FixedCost::class, Cycle::class, Expense::class], version = 1, exportSchema = true)
abstract class GastonDatabase : RoomDatabase() { abstract fun bags(): BagDao }
