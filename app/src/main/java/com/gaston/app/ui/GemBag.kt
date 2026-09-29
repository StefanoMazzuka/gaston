package com.gaston.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.ceil

@Composable
fun GemBag(fraction: Float, isNegative: Boolean = false, modifier: Modifier = Modifier) {
    val level by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(650), label = "Contenido del saco")
    Canvas(modifier.aspectRatio(1f).semantics {
        contentDescription = if (isNegative) "Presupuesto agotado" else
            "Pilas de monedas: ${(fraction.coerceIn(0f, 1f) * 100).toInt()} por ciento del presupuesto disponible"
    }) {
        val w = size.width
        val h = size.height
        val stackHeights = listOf(1, 3, 4, 3, 1)
        val coinSpots = stackHeights.flatMapIndexed { column, stackHeight ->
            (0 until stackHeight).map { row -> column to row }
        }
        val visibleCoins = ceil(coinSpots.size * level).toInt()
        drawOval(Color(0x1F263D38), Offset(w * .13f, h * .79f), Size(w * .74f, h * .11f))
        coinSpots.take(visibleCoins).forEach { (column, row) ->
            val centerX = w * (.18f + column * .16f)
            val centerY = h * (.77f - row * .12f)
            val coinSize = Size(w * .23f, h * .15f)
            val topLeft = Offset(centerX - coinSize.width / 2, centerY - coinSize.height / 2)
            drawOval(Color(0xFFB66A1E), topLeft, coinSize)
            drawOval(Color(0xFFFFC94A), topLeft + Offset(0f, -h * .018f), coinSize)
            drawOval(
                Color(0xFFFFE28A),
                topLeft + Offset(w * .035f, -h * .008f),
                Size(coinSize.width * .7f, coinSize.height * .63f),
                style = Stroke(width = w * .012f)
            )
        }
    }
}
