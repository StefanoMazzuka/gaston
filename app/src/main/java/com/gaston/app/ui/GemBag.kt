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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun GemBag(fraction: Float, modifier: Modifier = Modifier) {
    val level by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(650), label = "Contenido del saco")
    val colors = listOf(Color(0xFF73DEB4), Color(0xFFA89AE8), Color(0xFFF4C775), Color(0xFF78C8E3))
    Canvas(modifier.height(210.dp).fillMaxWidth().semantics {
        contentDescription = "Saco con ${(fraction.coerceIn(0f, 1f) * 100).toInt()} por ciento del presupuesto"
    }) {
        val w = size.width
        val h = size.height
        drawOval(Color(0x16000000), Offset(w * .23f, h * .9f), Size(w * .54f, h * .08f))
        val bag = Path().apply {
            moveTo(w * .35f, h * .19f)
            cubicTo(w * .12f, h * .5f, w * .18f, h * .92f, w * .36f, h * .93f)
            lineTo(w * .64f, h * .93f)
            cubicTo(w * .82f, h * .92f, w * .88f, h * .5f, w * .65f, h * .19f)
            close()
        }
        drawPath(bag, Color(0xFFE0C499))
        clipPath(bag) {
            drawRect(Color(0xFFAF8759), Offset(0f, h * (.91f - .67f * level)), Size(w, h))
            for (row in 0..7) for (col in 0..7) {
                val x = w * (.23f + col * .078f + if (row % 2 == 0) 0f else .035f)
                val y = h * (.87f - row * .083f)
                if (y >= h * (.91f - .67f * level)) {
                    val r = h * .045f
                    val gem = Path().apply {
                        moveTo(x, y-r); lineTo(x+r, y-r*.3f); lineTo(x+r*.7f,y+r*.6f)
                        lineTo(x,y+r); lineTo(x-r*.8f,y+r*.3f); lineTo(x-r,y-r*.3f); close()
                    }
                    drawPath(gem, colors[(row * 3 + col) % colors.size])
                    drawLine(Color.White.copy(alpha=.6f), Offset(x-r*.6f,y-r*.2f), Offset(x,y-r*.65f), 2f)
                }
            }
        }
        drawOval(Color(0xFF866545), Offset(w*.32f,h*.1f), Size(w*.36f,h*.15f))
        drawOval(Color(0xFF453C35), Offset(w*.35f,h*.13f), Size(w*.30f,h*.08f))
    }
}
