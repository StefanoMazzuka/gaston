package com.gaston.app.ui

import android.app.Application
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
    val bags = repository.bags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
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
    private fun action(block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try { block() }
            catch (e: Exception) { if (e is CancellationException) throw e; error.value = "No se pudo guardar. Revisa los datos e inténtalo de nuevo." }
            finally { busy.value = false }
        }
    }
}
