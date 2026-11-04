package org.librespeed.speedtest.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import org.librespeed.speedtest.R
import org.librespeed.speedtest.data.GeoDistance
import org.librespeed.speedtest.data.HistoryEntry
import org.librespeed.speedtest.ui.history.formatDate
import java.io.File
import java.util.Locale

object ShareImage {

    private const val TEAL = 0xFF2DD4BF.toInt()
    private const val PURPLE = 0xFFA78BFA.toInt()
    private const val WHITE = 0xFFE4E9F2.toInt()
    private const val GRAY = 0xFF9AA4B8.toInt()
    private const val BACKGROUND_TOP = 0xFF0B1120.toInt()
    private const val BACKGROUND_BOTTOM = 0xFF1A1440.toInt()

    private const val WIDTH = 1080
    private const val HEIGHT = 1350

    fun share(context: Context, entry: HistoryEntry, useMBytes: Boolean) {
        try {
            val bitmap = render(context, entry, useMBytes)
            val directory = File(context.cacheDir, "share").apply { mkdirs() }
            val file = File(directory, "librespeed-result.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_result)))
        } catch (_: Exception) {
        }
    }

    fun render(context: Context, entry: HistoryEntry, useMBytes: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawBackground(canvas)

        val unit = context.getString(if (useMBytes) R.string.unit_mbytes else R.string.unit_mbps)
        fun speed(value: Double) =
            if (value < 0) "—" else String.format(Locale.getDefault(), "%.2f", if (useMBytes) value / 8 else value)
        fun ms(value: Double) =
            if (value < 0) "—" else String.format(Locale.getDefault(), "%.0f %s", value, context.getString(R.string.unit_ms))

        fun paint(color: Int, size: Float, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }

        val centerX = WIDTH / 2f

        //wordmark
        val titlePaint = paint(TEAL, 92f, bold = true).apply { textAlign = Paint.Align.LEFT }
        val libreWidth = titlePaint.measureText("Libre")
        val speedWidth = titlePaint.measureText("Speed")
        val titleStart = centerX - (libreWidth + speedWidth) / 2
        canvas.drawText("Libre", titleStart, 170f, titlePaint)
        titlePaint.color = WHITE
        canvas.drawText("Speed", titleStart + libreWidth, 170f, titlePaint)
        canvas.drawText(context.getString(R.string.share_image_subtitle), centerX, 235f, paint(GRAY, 42f))

        //download
        drawCircleArrow(canvas, centerX - measureSpeedHalf(speed(entry.download)) - 70f, 425f, TEAL, down = true)
        canvas.drawText(speed(entry.download), centerX, 460f, paint(TEAL, 140f, bold = true))
        canvas.drawText(unit, centerX + measureSpeedHalf(speed(entry.download)) + 85f, 455f, paint(GRAY, 46f))
        canvas.drawText(context.getString(R.string.test_download).uppercase(Locale.ROOT), centerX, 535f, paint(GRAY, 44f, bold = true))

        //upload
        drawCircleArrow(canvas, centerX - measureSpeedHalf(speed(entry.upload)) - 70f, 690f, PURPLE, down = false)
        canvas.drawText(speed(entry.upload), centerX, 725f, paint(PURPLE, 140f, bold = true))
        canvas.drawText(unit, centerX + measureSpeedHalf(speed(entry.upload)) + 85f, 720f, paint(GRAY, 46f))
        canvas.drawText(context.getString(R.string.test_upload).uppercase(Locale.ROOT), centerX, 800f, paint(GRAY, 44f, bold = true))

        //ping + jitter
        val leftX = WIDTH / 3f
        val rightX = 2 * WIDTH / 3f
        canvas.drawText(ms(entry.ping), leftX, 950f, paint(WHITE, 72f, bold = true))
        canvas.drawText(context.getString(R.string.test_ping).uppercase(Locale.ROOT), leftX, 1005f, paint(GRAY, 40f))
        canvas.drawText(ms(entry.jitter), rightX, 950f, paint(WHITE, 72f, bold = true))
        canvas.drawText(context.getString(R.string.test_jitter).uppercase(Locale.ROOT), rightX, 1005f, paint(GRAY, 40f))

        //server + date, above the waves
        canvas.drawText(
            listOfNotNull(GeoDistance.cleanName(entry.server), GeoDistance.sponsor(entry.server)).joinToString(" · "),
            centerX, 1075f, paint(WHITE, 44f)
        )
        canvas.drawText(formatDate(entry.date), centerX, 1133f, paint(GRAY, 40f))

        //rounded border framing the whole card, like the mockup
        canvas.drawRoundRect(
            android.graphics.RectF(5f, 5f, WIDTH - 5f, HEIGHT - 5f), 48f, 48f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x30FFFFFF
                style = Paint.Style.STROKE
                strokeWidth = 5f
            }
        )

        return bitmap
    }

    private fun measureSpeedHalf(text: String): Float =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 140f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }.measureText(text) / 2

    private fun drawBackground(canvas: Canvas) {
        val width = WIDTH.toFloat()
        val height = HEIGHT.toFloat()
        canvas.drawRect(0f, 0f, width, height, Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, height, BACKGROUND_TOP, BACKGROUND_BOTTOM, Shader.TileMode.CLAMP)
        })
        //soft teal glow behind the wordmark and purple glow behind the upload value
        canvas.drawRect(0f, 0f, width, 500f, Paint().apply {
            shader = RadialGradient(width / 2f, 60f, 620f, 0x2E2DD4BF, 0x002DD4BF, Shader.TileMode.CLAMP)
        })
        canvas.drawRect(0f, 350f, width, height, Paint().apply {
            shader = RadialGradient(width / 2f, 700f, 760f, 0x1E7C5CFC, 0x007C5CFC, Shader.TileMode.CLAMP)
        })
        //subtle gradient waves along the very bottom, below the server line
        wave(canvas, height - 210f, 70f, 0x2E2DD4BF, 0x2EA78BFA, phase = 0f)
        wave(canvas, height - 145f, 60f, 0x38A78BFA, 0x382DD4BF, phase = 0.5f)
        wave(canvas, height - 80f, 55f, 0x4D2DD4BF, 0x4D7C5CFC, phase = 0.25f)
    }

    private fun wave(canvas: Canvas, top: Float, amplitude: Float, colorStart: Int, colorEnd: Int, phase: Float) {
        val width = WIDTH.toFloat()
        val height = HEIGHT.toFloat()
        val path = Path().apply {
            moveTo(0f, top + amplitude * phase)
            cubicTo(
                width * 0.25f, top - amplitude,
                width * 0.45f, top + amplitude * 1.4f,
                width * 0.7f, top + amplitude * 0.2f
            )
            cubicTo(
                width * 0.85f, top - amplitude * 0.5f,
                width * 0.95f, top + amplitude * 0.6f,
                width, top - amplitude * 0.2f
            )
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, top - amplitude, width, height, colorStart, colorEnd, Shader.TileMode.CLAMP)
        })
    }

    private fun drawCircleArrow(canvas: Canvas, cx: Float, cy: Float, color: Int, down: Boolean) {
        val radius = 36f
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawCircle(cx, cy, radius, stroke)
        val shaft = 17f
        val head = 10f
        val direction = if (down) 1f else -1f
        canvas.drawLine(cx, cy - shaft * direction, cx, cy + shaft * direction, stroke)
        canvas.drawLine(cx, cy + shaft * direction, cx - head, cy + (shaft - head) * direction, stroke)
        canvas.drawLine(cx, cy + shaft * direction, cx + head, cy + (shaft - head) * direction, stroke)
    }

}
