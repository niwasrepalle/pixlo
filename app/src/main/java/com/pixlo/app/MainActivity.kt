package com.pixlo.app

import android.app.Application
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

data class UiState(val settings: Settings = Settings(), val photos: List<File> = emptyList(), val busy: Boolean = false, val message: String? = null, val revision: Long = 0)
class PixloViewModel(application: Application) : AndroidViewModel(application), SharedPreferences.OnSharedPreferenceChangeListener {
    val store = PhotoStore(application)
    private val mutable = MutableStateFlow(UiState(store.settings(), store.photos()))
    val state = mutable.asStateFlow()
    init { store.preferences.registerOnSharedPreferenceChangeListener(this); DailyRefresh.schedule(application) }
    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) { mutable.value = mutable.value.copy(settings = store.settings(), photos = store.photos(), revision = mutable.value.revision + 1) }
    fun save(settings: Settings) = store.save(settings)
    fun message(text: String) { mutable.value = mutable.value.copy(message = text) }
    fun clearMessage() { mutable.value = mutable.value.copy(message = null) }
    fun importPhotos(uris: List<Uri>) {
        if (mutable.value.busy || uris.isEmpty()) return
        mutable.value = mutable.value.copy(busy = true)
        viewModelScope.launch {
            var added = 0; var failed = 0
            withContext(Dispatchers.IO) { uris.forEach { try { store.importPhoto(it); added++ } catch (_: Exception) { failed++ } } }
            mutable.value = mutable.value.copy(photos = store.photos(), busy = false, message = "$added photo(s) added." + if (failed > 0) " $failed failed or exceeded the 50-photo limit." else "")
        }
    }
    fun delete(file: File) {
        if (mutable.value.busy) return
        mutable.value = mutable.value.copy(busy = true)
        viewModelScope.launch { val result = withContext(Dispatchers.IO) { runCatching { store.delete(file) } }; mutable.value = mutable.value.copy(photos = store.photos(), busy = false, message = if (result.isSuccess) "Photo removed." else "Could not remove photo.") }
    }
    fun download() {
        if (mutable.value.busy) return
        val genre = mutable.value.settings.genre; mutable.value = mutable.value.copy(busy = true)
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { InternetQuotes(getApplication()).refresh(genre) } }
            mutable.value = mutable.value.copy(busy = false, revision = mutable.value.revision + 1, message = result.fold({ "$it quotation images downloaded for $genre." }, { it.message ?: "Download failed. Cached images are kept." }))
        }
    }
    override fun onCleared() { store.preferences.unregisterOnSharedPreferenceChangeListener(this) }
}

@Composable fun PixloTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFB9F56B), background = Color(0xFF101620), surface = Color(0xFF1F2836)), content = content)
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PixloTheme { val model: PixloViewModel = viewModel(); PixloScreen(model) {
            try { startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this, PixloWallpaperService::class.java))) }
            catch (_: android.content.ActivityNotFoundException) { try { startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)) } catch (_: android.content.ActivityNotFoundException) { model.message("This device has no live wallpaper picker.") } }
        } } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun Choices(items: List<String>, selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items.forEach { label -> FilterChip(selected == label, onClick = { onSelect(label) }, label = { Text(label) }) }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable fun ColorChoices(selected: Int, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PixloColors.forEachIndexed { index, color ->
            FilterChip(selected == color, onClick = { onSelect(color) }, label = { Text(if (selected == color) "✓" else "●", color = Color(color)) }, modifier = Modifier.semantics { contentDescription = listOf("Lime", "Blue", "Gold", "Purple", "Pink", "White")[index] })
        }
    }
}

@Composable fun PixloScreen(model: PixloViewModel, activate: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle(); val s = state.settings; val context = LocalContext.current
    var page by remember { mutableStateOf("home") }
    var selected by remember { mutableIntStateOf(0) }
    var custom by remember(s.seconds) { mutableStateOf(s.seconds.toString()) }
    var block by remember(s.block) { mutableFloatStateOf(s.block.toFloat()) }
    var fontSize by remember(s.fontSize) { mutableFloatStateOf(s.fontSize.toFloat()) }
    var confirmDelete by remember { mutableStateOf<File?>(null) }
    var widgetRevision by remember { mutableIntStateOf(0) }
    val snackbars = remember { SnackbarHostState() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { model.importPhotos(it) }
    LaunchedEffect(state.photos.size) { selected = selected.coerceIn(0, (state.photos.size - 1).coerceAtLeast(0)) }
    LaunchedEffect(state.message) { state.message?.let { snackbars.showSnackbar(it); model.clearMessage() } }
    val personal = state.photos.getOrNull(if (page == "quotes") DailyRules.dailyIndex(LocalDate.now(), state.photos.size) else selected)
    var preview by remember(page, personal, s, state.revision) { mutableStateOf<Bitmap?>(null) }
    var online by remember { mutableStateOf<InternetQuote?>(null) }
    var previewError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(page, personal, s, state.revision) {
        previewError = null
        val result = withContext(Dispatchers.IO) { runCatching {
            when (page) {
                "dots" -> WallpaperArt.dots(540, 960, s)
                "photos" -> personal?.let { model.store.filtered(it, s)?.let { b -> try { WallpaperArt.fit(b, 540, 960) } finally { b.recycle() } } }
                "quotes" -> if (s.quoteSource == "internet") {
                    val item = InternetQuotes(context).today(s.genre)
                    item to item?.let { BitmapFactory.decodeFile(it.file.absolutePath)?.let { b -> try { WallpaperArt.fit(b, 540, 960, true) } finally { b.recycle() } } }
                } else personal?.let { WallpaperArt.quote(context, it, 540, 960, s) }
                else -> null
            }
        } }
        val value = result.getOrNull()
        if (value is Pair<*, *>) { online = value.first as? InternetQuote; preview = value.second as? Bitmap } else { online = null; preview = value as? Bitmap }
        if (result.isFailure) previewError = "Preview could not be rendered. Try another photo."
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbars) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (page == "home") {
                Text("PIXLO", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                Text("Small moments. Bigger days.", style = MaterialTheme.typography.titleMedium)
                val cards = listOf(Triple("photos", "Photo wallpapers", "Your photos, six styles and smooth transitions"), Triple("quotes", "Daily quotes", "Internet images or quotes on your photos"), Triple("dots", "Year dots", "Every day, one more dot"), Triple("widgets", "Streak widgets", "One tap a day. A widget for every habit."))
                cards.forEachIndexed { i, card -> Card(onClick = { page = card.first }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(listOf("▧", "❝", "● ● ●", "🔥")[i], color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineMedium)
                        Text(card.second, style = MaterialTheme.typography.titleLarge); Text(card.third)
                    }
                } }
                Text("Active wallpaper mode: ${s.mode.replaceFirstChar { it.uppercase() }}")
            } else {
                TextButton(onClick = { page = "home" }) { Text("← PIXLO") }
                Text(when (page) { "photos" -> "Photo wallpapers"; "quotes" -> "Daily quotes"; "dots" -> "Year dots"; else -> "Streak widgets" }, style = MaterialTheme.typography.headlineMedium)
                if (page != "widgets") {
                    Box(Modifier.fillMaxWidth().height(360.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                        preview?.let { Image(it.asImageBitmap(), "Wallpaper preview", Modifier.fillMaxSize()) }
                            ?: Text(previewError ?: if (page == "quotes" && s.quoteSource == "internet") "Download quote images below" else if (personal == null) "Add photos to begin" else "Preparing wallpaper…", modifier = Modifier.padding(24.dp))
                    }
                }
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                fun addPhotos() { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                if (page == "photos" || (page == "quotes" && s.quoteSource == "personal")) {
                    Button(onClick = { addPhotos() }, enabled = !state.busy && state.photos.size < 50, modifier = Modifier.fillMaxWidth()) { Text("Add photos (${state.photos.size}/50)") }
                    if (page == "photos" && state.photos.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { selected = (selected - 1 + state.photos.size) % state.photos.size }) { Text("Previous") }
                            Text("${selected + 1} / ${state.photos.size}")
                            TextButton(onClick = { selected = (selected + 1) % state.photos.size }) { Text("Next") }
                        }
                        TextButton(onClick = { confirmDelete = personal }, enabled = !state.busy) { Text("Remove this photo") }
                    }
                }
                if (page == "photos") {
                    Text("Filter style", style = MaterialTheme.typography.titleMedium)
                    Choices(PhotoFilters.names, s.filter) { model.save(s.copy(filter = it)) }
                    Text("Applies automatically to every photo.")
                    if (s.filter == "Pixel") {
                        Text("Pixel block size: ${block.toInt()}")
                        Slider(block, { block = it }, valueRange = 2f..128f, onValueChangeFinished = { model.save(s.copy(block = block.toInt())) })
                    }
                    Text("Transition effect", style = MaterialTheme.typography.titleMedium)
                    Choices(listOf("Fade", "Slide", "Zoom", "None"), s.transition) { model.save(s.copy(transition = it)) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(s.rotating, { model.save(s.copy(rotating = it)) }); Spacer(Modifier.width(12.dp)); Text("Random rotation") }
                    Choices(listOf("10s", "30s", "60s"), "${s.seconds}s") { model.save(s.copy(seconds = it.removeSuffix("s").toLong())) }
                    OutlinedTextField(custom, { custom = it }, label = { Text("Custom seconds (1–86400)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                    OutlinedButton(onClick = { val n = custom.toLongOrNull(); if (n != null && RotationRules.validSeconds(n)) model.save(s.copy(seconds = n)) else model.message("Enter whole seconds from 1 to 86400.") }) { Text("Save interval (${s.seconds}s)") }
                }
                if (page == "quotes") {
                    Choices(listOf("Internet wallpapers", "Your photos"), if (s.quoteSource == "internet") "Internet wallpapers" else "Your photos") { model.save(s.copy(quoteSource = if (it == "Internet wallpapers") "internet" else "personal")) }
                    Text("Quote genre", style = MaterialTheme.typography.titleMedium)
                    Choices(QuoteBank.genres, s.genre) { model.save(s.copy(genre = it)) }
                    if (s.quoteSource == "internet") {
                        Button(onClick = { model.download() }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Download / refresh ${s.genre} images") }
                        Text("Quotation images from Wikimedia Commons. Search relevance varies by genre. Cached images rotate daily and remain available offline.")
                        online?.let { item -> Text(item.title); Text(item.attribution); TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.source))) } }) { Text("View image source and license") } }
                        Text("Text inside an internet image is part of that image. For editable fonts, choose Your photos.")
                    } else {
                        Text(QuoteBank.today(s.genre), style = MaterialTheme.typography.titleMedium)
                        Text("Font style")
                        Choices(listOf("Sans", "Serif", "Handwritten"), s.font) { model.save(s.copy(font = it)) }
                        Text("Font size: ${fontSize.toInt()}")
                        Slider(fontSize, { fontSize = it }, valueRange = 24f..72f, onValueChangeFinished = { model.save(s.copy(fontSize = fontSize.toInt())) })
                        Text("Text colour"); ColorChoices(s.textColor) { model.save(s.copy(textColor = it)) }
                        Text("Quote placement")
                        Choices(listOf("Auto", "Top", "Middle", "Bottom"), s.placement) { model.save(s.copy(placement = it)) }
                        Text("Auto uses object detection to avoid the subject and choose a quiet area. The detector may need its first model download; if detection is unavailable, placement uses image texture. Manual placement is available.")
                    }
                    Text("Changes daily using your phone's local date.")
                }
                if (page == "dots") {
                    Text("Dot colour"); ColorChoices(s.dotColor) { model.save(s.copy(dotColor = it)) }
                    Choices(listOf("Dark", "Light"), if (s.lightDots) "Light" else "Dark") { model.save(s.copy(lightDots = it == "Light")) }
                    Text("Only dots and a small footer: days left : year completed. Dates are calculated automatically; leap years have 366 dots.")
                }
                if (page != "widgets") {
                    val allowed = page == "dots" || if (page == "quotes" && s.quoteSource == "internet") InternetQuotes(context).entries(s.genre).isNotEmpty() else state.photos.isNotEmpty()
                    Button(onClick = { model.save(s.copy(mode = page)); activate() }, enabled = allowed && !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Set live wallpaper") }
                } else {
                    WidgetEditor { habit ->
                        if (pinWidget(context, habit)) { model.message("Confirm the widget on your home screen."); widgetRevision++ }
                        else model.message("Long-press your home screen → Widgets → PIXLO Streak. Configure each widget there.")
                    }
                    Text("Add as many widgets as you like. Each has its own name, colour, size and streak. One tap a day; same-day taps never increase the count.")
                    TextButton(onClick = { widgetRevision++ }) { Text("Refresh my widgets") }
                    val ids = remember(widgetRevision) { StreakWidgetProvider.ids(context).toList() }
                    ids.forEach { id ->
                        val habit = HabitStore(context).get(id)
                        Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${habit.name}  🔥 ${DailyRules.visibleStreak(habit.state, LocalDate.now().toEpochDay())}", Modifier.weight(1f))
                            TextButton(onClick = { context.startActivity(Intent(context, WidgetConfigActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)) }) { Text("Edit") }
                        } }
                    }
                    Text("A missed full day resets that widget. Launcher/background refresh can be delayed while your phone sleeps; the next tap always uses the current date.")
                }
            }
        }
    }
    confirmDelete?.let { file -> AlertDialog(onDismissRequest = { confirmDelete = null }, title = { Text("Remove photo?") }, text = { Text("Only PIXLO's copy is removed. Your gallery original stays.") }, confirmButton = { TextButton(onClick = { model.delete(file); confirmDelete = null }) { Text("Remove") } }, dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } }) }
}
