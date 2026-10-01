package com.gaston.app.domain

import java.util.Locale

data class ReceiptAmount(val cents: Long, val label: String, val isTotal: Boolean)

object ReceiptAmounts {
    private val amount = Regex("""(?<![\d.,/-])(?:\d{1,3}(?:[. ]\d{3})+|\d+)[,.]\d{2}(?![\d.,/%])""")
    private val total = Regex("""\b(total|a pagar|importe total)\b""")
    private val excluded = Regex("""\b(subtotal|sub total|iva|base|cambio|efectivo|entregado|descuento)\b""")

    fun parse(text: String): List<ReceiptAmount> = text.lineSequence().flatMap { line ->
        val normalized = line.lowercase(Locale.ROOT)
        val isTotal = total.containsMatchIn(normalized) && !excluded.containsMatchIn(normalized)
        amount.findAll(line).mapNotNull { match ->
            val raw = match.value
            val decimalIndex = raw.length - 3
            val value = raw.take(decimalIndex).replace(".", "").replace(" ", "") + "." + raw.takeLast(2)
            Money.parse(value)?.takeIf { it > 0 }?.let { ReceiptAmount(it, line.trim(), isTotal) }
        }
    }.toList().sortedByDescending { it.isTotal }.distinctBy { it.cents }
}
