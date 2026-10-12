package com.pixlo.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        setResult(RESULT_CANCELED)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        setContent {
            PixloTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp)) {
                        WidgetEditor(HabitStore(this@WidgetConfigActivity).get(id), "Save widget") { habit ->
                            val store = HabitStore(this@WidgetConfigActivity)
                            store.save(id, habit.copy(state = store.get(id).state))
                            StreakWidgetProvider.update(this@WidgetConfigActivity, id)
                            DailyRefresh.schedule(this@WidgetConfigActivity)
                            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)); finish()
                        }
                    }
                }
            }
        }
    }
}

val PixloColors = listOf(0xFFB9F56B.toInt(), 0xFF83C9FF.toInt(), 0xFFFFD979.toInt(), 0xFFE4A1F7.toInt(), 0xFFFFA9BE.toInt(), 0xFFFFFFFF.toInt())

@Composable fun WidgetEditor(initial: Habit = Habit(), button: String = "Add widget", onSave: (Habit) -> Unit) {
    var name by remember(initial) { mutableStateOf(initial.name) }
    var color by remember(initial) { mutableIntStateOf(initial.color) }
    var wide by remember(initial) { mutableStateOf(initial.wide) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Customize widget", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(name, { name = it.take(40) }, label = { Text("Habit name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Widget colour")
        ColorChoices(color) { color = it }
        Choices(listOf("Compact", "Wide"), if (wide) "Wide" else "Compact") { wide = it == "Wide" }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(24.dp)) {
            Text(name.ifBlank { "My streak" }, style = MaterialTheme.typography.titleLarge)
            Text("🔥 ${DailyRules.visibleStreak(initial.state, java.time.LocalDate.now().toEpochDay())}", color = Color(color), style = MaterialTheme.typography.headlineLarge)
            Text("One tap each day")
        } }
        Button(onClick = { onSave(initial.copy(name = name.trim(), color = color, wide = wide)) }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(button) }
    }
}

fun pinWidget(context: Context, habit: Habit): Boolean {
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) return false
    val intent = Intent(context, WidgetPinnedReceiver::class.java).setAction("com.pixlo.app.PINNED")
        .setData(android.net.Uri.parse("pixlo://pin/${java.util.UUID.randomUUID()}"))
        .putExtra("name", habit.name).putExtra("color", habit.color).putExtra("wide", habit.wide)
    val flag = if (android.os.Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
    val callback = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or flag)
    return manager.requestPinAppWidget(ComponentName(context, StreakWidgetProvider::class.java), null, callback)
}

class WidgetPinnedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id !in StreakWidgetProvider.ids(context)) return
        HabitStore(context).save(id, Habit(intent.getStringExtra("name") ?: "My streak", intent.getIntExtra("color", PixloColors[0]), intent.getBooleanExtra("wide", false)))
        StreakWidgetProvider.update(context, id); DailyRefresh.schedule(context)
    }
}
