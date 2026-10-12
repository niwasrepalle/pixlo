package com.pixlo.app

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PixelatorTest {
    @Test fun preservesDimensionsAndSolidColorWithoutMutatingSource() {
        val source = Bitmap.createBitmap(101, 73, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.RED)
        val result = Pixelator.apply(source, 24)
        assertEquals(101, result.width); assertEquals(73, result.height)
        assertEquals(Color.RED, result.getPixel(100, 72))
        assertFalse(source.isRecycled)
        result.recycle(); source.recycle()
    }
    @Test fun createsHardEdgedBlocks() {
        val source = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        for (y in 0..7) for (x in 0..7) source.setPixel(x, y, if (x < 4) Color.RED else Color.BLUE)
        val result = Pixelator.apply(source, 4)
        for (y in 0..7) for (x in 0..7) assertEquals(if (x < 4) Color.RED else Color.BLUE, result.getPixel(x, y))
        result.recycle(); source.recycle()
    }
}
