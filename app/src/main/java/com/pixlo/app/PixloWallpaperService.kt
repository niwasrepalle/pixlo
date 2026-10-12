package com.pixlo.app
import android.content.SharedPreferences
import android.graphics.*
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.time.LocalDate

class PixloWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = PixloEngine()
    inner class PixloEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private val store = PhotoStore(this@PixloWallpaperService)
        private val thread = HandlerThread("PIXLO renderer").apply { start() }
        private val handler = Handler(thread.looper)
        @Volatile private var visible = false
        @Volatile private var surfaceReady = false
        @Volatile private var destroyed = false
        private var currentPath: String? = null
        private var current: Bitmap? = null
        private var old: Bitmap? = null
        private var lastSettings: Settings? = null
        private var day: LocalDate? = null
        private var start = 0L
        private val tick = Runnable { render(true) }
        private val frame = object : Runnable {
            override fun run() {
                if (!visible || !surfaceReady || destroyed) { old?.recycle(); old = null; return }
                val progress = ((SystemClock.uptimeMillis() - start) / 650f).coerceIn(0f, 1f)
                draw(progress)
                if (progress < 1 && old != null) handler.postDelayed(this, 33) else { old?.recycle(); old = null; schedule() }
            }
        }
        override fun onCreate(holder: SurfaceHolder) { super.onCreate(holder); store.preferences.registerOnSharedPreferenceChangeListener(this); DailyRefresh.schedule(this@PixloWallpaperService) }
        override fun onVisibilityChanged(value: Boolean) { visible = value; handler.removeCallbacks(tick); handler.removeCallbacks(frame); if (value) handler.post { render(false) } }
        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) { super.onSurfaceChanged(holder, format, width, height); surfaceReady = true; handler.post { render(false) } }
        override fun onSurfaceDestroyed(holder: SurfaceHolder) { surfaceReady = false; handler.removeCallbacks(tick); handler.removeCallbacks(frame); super.onSurfaceDestroyed(holder) }
        override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) { handler.post { render(false) } }
        private fun render(advance: Boolean) {
            handler.removeCallbacks(tick); handler.removeCallbacks(frame)
            if (!visible || !surfaceReady || destroyed) return
            val s = store.settings(); val today = LocalDate.now(); val surface = surfaceHolder.surfaceFrame
            if (advance && s.mode != "photos" && day == today && s == lastSettings && current != null) { draw(1f); schedule(); return }
            val ratio = surface.height().toFloat() / surface.width().coerceAtLeast(1)
            val width = minOf(720, surface.width().coerceAtLeast(1)); val height = (width * ratio).toInt().coerceIn(1, 2000)
            val photos = store.photos(); val prev = photos.indexOfFirst { it.absolutePath == currentPath }
            val index = if (s.mode == "quotes") DailyRules.dailyIndex(today, photos.size) else if ((advance && s.rotating) || prev < 0) RotationRules.nextIndex(photos.size, prev) else prev
            val photo = photos.getOrNull(index); val changedDay = day != today; day = today
            val next = try {
                when (s.mode) {
                    "dots" -> WallpaperArt.dots(width, height, s, today)
                    "quotes" -> if (s.quoteSource == "internet") InternetQuotes(this@PixloWallpaperService).today(s.genre, today)?.let { item -> BitmapFactory.decodeFile(item.file.absolutePath)?.let { b -> try { WallpaperArt.fit(b, width, height, true) } finally { b.recycle() } } }
                        else photo?.let { WallpaperArt.quote(this@PixloWallpaperService, it, width, height, s, today) }
                    else -> photo?.let { store.filtered(it, s)?.let { b -> try { WallpaperArt.fit(b, width, height) } finally { b.recycle() } } }
                }
            } catch (_: Exception) { null }
            val animate = s.mode == "photos" && advance && photo?.absolutePath != currentPath && current != null && next != null && s.transition != "None" && s == lastSettings && !changedDay
            currentPath = photo?.absolutePath; lastSettings = s; old?.recycle(); old = null
            if (animate) { old = current; current = next; start = SystemClock.uptimeMillis(); frame.run() }
            else { current?.recycle(); current = next; draw(1f); schedule() }
        }
        private fun schedule() {
            if (!visible || !surfaceReady || destroyed) return
            val s = store.settings()
            val delay = if (s.mode == "photos") { if (!s.rotating || store.photos().size < 2) return; s.seconds * 1000L }
                else minOf(60_000L, java.time.Duration.between(java.time.ZonedDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault())).toMillis().coerceAtLeast(1000))
            handler.postDelayed(tick, delay)
        }
        private fun draw(progress: Float) {
            if (!surfaceReady || destroyed || !visible) return
            val canvas = try { surfaceHolder.lockCanvas() } catch (_: RuntimeException) { null } ?: return
            try {
                canvas.drawColor(Color.rgb(15, 21, 32)); val target = RectF(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat()); val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                fun image(bitmap: Bitmap?, rect: RectF = target, alpha: Int = 255) { bitmap?.let { paint.alpha = alpha; canvas.drawBitmap(it, null, rect, paint) } }
                if (old != null && progress < 1f) {
                    when (lastSettings?.transition) {
                        "Slide" -> { image(old, RectF(-canvas.width * progress, 0f, canvas.width * (1 - progress), canvas.height.toFloat())); image(current, RectF(canvas.width * (1 - progress), 0f, canvas.width * (2 - progress), canvas.height.toFloat())) }
                        "Zoom" -> { image(old); val extra = canvas.width * .10f * (1 - progress); image(current, RectF(-extra, -extra, canvas.width + extra, canvas.height + extra), (255 * progress).toInt()) }
                        else -> { image(old); image(current, alpha = (255 * progress).toInt()) }
                    }
                } else if (current != null) image(current)
                else canvas.drawText(if (lastSettings?.quoteSource == "internet" && lastSettings?.mode == "quotes") "Download quote images in PIXLO" else "Add photos in PIXLO", 24f, canvas.height / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 24f })
            } finally { try { surfaceHolder.unlockCanvasAndPost(canvas) } catch (_: RuntimeException) { } }
        }
        override fun onDestroy() { destroyed = true; store.preferences.unregisterOnSharedPreferenceChangeListener(this); handler.removeCallbacksAndMessages(null); handler.post { current?.recycle(); old?.recycle(); current = null; old = null }; thread.quitSafely(); super.onDestroy() }
    }
}
