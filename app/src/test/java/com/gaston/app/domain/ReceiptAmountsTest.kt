package com.gaston.app.domain

import org.junit.Assert.*
import org.junit.Test

class ReceiptAmountsTest {
    @Test fun prioritizesTotalOverSubtotalTaxAndChange() {
        val amounts = ReceiptAmounts.parse("Subtotal 10,00\nIVA 2,10\nTOTAL 12,10\nEfectivo 20,00\nCambio 7,90")
        assertEquals(1210L, amounts.first().cents)
        assertEquals(listOf(1210L), amounts.filter { it.isTotal }.map { it.cents })
    }
    @Test fun supportsDecimalAndThousandsSeparators() {
        assertEquals(listOf(123456L, 250L), ReceiptAmounts.parse("TOTAL 1.234,56\nPan 2.50").map { it.cents })
    }
    @Test fun preservesAmbiguityAndDeduplicatesAmounts() {
        assertEquals(2, ReceiptAmounts.parse("TOTAL 5,00\nTOTAL 6,00\nTarjeta 6,00").size)
    }
    @Test fun ignoresDatesPercentagesAndZero() {
        assertTrue(ReceiptAmounts.parse("30/09/2026\nIVA 21,00%\n0,00\nSin importe").isEmpty())
    }
}
