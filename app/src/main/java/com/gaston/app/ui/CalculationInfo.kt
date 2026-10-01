package com.gaston.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gaston.app.data.*
import com.gaston.app.domain.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.text.NumberFormat
import java.math.BigDecimal
import java.util.Locale

internal fun moneyValue(value: Long): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES")).format(BigDecimal.valueOf(value, 2))

@Composable
internal fun CalculationText(text: String, explanation: String, modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current, color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null, maxLines: Int = Int.MAX_VALUE,
    overflow: androidx.compose.ui.text.style.TextOverflow = androidx.compose.ui.text.style.TextOverflow.Clip, onSelect: () -> Unit = {}) {
    var show by remember { mutableStateOf(false) }
    Text(text, modifier.clickable(onClickLabel = "Ver cálculo") { onSelect(); show = true }, style = style, color = color, fontWeight = fontWeight, maxLines = maxLines, overflow = overflow)
    if (show) AlertDialog(onDismissRequest = { show = false }, title = { Text("Cómo se calcula") },
        text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) { Text(explanation) } },
        confirmButton = { TextButton(onClick = { show = false }) { Text("Cerrar") } })
}

internal fun cycleMath(data: BagData, cycle: Cycle?, date: LocalDate): String {
    if (cycle == null) return "Esperando al ciclo actual."
    val spent = data.expenses.filter { it.date >= cycle.start && it.date < cycle.end && it.date <= date.toString() }.sumOf { it.cents }
    val result = data.balance(cycle, date)
    val opening = if (cycle.accountOpening != null && cycle.reservedCosts != null)
        "Saldo inicial ${moneyValue(cycle.accountOpening)} − pagos reservados ${moneyValue(cycle.reservedCosts)} − ahorro deseado ${moneyValue(cycle.saving)} = disponible inicial ${moneyValue(cycle.initial)}."
    else "Disponible inicial guardado: ${moneyValue(cycle.initial)}. El saldo de cuenta y los pagos reservados no se guardaron en este ciclo antiguo."
    return "$opening\n\nDisponible: ${moneyValue(cycle.initial)} − gastos ${moneyValue(spent)} = ${moneyValue(cycle.initial - spent)}.\n\n" +
        "Ahorro consumido: ${moneyValue(result.savingConsumed)}.\nAhorro restante: ${moneyValue(cycle.saving)} − ${moneyValue(result.savingConsumed)} = ${moneyValue(result.savingRemaining)}.\n\n" +
        "Aportación al cofre: ahorro restante ${moneyValue(result.savingRemaining)} + disponible positivo ${moneyValue(result.usefulRemaining)} = ${moneyValue(result.saved)}. Solo se suma al cerrar el ciclo.\n" +
        "Diferencia respecto al objetivo: ${moneyValue(cycle.initial - spent)}.\nDéficit sin cubrir: ${moneyValue(result.deficit)}. Los importes disponibles y los depósitos se limitan a cero si el saldo es negativo."
}

internal fun dailyMath(data: BagData, cycle: Cycle?, date: LocalDate): String {
    if (cycle == null) return "Esperando al ciclo actual."
    val prior = data.expenses.filter { it.date >= cycle.start && it.date < date.toString() }.sumOf { it.cents }
    val spent = data.expenses.filter { it.date == date.toString() }.sumOf { it.cents }
    val days = ChronoUnit.DAYS.between(date, LocalDate.parse(cycle.end))
    if (days <= 0) return "Este día comienza el siguiente ciclo."
    val opening = (cycle.initial - prior).coerceAtLeast(0)
    val daily = Math.floorDiv(opening, days)
    return "Disponible al comenzar el día: ${moneyValue(cycle.initial)} − gastos anteriores ${moneyValue(prior)} = ${moneyValue(opening)} (mínimo cero).\n\n${moneyValue(opening)} ÷ $days días = ${moneyValue(daily)} para hoy.\n\n${moneyValue(daily)} − gastos de hoy ${moneyValue(spent)} = ${moneyValue(daily - spent)}.\n\nSe incluye hoy y se excluye el próximo cobro. Se redondea hacia abajo al céntimo; el resto queda para los siguientes días."
}

internal fun treasuryMath(bags: List<BagData>, today: LocalDate): String {
    val entries = bags.flatMap { data -> data.cycles.filter { it.end <= today.toString() }.map {
        "${data.bag.name} · ${it.start} → ${it.end}: ${moneyValue(data.balance(it, today).saved)}"
    } }
    return "Suma de aportaciones de ciclos cerrados:\n\n" + (entries.joinToString("\n").ifEmpty { "Todavía no hay ciclos cerrados." }) +
        "\n\nTotal: ${moneyValue(bags.sumOf { it.treasury(today) })}. El ciclo en curso no está incluido."
}

internal fun calendarMath(data: BagData, cycle: Cycle, today: LocalDate, date: LocalDate): String {
    val end = LocalDate.parse(cycle.end)
    if (date == end) return "El ${cycle.end} comienza el siguiente ciclo."
    if (date <= today) return dailyMath(data, cycle, date)
    val spent = data.expenses.filter { it.date >= cycle.start && it.date <= today.toString() && it.date < cycle.end }.sumOf { it.cents }
    val remaining = (cycle.initial - spent).coerceAtLeast(0)
    val days = ChronoUnit.DAYS.between(maxOf(LocalDate.parse(cycle.start), today.plusDays(1)), end)
    val result = BudgetCalculator.calendarAllowance(cycle.initial, Window(LocalDate.parse(cycle.start), end), today, date,
        data.expenses.map { Spending(LocalDate.parse(it.date), it.cents) })
    return "${moneyValue(cycle.initial)} − gastos hasta hoy ${moneyValue(spent)} = ${moneyValue(remaining)} (mínimo cero).\n\n${moneyValue(remaining)} ÷ $days días futuros.\n\nPara el $date: ${moneyValue(result)}. Los céntimos sobrantes se asignan a los últimos días. Esta previsión supone que hoy no gastas más."
}
