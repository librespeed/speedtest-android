package org.librespeed.speedtest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun Sparkline(
    data: List<Double>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        val max = data.max().takeIf { it > 0 } ?: return@Canvas
        val stepX = size.width / (data.size - 1)
        val vPadding = size.height * 0.1f
        val usable = size.height - vPadding * 2
        fun pointAt(index: Int): Offset {
            val y = size.height - vPadding - (data[index] / max * usable).toFloat()
            return Offset(stepX * index, y)
        }

        val line = Path()
        line.moveTo(pointAt(0).x, pointAt(0).y)
        for (i in 1 until data.size) {
            val previous = pointAt(i - 1)
            val current = pointAt(i)
            val midX = (previous.x + current.x) / 2
            line.cubicTo(midX, previous.y, midX, current.y, current.x, current.y)
        }
        drawPath(line, color = color, style = Stroke(width = 4f, cap = StrokeCap.Round))

        val fill = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            fill,
            brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.25f), Color.Transparent))
        )
    }
}
