package com.pixlo.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

data class Settings(val block: Int = 24, val seconds: Long = 30, val rotating: Boolean = true,
    val mode: String = "photos", val filter: String = "Pixel", val transition: String = "Fade",
    val quoteSource: String = "personal", val genre: String = "Work", val font: String = "Sans",
    val fontSize: Int = 44, val textColor: Int = android.graphics.Color.WHITE,
    val placement: String = "Auto", val dotColor: Int = 0xFFB9F56B.toInt(), val lightDots: Boolean = false)

class PhotoStore(context: Context) {
    private val app = context.applicationContext
    val preferences = app.getSharedPreferences("pixlo", Context.MODE_PRIVATE)
    private val directory = File(app.filesDir, "photos").apply { mkdirs() }
    fun settings() = Settings(preferences.getInt("block", 24).coerceIn(2, 128),
        preferences.getLong("seconds", 30).coerceIn(1, 86400), preferences.getBoolean("rotating", true),
        preferences.getString("mode", "photos") ?: "photos", preferences.getString("filter", "Pixel") ?: "Pixel",
        preferences.getString("transition", "Fade") ?: "Fade", preferences.getString("quoteSource", "personal") ?: "personal",
        preferences.getString("genre", "Work") ?: "Work", preferences.getString("font", "Sans") ?: "Sans",
        preferences.getInt("fontSize", 44).coerceIn(24, 72), preferences.getInt("textColor", -1),
        preferences.getString("placement", "Auto") ?: "Auto", preferences.getInt("dotColor", 0xFFB9F56B.toInt()),
        preferences.getBoolean("lightDots", false))
    fun save(settings: Settings) {
        require(RotationRules.validSeconds(settings.seconds))
        require(settings.block in 2..128)
        preferences.edit().putInt("block", settings.block).putLong("seconds", settings.seconds)
            .putBoolean("rotating", settings.rotating).putString("mode", settings.mode)
            .putString("filter", settings.filter).putString("transition", settings.transition)
            .putString("quoteSource", settings.quoteSource).putString("genre", settings.genre)
            .putString("font", settings.font).putInt("fontSize", settings.fontSize).putInt("textColor", settings.textColor)
            .putString("placement", settings.placement).putInt("dotColor", settings.dotColor).putBoolean("lightDots", settings.lightDots).apply()
    }
    fun photos(): List<File> = directory.listFiles()?.filter { it.extension == "jpg" }?.sortedBy { it.name } ?: emptyList()
    private fun notifyPhotos() { preferences.edit().putString("photos_revision", UUID.randomUUID().toString()).apply() }
    fun importPhoto(uri: Uri) {
        require(photos().size < 50) { "The library limit is 50 photos. Remove some before adding more." }
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(app.contentResolver, uri)) { decoder, info, _ ->
            val scale = minOf(1.0, 1280.0 / maxOf(info.size.width, info.size.height))
            decoder.setTargetSize((info.size.width * scale).roundToInt().coerceAtLeast(1), (info.size.height * scale).roundToInt().coerceAtLeast(1))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val pending = File(directory, "${UUID.randomUUID()}.tmp")
        try {
            pending.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it)) }
            check(pending.renameTo(File(directory, pending.nameWithoutExtension + ".jpg")))
            notifyPhotos()
        } finally { bitmap.recycle(); pending.delete() }
    }
    fun delete(file: File) {
        require(file.parentFile?.canonicalFile == directory.canonicalFile)
        check(!file.exists() || file.delete()) { "Could not remove photo." }
        app.getSharedPreferences("subjects", Context.MODE_PRIVATE).edit().remove(file.name).apply()
        notifyPhotos()
    }
    fun preview(file: File, block: Int): Bitmap? {
        val original = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        return try { Pixelator.apply(original, block) } finally { original.recycle() }
    }
    fun filtered(file: File, settings: Settings): Bitmap? {
        val original = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        return try { PhotoFilters.apply(original, settings.filter, settings.block) } finally { original.recycle() }
    }
}
