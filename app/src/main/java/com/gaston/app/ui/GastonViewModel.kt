package com.gaston.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.gaston.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.time.LocalDate

class GastonViewModel(app: Application) : AndroidViewModel(app) {
    private val db = Room.databaseBuilder(app, GastonDatabase::class.java, "gaston.db").build()
    private val repository = BagRepository(db)
    val bags = repository.bags.stateIn<List<BagData>?>(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val preferences = app.getSharedPreferences("gaston_preferences", Context.MODE_PRIVATE)
    private val _favoriteBagId = MutableStateFlow(preferences.getString("favorite_bag_id", null))
    val favoriteBagId = _favoriteBagId.asStateFlow()

    fun toggleFavorite(id: String) {
        if (busy.value || bags.value?.none { it.bag.id == id } != false) return
        setFavorite(if (_favoriteBagId.value == id) null else id)
    }

    private fun setFavorite(id: String?) {
        preferences.edit().putString("favorite_bag_id", id).apply()
        _favoriteBagId.value = id
    }
    val error = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    fun refresh() { viewModelScope.launch { try { repository.refresh(LocalDate.now()) } catch (e: Exception) { if (e is CancellationException) throw e; error.value = "No se pudieron actualizar los sacos." } } }
    fun create(name: String, income: Long, day: Int, saving: Long, percent: Boolean,
               costs: List<FixedCost>, opening: Long?, done: (String) -> Unit) = action {
        done(repository.create(name, income, day, saving, percent, costs, opening, LocalDate.now()))
    }
    fun spend(id: String, name: String, icon: String, cents: Long, done: () -> Unit) = action {
        repository.spend(id, name, icon, cents, LocalDate.now()); done()
    }
    fun undo(id: String) = action { repository.undo(id) }
    fun editExpense(id: String, name: String, icon: String, cents: Long, done: () -> Unit) = action {
        repository.editExpense(id, name, icon, cents)
        done()
    }
    fun update(id: String, name: String, income: Long, day: Int, saving: Long, percent: Boolean,
               costs: List<FixedCost>, opening: Long? = null, done: () -> Unit) = action {
        repository.update(id, name, income, day, saving, percent, costs, opening, LocalDate.now())
        done()
    }
    fun delete(id: String, done: () -> Unit) = action {
        repository.delete(id)
        if (_favoriteBagId.value == id) setFavorite(null)
        done()
    }
    private fun action(block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try { block() }
            catch (e: Exception) { if (e is CancellationException) throw e; error.value = "No se pudo completar la operación. Revisa los datos e inténtalo de nuevo." }
            finally { busy.value = false }
        }
    }
}
