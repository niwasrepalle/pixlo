package com.pixlo.app
import android.content.Context
import androidx.work.*
import java.time.LocalDate
import java.util.concurrent.TimeUnit
object DailyRefresh {
    fun schedule(context: Context) {
        val manager = WorkManager.getInstance(context)
        manager.enqueueUniquePeriodicWork("pixlo_widgets", ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES).build())
        manager.enqueueUniquePeriodicWork("pixlo_internet", ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<InternetRefreshWorker>(12, TimeUnit.HOURS).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
    }
}
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result { StreakWidgetProvider.updateAll(applicationContext); return Result.success() }
}
class InternetRefreshWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val store = PhotoStore(applicationContext); val s = store.settings()
        if (s.mode != "quotes" || s.quoteSource != "internet") return Result.success()
        if (store.preferences.getString("internet_${s.genre}_day", "") == LocalDate.now().toString()) return Result.success()
        return try { InternetQuotes(applicationContext).refresh(s.genre); Result.success() } catch (_: Exception) { Result.retry() }
    }
}
