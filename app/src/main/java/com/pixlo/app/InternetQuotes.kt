package com.pixlo.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.text.Html
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToInt

data class InternetQuote(val file: File, val title: String, val source: String, val attribution: String)
class InternetQuotes(private val context: Context) {
    private val directory = File(context.filesDir, "internet_quotes").apply { mkdirs() }
    private fun manifest(genre: String) = File(directory, "$genre.json")
    fun entries(genre: String): List<InternetQuote> = runCatching {
        val a = JSONArray(manifest(genre).readText())
        (0 until a.length()).map { i -> val item = a.getJSONObject(i); InternetQuote(File(directory, item.getString("file")), item.getString("title"), item.getString("source"), item.getString("attribution")) }.filter { it.file.exists() }
    }.getOrDefault(emptyList())
    fun today(genre: String, date: LocalDate = LocalDate.now()): InternetQuote? { val list = entries(genre); return list.getOrNull(DailyRules.dailyIndex(date, list.size)) }
    private fun request(url: String, limit: Int): ByteArray {
        require(url.startsWith("https://"))
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000; c.readTimeout = 20000
        c.setRequestProperty("User-Agent", "PIXLO/2.0 personal Android wallpaper app (Wikimedia Commons client)")
        try {
            check(c.responseCode in 200..299) { "Image source returned ${c.responseCode}. Try again later." }
            return c.inputStream.use { input -> val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(16384)
                while (true) { val n = input.read(buffer); if (n < 0) break; check(out.size() + n <= limit) { "Image exceeds download limit." }; out.write(buffer, 0, n) }; out.toByteArray() }
        } finally { c.disconnect() }
    }
    companion object { private val refreshLock = Any() }
    fun refresh(genre: String): Int = synchronized(refreshLock) { refreshLocked(genre) }
    private fun refreshLocked(genre: String): Int {
        require(genre in QuoteBank.genres)
        val term = when (genre) { "Gym" -> "fitness"; "Time" -> "time"; "Study" -> "learning"; "Heartbreak" -> "healing"; else -> "work" }
        val search = URLEncoder.encode("motivational quote $term", "UTF-8")
        val url = "https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch=$search&gsrnamespace=6&gsrlimit=12&prop=imageinfo&iiprop=url%7Cmime%7Cextmetadata&iiurlwidth=1280&format=json"
        val root = JSONObject(String(request(url, 2_000_000), Charsets.UTF_8))
        val pages = root.optJSONObject("query")?.optJSONObject("pages") ?: error("No quotation images found for $genre. Try another genre or your photos.")
        val output = JSONArray(); val created = mutableListOf<File>(); val old = entries(genre)
        try {
            for (key in pages.keys().asSequence().toList().sorted()) {
                if (output.length() >= 8) break
                val page = pages.getJSONObject(key); val info = page.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
                if (info.optString("mime") !in listOf("image/jpeg", "image/png", "image/webp")) continue
                val imageUrl = info.optString("thumburl", info.optString("url"))
                if (!imageUrl.startsWith("https://upload.wikimedia.org/")) continue
                val result = runCatching {
                    val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(request(imageUrl, 12_000_000)))) { decoder, header, _ ->
                        val scale = minOf(1.0, 1280.0 / maxOf(header.size.width, header.size.height))
                        decoder.setTargetSize((header.size.width * scale).roundToInt().coerceAtLeast(1), (header.size.height * scale).roundToInt().coerceAtLeast(1)); decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE }
                    val file = File(directory, "${genre}_${UUID.randomUUID()}.jpg"); created += file
                    try { file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 94, it)) } } finally { bitmap.recycle() }
                    val meta = info.optJSONObject("extmetadata")
                    fun clean(field: String) = Html.fromHtml(meta?.optJSONObject(field)?.optString("value", "") ?: "", Html.FROM_HTML_MODE_LEGACY).toString().trim().take(400)
                    JSONObject().put("file", file.name).put("title", page.optString("title").removePrefix("File:"))
                        .put("source", info.optString("descriptionurl")).put("attribution", listOf(clean("Artist"), clean("LicenseShortName")).filter { it.isNotBlank() }.joinToString(" • "))
                }
                result.getOrNull()?.let { output.put(it) }
            }
            check(output.length() > 0) { "No images could be downloaded. Existing wallpapers are kept; try again later." }
            val pending = File(directory, "$genre.tmp"); pending.writeText(output.toString()); check(pending.renameTo(manifest(genre)))
            old.forEach { it.file.delete() }
            PhotoStore(context).preferences.edit().putString("internet_revision", UUID.randomUUID().toString()).putString("internet_${genre}_day", LocalDate.now().toString()).apply()
            return output.length()
        } catch (e: Exception) { created.forEach { it.delete() }; throw e }
    }
}
