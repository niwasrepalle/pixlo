package com.pixlo.app

import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.min

object WallpaperArt {
    fun dots(width: Int, height: Int, settings: Settings, date: LocalDate = LocalDate.now()): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(if (settings.lightDots) Color.rgb(242, 243, 236) else Color.rgb(15, 21, 32))
        val year = DailyRules.year(date)
        val columns = 20; val rows = ceil(year.total / columns.toDouble()).toInt()
        val gap = min(width * .83f / columns, height * .61f / rows)
        val left = (width - (columns - 1) * gap) / 2
        val top = (height - (rows - 1) * gap) / 2
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 0 until year.total) {
            paint.color = if (i < year.filled) settings.dotColor else if (settings.lightDots) Color.rgb(188, 194, 201) else Color.rgb(60, 70, 86)
            canvas.drawCircle(left + i % columns * gap, top + i / columns * gap, gap * .32f, paint)
        }
        paint.color = if (settings.lightDots) Color.rgb(40, 45, 55) else Color.rgb(217, 223, 232)
        paint.textSize = width * .032f; paint.textAlign = Paint.Align.CENTER
        canvas.drawText("${year.left}D left : ${year.percentComplete}%", width / 2f, height * .92f, paint)
        return output
    }
    fun fit(source: Bitmap, width: Int, height: Int, contain: Boolean = false): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(output); c.drawColor(Color.rgb(15, 21, 32))
        val scale = if (contain) minOf(width.toFloat() / source.width, height.toFloat() / source.height) else maxOf(width.toFloat() / source.width, height.toFloat() / source.height)
        val w = source.width * scale; val h = source.height * scale
        c.drawBitmap(source, null, RectF((width - w) / 2, (height - h) / 2, (width + w) / 2, (height + h) / 2), Paint().apply { isFilterBitmap = false })
        return output
    }
    fun quote(context: Context, file: File, width: Int, height: Int, settings: Settings, date: LocalDate = LocalDate.now()): Bitmap? {
        val source = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        try {
            val detected = if (settings.placement == "Auto") SubjectDetection.boxes(context, file) else emptyList()
            val scale = maxOf(width.toFloat() / source.width, height.toFloat() / source.height)
            val subject = detected.maxByOrNull { it.width() * it.height() }
            val dx = if (subject == null) (width - source.width * scale) / 2 else
                (width / 2f - subject.centerX() * source.width * scale).coerceIn(minOf(0f, width - source.width * scale), 0f)
            val dy = if (subject == null) (height - source.height * scale) / 2 else
                (height / 2f - subject.centerY() * source.height * scale).coerceIn(minOf(0f, height - source.height * scale), 0f)
            val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val c = Canvas(output)
            c.drawBitmap(source, null, RectF(dx, dy, dx + source.width * scale, dy + source.height * scale), Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true })
            val objects = detected.map { b ->
                RectF(b.left * source.width * scale + dx, b.top * source.height * scale + dy,
                    b.right * source.width * scale + dx, b.bottom * source.height * scale + dy) }
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = settings.textColor; textSize = settings.fontSize * width / 720f
                typeface = when (settings.font) { "Serif" -> Typeface.create("serif", Typeface.BOLD); "Handwritten" -> Typeface.create("cursive", Typeface.NORMAL); else -> Typeface.create("sans-serif", Typeface.BOLD) }
            }
            val text = QuoteBank.today(settings.genre, date)
            val textWidth = (width * .62f).toInt()
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, textWidth).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false).setLineSpacing(2f, 1.1f).build()
            val padding = width * .045f
            val boxHeight = layout.height + padding * 2
            val candidates = listOf(.15f, .43f, .70f).flatMap { y -> listOf(.07f, .26f).map { x -> RectF(width * x, height * y, width * x + textWidth + padding * 2, height * y + boxHeight) } }
                .map { r -> if (r.bottom > height * .90f) { r.offset(0f, height * .90f - r.bottom) }; r }
            fun overlap(rect: RectF): Float = objects.sumOf { o -> val intersection = RectF(rect); if (intersection.intersect(o)) (intersection.width() * intersection.height()).toDouble() else 0.0 }.toFloat()
            val chosen = when (settings.placement) { "Top" -> candidates[0]; "Middle" -> candidates[2]; "Bottom" -> candidates[4]; else -> candidates.minWithOrNull(compareBy<RectF>({ overlap(it) }, { texture(output, it) })) ?: candidates[0] }
            c.drawRoundRect(chosen, width * .03f, width * .03f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(135, 12, 17, 26) })
            c.save(); c.translate(chosen.left + padding, chosen.top + padding); layout.draw(c); c.restore()
            return output
        } finally { source.recycle() }
    }
    private fun texture(bitmap: Bitmap, box: RectF): Float {
        var previous = 0; var variation = 0
        for (i in 0..15) { val x = (box.left + box.width() * (i % 4) / 4).toInt().coerceIn(0, bitmap.width - 1); val y = (box.top + box.height() * (i / 4) / 4).toInt().coerceIn(0, bitmap.height - 1)
            val color = bitmap.getPixel(x, y); val light = (Color.red(color) + Color.green(color) + Color.blue(color)) / 3
            if (i > 0) variation += kotlin.math.abs(light - previous); previous = light }
        return variation / 15f
    }
}
