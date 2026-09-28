package com.gaston.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.gaston.app.data.*
import com.gaston.app.domain.*
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val Spanish = Locale.forLanguageTag("es-ES")
private fun euros(cents: Long) = NumberFormat.getCurrencyInstance(Spanish).format(java.math.BigDecimal.valueOf(cents, 2))
private val dateFormat = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Spanish)
private val icons = listOf("🏠", "💡", "📺", "❤️", "🛒", "🚌", "☕", "🎁")

@Composable
fun GastonApp(vm: GastonViewModel) {
    val nav = rememberNavController()
    val bags by vm.bags.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var today by remember { mutableStateOf(LocalDate.now()) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { today = LocalDate.now(); vm.refresh() }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        vm.refresh()
        while (true) { delay(30_000); val now = LocalDate.now(); if (now != today) { today = now; vm.refresh() } }
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF245E4B), secondary = Color(0xFF78629F), background = Color(0xFFF8F6F0), surface = Color(0xFFFFFDF7))) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            NavHost(nav, "bags", Modifier.safeDrawingPadding()) {
                composable("bags") {
                    Page("Tus sacos", "Un lugar para cada banco") {
                        if (bags.isEmpty()) { GemBag(1f); Text("Tu dinero, un día a la vez.", style = MaterialTheme.typography.headlineSmall); Text("Crea tu primer saco para conocer tu margen diario.") }
                        bags.forEach { data ->
                            Card(onClick = { nav.navigate("bag/${data.bag.id}") }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("💎  ${data.bag.name}", style = MaterialTheme.typography.titleLarge)
                                    currentBudget(data, today)?.let { Text("Hoy: ${euros(it.today)} · Total: ${euros(it.remaining)}") }
                                    Text("Abrir saco →", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        Button(onClick = { nav.navigate("create") }, modifier = Modifier.fillMaxWidth()) { Text("+ Crear saco") }
                    }
                }
                composable("create") { CreateBag(busy, { nav.popBackStack() }) { name, income, day, saving, percent, costs, opening ->
                    vm.create(name, income, day, saving, percent, costs, opening) { id -> nav.popBackStack(); nav.navigate("bag/$id") }
                } }
                composable("bag/{id}") { entry ->
                    val data = bags.find { it.bag.id == entry.arguments?.getString("id") }
                    if (data != null) BagScreen(data, today, busy, { nav.popBackStack() }, { nav.navigate("calendar/${data.bag.id}") }, vm)
                    else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                composable("calendar/{id}") { entry ->
                    val data = bags.find { it.bag.id == entry.arguments?.getString("id") }
                    if (data != null) CalendarScreen(data, today, { nav.popBackStack() })
                }
            }
            if (error != null) AlertDialog(onDismissRequest = { vm.error.value = null }, title = { Text("No se ha guardado") }, text = { Text(error!!) }, confirmButton = { TextButton(onClick = { vm.error.value = null }) { Text("Entendido") } })
        }
    }
}

@Composable
private fun Page(title: String, subtitle: String, back: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (back != null) TextButton(onClick = back) { Text("← Volver") }
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
        Spacer(Modifier.height(16.dp))
    }
}
private fun currentBudget(data: BagData, today: LocalDate): Budget? {
    val cycle = data.cycles.find { today.toString() >= it.start && today.toString() < it.end } ?: return null
    return BudgetCalculator.calculate(cycle.initial, Window(LocalDate.parse(cycle.start), LocalDate.parse(cycle.end)), today,
        data.expenses.map { Spending(LocalDate.parse(it.date), it.cents) })
}

@Composable
private fun BagScreen(data: BagData, today: LocalDate, busy: Boolean, back: () -> Unit, calendar: () -> Unit, vm: GastonViewModel) {
    var spending by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Expense?>(null) }
    val budget = currentBudget(data, today)
    val cycle = data.cycles.find { today.toString() >= it.start && today.toString() < it.end }
    Page(data.bag.name, today.format(dateFormat), back) {
        Text("HOY TE QUEDAN", style = MaterialTheme.typography.labelLarge)
        Text(budget?.let { euros(it.today) } ?: "Actualizando…", style = MaterialTheme.typography.displayMedium,
            color = if ((budget?.today ?: 0) < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        GemBag(if (cycle != null && cycle.initial > 0) (budget?.remaining ?: 0).toFloat() / cycle.initial else 0f)
        Text("En tu saco: ${euros(budget?.remaining ?: 0)}", style = MaterialTheme.typography.titleLarge)
        Text("Próximos ${minOf(budget?.days ?: 0, 7)} días: ${euros(budget?.week ?: 0)}")
        Text("Próximo cobro: ${cycle?.end ?: "…"} · Quedan ${budget?.days ?: 0} días")
        if ((budget?.today ?: 0) < 0) Text("Has superado el margen de hoy. El presupuesto de los próximos días se ajustará.")
        Button(onClick = { spending = true }, enabled = !busy && cycle != null, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("− Sacar") }
        OutlinedButton(onClick = calendar, modifier = Modifier.fillMaxWidth()) { Text("Calendario e historial") }
        Text("Dinero reservado", style = MaterialTheme.typography.titleLarge)
        Text("Ahorro del ciclo: ${euros(cycle?.saving ?: 0)}")
        data.costs.forEach { Text("${it.icon} ${it.name} · ${euros(it.cents)}") }
        Text("Estos pagos ya están descontados del presupuesto libre.", style = MaterialTheme.typography.bodySmall)
        Text("Últimos gastos", style = MaterialTheme.typography.titleLarge)
        if (data.expenses.isEmpty()) Text("Tu saco todavía no tiene gastos.")
        data.expenses.sortedByDescending { it.date }.take(10).forEach { expense ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("${expense.icon} ${expense.name} · ${euros(expense.cents)}"); Text(expense.date, style = MaterialTheme.typography.bodySmall) }
                TextButton(onClick = { deleting = expense }, enabled = !busy) { Text("Deshacer") }
            }
        }
    }
    if (spending) ExpenseDialog(busy, { spending = false }) { name, icon, amount -> vm.spend(data.bag.id, name, icon, amount) { spending = false } }
    deleting?.let { expense -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("¿Deshacer este gasto?") }, text = { Text("${expense.name}: ${euros(expense.cents)} volverán al saldo del ciclo correspondiente.") }, confirmButton = { TextButton(onClick = { vm.undo(expense.id); deleting = null }) { Text("Deshacer") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancelar") } }) }
}

@Composable
private fun AmountField(value: String, change: (String) -> Unit, label: String) {
    OutlinedTextField(value, change, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
}
@Composable
private fun IconPicker(selected: String, change: (String) -> Unit) {
    Column { icons.chunked(4).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { icon -> FilterChip(selected == icon, { change(icon) }, label = { Text(icon) }) } } } }
}
@Composable
private fun ExpenseDialog(busy: Boolean, dismiss: () -> Unit, save: (String, String, Long) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var icon by rememberSaveable { mutableStateOf("🛒") }
    val cents = Money.parse(amount)
    AlertDialog(onDismissRequest = { if (!busy) dismiss() }, title = { Text("Sacar gemas") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AmountField(amount, { amount = it }, "Importe (€)")
            OutlinedTextField(name, { name = it }, label = { Text("¿En qué lo has gastado?") }, singleLine = true)
            IconPicker(icon) { icon = it }
        }
    }, confirmButton = { TextButton(enabled = !busy && cents != null && cents > 0 && name.isNotBlank(), onClick = { save(name, icon, cents!!) }) { Text(if (busy) "Guardando…" else "Guardar gasto") } }, dismissButton = { TextButton(enabled = !busy, onClick = dismiss) { Text("Cancelar") } })
}

@Composable
private fun CreateBag(busy: Boolean, back: () -> Unit, save: (String, Long, Int, Long, Boolean, List<FixedCost>, Long?) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var income by rememberSaveable { mutableStateOf("") }
    var day by rememberSaveable { mutableStateOf("25") }
    var saving by rememberSaveable { mutableStateOf("0") }
    var percent by rememberSaveable { mutableStateOf(false) }
    var opening by rememberSaveable { mutableStateOf("") }
    var costName by rememberSaveable { mutableStateOf("") }
    var costAmount by rememberSaveable { mutableStateOf("") }
    var costIcon by rememberSaveable { mutableStateOf("🏠") }
    val costs = rememberSaveable(saver = listSaver(
        save = { list -> list.flatMap { listOf(it.id, it.name, it.icon, it.cents.toString()) } },
        restore = { values -> mutableStateListOf<FixedCost>().apply {
            values.chunked(4).forEach { add(FixedCost(it[0], "", it[1], it[2], it[3].toLong())) }
        } }
    )) { mutableStateListOf<FixedCost>() }
    val inc = Money.parse(income)
    val sav = Money.parse(saving)
    val payday = day.toIntOrNull()
    val available = if (inc != null && sav != null && (!percent || sav <= 10000)) inc - Money.saving(inc, sav, percent) - costs.sumOf { it.cents } else null
    val valid = name.isNotBlank() && inc != null && inc > 0 && payday != null && payday in 1..31 && available != null && available >= 0 && (opening.isBlank() || Money.parse(opening) != null) && costName.isBlank() && costAmount.isBlank()
    Page("Nuevo saco", "Configura el dinero de este banco", back) {
        OutlinedTextField(name, { name = it }, label = { Text("Nombre del banco") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        AmountField(income, { income = it }, "Ingreso mensual (€)")
        OutlinedTextField(day, { day = it }, label = { Text("Día de cobro (1–31)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Si cae en fin de semana, cobrarás el lunes siguiente.", style = MaterialTheme.typography.bodySmall)
        Text("Salidas fijas", style = MaterialTheme.typography.titleLarge)
        costs.forEach { cost -> Row(verticalAlignment = Alignment.CenterVertically) { Text("${cost.icon} ${cost.name} · ${euros(cost.cents)}", Modifier.weight(1f)); TextButton(onClick = { costs.remove(cost) }) { Text("Quitar") } } }
        OutlinedTextField(costName, { costName = it }, label = { Text("Concepto: alquiler, luz…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        AmountField(costAmount, { costAmount = it }, "Importe de la salida (€)")
        IconPicker(costIcon) { costIcon = it }
        OutlinedButton(onClick = { costs.add(FixedCost(UUID.randomUUID().toString(), "", costName.trim(), costIcon, Money.parse(costAmount)!!)); costName = ""; costAmount = "" }, enabled = costName.isNotBlank() && (Money.parse(costAmount) ?: 0) > 0) { Text("+ Añadir salida") }
        Text("Ahorro", style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically) { Switch(percent, { percent = it }); Text(if (percent) "Porcentaje del ingreso" else "Cantidad fija", Modifier.padding(start = 12.dp)) }
        AmountField(saving, { saving = it }, if (percent) "Ahorro (%)" else "Ahorro (€)")
        Text("Disponible mensual: ${available?.let { euros(it) } ?: "—"}")
        AmountField(opening, { opening = it }, "Disponible real hoy (€), opcional")
        Text("Si empiezas a mitad de mes, indica lo que te queda libre después de reservar pagos y ahorro. Vacío usa el disponible mensual completo para este primer ciclo.", style = MaterialTheme.typography.bodySmall)
        if (available != null && available < 0) Text("Las salidas y el ahorro superan el ingreso.", color = MaterialTheme.colorScheme.error)
        if (costName.isNotBlank() || costAmount.isNotBlank()) Text("Añade la salida pendiente o vacía sus campos antes de crear el saco.")
        Button(onClick = { save(name, inc!!, payday!!, sav!!, percent, costs.toList(), if (opening.isBlank()) null else Money.parse(opening)) }, enabled = valid && !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Guardando…" else "Crear mi saco") }
    }
}

@Composable
private fun CalendarScreen(data: BagData, today: LocalDate, back: () -> Unit) {
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var selectedText by rememberSaveable { mutableStateOf(today.toString()) }
    val month = YearMonth.parse(monthText)
    val selected = LocalDate.parse(selectedText)
    val nextPay = BudgetCalculator.window(today, data.bag.payday).end
    Page("Calendario", data.bag.name, back) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { monthText = month.minusMonths(1).toString() }) { Text("←") }
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Spanish)))
            TextButton(onClick = { monthText = month.plusMonths(1).toString() }) { Text("→") }
        }
        Row { listOf("L", "M", "X", "J", "V", "S", "D").forEach { Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(it) } } }
        val offset = month.atDay(1).dayOfWeek.value - 1
        val count = ((offset + month.lengthOfMonth() + 6) / 7) * 7
        (0 until count).toList().chunked(7).forEach { week ->
            Row { week.forEach { index ->
                val number = index - offset + 1
                val date = if (number in 1..month.lengthOfMonth()) month.atDay(number) else null
                Column(Modifier.weight(1f).height(64.dp).background(if (date == selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(12.dp)).then(if (date != null) Modifier.clickable { selectedText = date.toString() } else Modifier), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    if (date != null) { Text(number.toString(), fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal); if (date == nextPay) Text("💰") else if (data.expenses.any { it.date == date.toString() }) Text("•", color = MaterialTheme.colorScheme.primary) }
                }
            } }
        }
        Text("💰 Próximo cobro: $nextPay")
        Text(selected.format(dateFormat), style = MaterialTheme.typography.titleMedium)
        val expenses = data.expenses.filter { it.date == selected.toString() }
        Text("Gastado: ${euros(expenses.sumOf { it.cents })}")
        expenses.forEach { Text("${it.icon} ${it.name} · ${euros(it.cents)}") }
        if (expenses.isEmpty()) Text("Sin gastos registrados este día.")
        Text("Ciclos anteriores", style = MaterialTheme.typography.titleLarge)
        data.cycles.filter { it.end <= today.toString() }.sortedByDescending { it.start }.forEach { cycle ->
            val spent = data.expenses.filter { it.date >= cycle.start && it.date < cycle.end }.sumOf { it.cents }
            Text("${cycle.start} → ${cycle.end}\nSaldo al cierre: ${euros(cycle.initial - spent)}")
        }
    }
}
