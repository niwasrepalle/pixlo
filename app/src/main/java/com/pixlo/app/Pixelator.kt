package com.pixlo.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.ceil
import kotlin.random.Random

object RotationRules {
    fun validSeconds(value: Long) = value in 1L..86400L
    fun nextIndex(count: Int, previous: Int, random: Random = Random.Default): Int {
        if (count <= 1) return 0
        if (previous !in 0 until count) return random.nextInt(count)
        val choice = random.nextInt(count - 1)
        return if (choice >= previous) choice + 1 else choice
    }
    fun gridSize(size: Int, block: Int): Int = ceil(size.toDouble() / block.coerceAtLeast(1)).toInt().coerceAtLeast(1)
}

object Pixelator {
    // Downsample averages nearby colors; nearest-neighbor enlargement preserves square edges.
    fun apply(source: Bitmap, block: Int): Bitmap {
        val tiny = Bitmap.createScaledBitmap(source,
            RotationRules.gridSize(source.width, block),
            RotationRules.gridSize(source.height, block), true)
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(output).drawBitmap(tiny, null, Rect(0, 0, output.width, output.height), Paint().apply { isFilterBitmap = false })
        if (tiny !== source) tiny.recycle()
        return output
    }
}
