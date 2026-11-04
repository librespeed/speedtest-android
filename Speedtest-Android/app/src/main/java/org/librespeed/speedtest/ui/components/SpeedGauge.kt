package org.librespeed.speedtest.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.librespeed.speedtest.ui.theme.LocalSpeedAccents
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val SCALE_STOPS = doubleArrayOf(0.0, 1.0, 5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0)
//the MB/s dial is not the Mbps dial divided by 8: that would label awkward stops like 31.25
private val SCALE_STOPS_MBYTES = doubleArrayOf(0.0, 0.5, 1.0, 2.5, 5.0, 10.0, 25.0, 50.0, 100.0, 125.0)
private const val START_ANGLE = 135f
private const val SWEEP = 270f

/** Maps a speed to a 0..1 position on the non-linear gauge scale. */
private fun scaleFraction(speed: Double, stops: DoubleArray): Float {
    if (speed <= 0) return 0f
    if (speed >= stops.last()) return 1f
    for (i in 1 until stops.size) {
        if (speed <= stops[i]) {
            val segment = (speed - stops[i - 1]) / (stops[i] - stops[i - 1])
            return ((i - 1) + segment).toFloat() / (stops.size - 1)
        }
    }
    return 1f
}

/** [speed] is always in Mbps; [useMBytes] converts the dial to match a MB/s readout. */
@Composable
fun SpeedGauge(
    speed: Double,
    modifier: Modifier = Modifier,
    useMBytes: Boolean = false,
    content: @Composable () -> Unit
) {
    val stops = if (useMBytes) SCALE_STOPS_MBYTES else SCALE_STOPS
    val fraction by animateFloatAsState(
        targetValue = scaleFraction(if (useMBytes) speed / 8 else speed, stops),
        animationSpec = tween(durationMillis = 300),
        label = "gauge"
    )
    val accents = LocalSpeedAccents.current
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelSizePx = with(LocalDensity.current) { 11.sp.toPx() }

    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = size.minDimension * 0.055f
            val labelInset = labelSizePx * 2.2f
            val diameter = size.minDimension - stroke - labelInset * 2
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = trackColor,
                startAngle = START_ANGLE,
                sweepAngle = SWEEP,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (fraction > 0f) {
                // sweep gradient starts at 3 o'clock; rotate so it begins at the gauge start
                rotate(degrees = START_ANGLE - 5f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0f to accents.download,
                            (SWEEP + 10f) / 360f to accents.upload,
                            1f to accents.download
                        ),
                        startAngle = 5f,
                        sweepAngle = (SWEEP * fraction).coerceAtLeast(1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
            // scale labels
            val paint = android.graphics.Paint().apply {
                color = labelColor.toArgb()
                textSize = labelSizePx
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            val labelRadius = diameter / 2 + stroke / 2 + labelSizePx * 1.1f
            val center = Offset(size.width / 2, size.height / 2)
            stops.forEachIndexed { index, stop ->
                val angleDeg = START_ANGLE + SWEEP * index / (stops.size - 1)
                val rad = Math.toRadians(angleDeg.toDouble())
                val x = center.x + labelRadius * cos(rad).toFloat()
                val y = center.y + labelRadius * sin(rad).toFloat() + labelSizePx * 0.35f
                val text = if (stop % 1.0 == 0.0) stop.toInt().toString()
                else String.format(Locale.getDefault(), "%.1f", stop)
                drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
            }
        }
        content()
    }
}
