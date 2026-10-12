package com.pixlo.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import java.time.LocalDate

data class Habit(val name: String = "My streak", val color: Int = 0xFFB9F56B.toInt(), val wide: Boolean = false,
    val state: StreakState = StreakState())

class HabitStore(context: Context) {
    private val prefs = context.getSharedPreferences("streaks", Context.MODE_PRIVATE)
    fun get(id: Int) = Habit(prefs.getString("${id}_name", "My streak") ?: "My streak", prefs.getInt("${id}_color", 0xFFB9F56B.toInt()),
        prefs.getBoolean("${id}_wide", false), StreakState(prefs.getInt("${id}_count", 0),
            prefs.getLong("${id}_last", Long.MIN_VALUE).takeUnless { it == Long.MIN_VALUE }))
    fun save(id: Int, habit: Habit) {
        prefs.edit().putString("${id}_name", habit.name.take(40)).putInt("${id}_color", habit.color)
            .putBoolean("${id}_wide", habit.wide).putInt("${id}_count", habit.state.count)
            .putLong("${id}_last", habit.state.lastDay ?: Long.MIN_VALUE).commit()
    }
    fun remove(id: Int) { val e = prefs.edit(); listOf("name", "color", "wide", "count", "last", "before_count", "before_last", "toggle_day").forEach { e.remove("${id}_$it") }; e.apply() }
    @Synchronized fun tap(id: Int) {
        val today = LocalDate.now().toEpochDay()
        val state = get(id).state
        val previous = if (prefs.getLong("${id}_toggle_day", Long.MIN_VALUE) == today && prefs.contains("${id}_before_count"))
            StreakState(prefs.getInt("${id}_before_count", 0), prefs.getLong("${id}_before_last", Long.MIN_VALUE).takeUnless { it == Long.MIN_VALUE }) else null
        val next = DailyRules.toggle(state, previous, today)
        val edit = prefs.edit()
        if (state.lastDay != today) {
            edit.putInt("${id}_before_count", state.count)
                .putLong("${id}_before_last", state.lastDay ?: Long.MIN_VALUE)
                .putLong("${id}_toggle_day", today)
        }
        edit.putInt("${id}_count", next.count).putLong("${id}_last", next.lastDay ?: Long.MIN_VALUE).commit()
    }
}

class StreakWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        DailyRefresh.schedule(context)
        ids.forEach { update(context, it) }
    }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) { update(context, id) }
    override fun onDeleted(context: Context, ids: IntArray) { ids.forEach { HabitStore(context).remove(it) } }
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TAP) {
            val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val actual = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, StreakWidgetProvider::class.java))
            if (id in actual) { HabitStore(context).tap(id); update(context, id) }
        }
        if (intent.action in listOf(Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) updateAll(context)
    }
    companion object {
        const val ACTION_TAP = "com.pixlo.app.STREAK_TAP"
        fun ids(context: Context): IntArray = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, StreakWidgetProvider::class.java))
        fun updateAll(context: Context) { ids(context).forEach { update(context, it) } }
        fun update(context: Context, id: Int) {
            val habit = HabitStore(context).get(id)
            val today = LocalDate.now().toEpochDay()
            val count = DailyRules.visibleStreak(habit.state, today)
            val views = RemoteViews(context.packageName, if (habit.wide) R.layout.streak_widget_wide else R.layout.streak_widget)
            views.setTextViewText(R.id.habit_name, habit.name)
            views.setTextViewText(R.id.streak_count, "🔥 $count")
            views.setTextColor(R.id.streak_count, habit.color)
            views.setTextViewText(R.id.streak_status, if (habit.state.lastDay == today) "Checked in · Tap to undo" else if (count == 0) "Tap to start" else "Tap to check in")
            val tap = Intent(context, StreakWidgetProvider::class.java).setAction(ACTION_TAP)
                .setData(Uri.parse("pixlo://streak/$id")).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getBroadcast(context, id, tap,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            AppWidgetManager.getInstance(context).updateAppWidget(id, views)
        }
    }
}
