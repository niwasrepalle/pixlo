package com.pixlo.app

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

object PhotoFilters {
    val names = listOf("Original", "Pixel", "Cartoon", "Color paint", "Doodle", "Sketch")
    fun apply(source: Bitmap, style: String, block: Int): Bitmap {
        if (style == "Pixel") return Pixelator.apply(source, block)
        if (style == "Original") return source.copy(Bitmap.Config.ARGB_8888, false)
        val scale = minOf(1f, 900f / maxOf(source.width, source.height))
        val small = Bitmap.createScaledBitmap(source, (source.width * scale).toInt().coerceAtLeast(1), (source.height * scale).toInt().coerceAtLeast(1), true)
        val w = small.width; val h = small.height
        val pixels = IntArray(w * h); small.getPixels(pixels, 0, w, 0, 0, w, h)
        val result = IntArray(pixels.size)
        fun gray(c: Int) = (Color.red(c) * 30 + Color.green(c) * 59 + Color.blue(c) * 11) / 100
        for (y in 0 until h) for (x in 0 until w) {
            val i = y * w + x; val c = pixels[i]
            val edge = if (x > 0 && y > 0 && x < w - 1 && y < h - 1)
                abs(gray(pixels[i - 1]) - gray(pixels[i + 1])) + abs(gray(pixels[i - w]) - gray(pixels[i + w])) else 0
            fun quant(v: Int, step: Int) = ((v / step) * step + step / 2).coerceIn(0, 255)
            result[i] = when (style) {
                "Cartoon" -> if (edge > 60) Color.rgb(22, 26, 34) else Color.rgb(quant(Color.red(c), 40), quant(Color.green(c), 40), quant(Color.blue(c), 40))
                "Color paint" -> {
                    var r = 0; var g = 0; var b = 0; var n = 0
                    for (dy in -1..1) for (dx in -1..1) { val cc = pixels[(y + dy).coerceIn(0, h - 1) * w + (x + dx).coerceIn(0, w - 1)]; r += Color.red(cc); g += Color.green(cc); b += Color.blue(cc); n++ }
                    Color.rgb(quant(r / n, 28), quant(g / n, 28), quant(b / n, 28))
                }
                "Doodle" -> if (edge > 38) Color.rgb(25, 32, 48) else Color.rgb((Color.red(c) / 3 + 165).coerceAtMost(255), (Color.green(c) / 3 + 165).coerceAtMost(255), (Color.blue(c) / 3 + 165).coerceAtMost(255))
                else -> { val pencil = (255 - edge * 3 - (255 - gray(c)) / 7).coerceIn(15, 255); Color.rgb(pencil, pencil, pencil) }
            }
        }
        val output = Bitmap.createBitmap(result, w, h, Bitmap.Config.ARGB_8888)
        if (small !== source) small.recycle()
        return output
    }
}
