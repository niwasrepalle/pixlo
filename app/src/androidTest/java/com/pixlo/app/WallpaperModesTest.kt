package com.pixlo.app
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class) class WallpaperModesTest {
    @Test fun allFiltersProduceValidImagesAndKeepSourceAlive() {
        val source = Bitmap.createBitmap(40, 60, Bitmap.Config.ARGB_8888); source.eraseColor(Color.BLUE)
        PhotoFilters.names.forEach { filter -> val out = PhotoFilters.apply(source, filter, 8); assertTrue(out.width > 0); assertTrue(out.height > 0); assertFalse(source.isRecycled); out.recycle() }; source.recycle()
    }
    @Test fun dotWallpaperDrawsFilledAndRemainingDots() {
        val out = WallpaperArt.dots(400, 800, Settings(), LocalDate.of(2026, 1, 1))
        val gap = minOf(400 * .83f / 20, 800 * .61f / 19)
        val left = (400 - 19 * gap) / 2; val top = (800 - 18 * gap) / 2
        assertEquals(Settings().dotColor, out.getPixel(left.toInt(), top.toInt()))
        assertNotEquals(Settings().dotColor, out.getPixel((left + gap).toInt(), top.toInt()))
        assertEquals(400, out.width); assertEquals(800, out.height); out.recycle()
    }
}
