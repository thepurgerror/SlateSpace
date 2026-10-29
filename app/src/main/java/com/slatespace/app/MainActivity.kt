package com.slatespace.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush as GraphicsBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/*
 * SlateSpace 3.0 — Command Desk
 * Package, SharedPreferences file name, and existing notes/todos/strokes keys are retained.
 * This allows the new screen to migrate the original local data without uninstalling the app.
 */
private const val PREFS_NAME = "slatespace"
private const val KEY_NOTES = "notes"
private const val KEY_TODOS = "todos"
private const val KEY_LEGACY_STROKES = "strokes"
private const val KEY_DRAWINGS = "drawings"
private const val KEY_SELECTED_DRAWING = "selectedDrawingId"

private const val TAB_HOME = "Home"
private const val TAB_SPACE = "Space"
private const val TAB_TASKS = "Tasks"
private const val TAB_DRAW = "Draw"
private const val TAB_SEARCH = "Search"
private const val TAB_SETTINGS = "Settings"
const val EXTRA_START_TAB = "com.slatespace.app.extra.START_TAB"
private const val KEY_THEME_PRESET = "themePreset"
private const val KEY_ACCENT_NAME = "accentName"
private const val KEY_HAPTICS_ENABLED = "hapticsEnabled"
private const val KEY_MOTION_ENABLED = "animationsEnabled"
private const val KEY_SETUP_COMPLETE = "setupComplete"
private const val KEY_REMINDERS_ENABLED = "remindersEnabled"
private const val KEY_REMINDER_INTERVAL = "reminderIntervalMinutes"
private const val KEY_QUIET_START = "quietStartHour"
private const val KEY_QUIET_END = "quietEndHour"
private const val KEY_DISPLAY_NAME = "displayName"
private const val KEY_WORKSPACE_MODE = "workspaceMode"
private const val KEY_MIGRATION_PERMISSION_PROMPT_PENDING = "migrationNotificationPromptPending"

private fun scheduleTaskReminders(context: Context, enabled: Boolean, intervalMinutes: Int) {
    val workManager = WorkManager.getInstance(context)
    if (!enabled) {
        workManager.cancelUniqueWork(TaskReminderWorker.UNIQUE_WORK_NAME)
        return
    }
    // WorkManager periodic work has a 15-minute minimum; clamp stale/corrupt values safely.
    val safeIntervalMinutes = intervalMinutes.coerceIn(15, 20)
    val request = PeriodicWorkRequestBuilder<TaskReminderWorker>(safeIntervalMinutes.toLong(), TimeUnit.MINUTES)
        .addTag(TaskReminderWorker.UNIQUE_WORK_NAME)
        .build()
    workManager.enqueueUniquePeriodicWork(
        TaskReminderWorker.UNIQUE_WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

private var ActiveThemePreset by mutableStateOf("Midnight")
private var ActiveAccentName by mutableStateOf("Lime")
private var MotionSetting by mutableStateOf(true)
private var Bg by mutableStateOf(Color(0xFF0B0E12))
private var Panel by mutableStateOf(Color(0xFF141A21))
private var PanelRaised by mutableStateOf(Color(0xFF1B232D))
private var PanelActive by mutableStateOf(Color(0xFF26372C))
private var Border by mutableStateOf(Color(0xFF28323D))
private var TextMain by mutableStateOf(Color(0xFFF2F5F7))
private var Muted by mutableStateOf(Color(0xFF939EAC))
private var Accent by mutableStateOf(Color(0xFFB8F36B))
private val AccentArgb: Int get() = Accent.toArgb()
private var Board by mutableStateOf(Color(0xFF10161D))

private fun applyThemePalette(preset: String, accentName: String) {
    ActiveThemePreset = preset.takeIf { it in listOf("Midnight", "AMOLED", "Light") } ?: "Midnight"
    ActiveAccentName = accentName.takeIf { it in listOf("Lime", "Sky", "Violet", "Coral") } ?: "Lime"
    Accent = when (ActiveAccentName) {
        "Sky" -> Color(0xFF69C8FF)
        "Violet" -> Color(0xFFC3A4FF)
        "Coral" -> Color(0xFFFF9F8E)
        else -> Color(0xFFB8F36B)
    }
    when (ActiveThemePreset) {
        "Light" -> {
            Bg = Color(0xFFF5F7F1)
            Panel = Color(0xFFFFFFFF)
            PanelRaised = Color(0xFFEBEFE6)
            PanelActive = Accent.copy(alpha = 0.18f)
            Border = Color(0xFFD8DED3)
            TextMain = Color(0xFF192019)
            Muted = Color(0xFF667266)
            Board = Color(0xFFEDF1E9)
        }
        "AMOLED" -> {
            Bg = Color(0xFF000000)
            Panel = Color(0xFF07090C)
            PanelRaised = Color(0xFF111419)
            PanelActive = Accent.copy(alpha = 0.16f)
            Border = Color(0xFF252B31)
            TextMain = Color(0xFFF4F6F7)
            Muted = Color(0xFF939DA7)
            Board = Color(0xFF030405)
        }
        else -> {
            Bg = Color(0xFF0B0E12)
            Panel = Color(0xFF141A21)
            PanelRaised = Color(0xFF1B232D)
            PanelActive = Accent.copy(alpha = 0.16f)
            Border = Color(0xFF28323D)
            TextMain = Color(0xFFF2F5F7)
            Muted = Color(0xFF939EAC)
            Board = Color(0xFF10161D)
        }
    }
}
private val NoteColors = listOf(
    Color(0xFFE9D98C), Color(0xFFB8DCCB), Color(0xFFC8B9F0), Color(0xFFE7B6AA)
)
private val BrushColors = listOf(
    Pair("Lime", AccentArgb), Pair("White", 0xFFFFFFFF.toInt()),
    Pair("Sky", 0xFF70C9FF.toInt()), Pair("Coral", 0xFFFF9D8C.toInt()),
    Pair("Violet", 0xFFCCB7FF.toInt())
)

private fun colorFromArgb(argb: Int): Color = Color(
    red = ((argb ushr 16) and 0xFF) / 255f,
    green = ((argb ushr 8) and 0xFF) / 255f,
    blue = (argb and 0xFF) / 255f,
    alpha = ((argb ushr 24) and 0xFF) / 255f
)

data class StickyNote(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "New note",
    val x: Float = 24f,
    val y: Float = 24f,
    val colorIndex: Int = 0
)

data class SubTask(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val done: Boolean = false
)

data class Todo(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val done: Boolean = false,
    val priority: String = "Normal",
    val category: String = "General",
    val subtasks: List<SubTask> = emptyList()
)

data class DrawStroke(
    val points: List<Offset>,
    val colorArgb: Int = AccentArgb,
    val width: Float = 5f,
    val eraser: Boolean = false
)

data class SavedDrawing(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Sketch 1",
    val strokes: List<DrawStroke> = emptyList()
)

private data class BackupPayload(
    val notes: List<StickyNote>,
    val todos: List<Todo>,
    val drawings: List<SavedDrawing>,
    val selectedDrawingId: String?
)

private data class SearchHit(
    val id: String,
    val type: String,
    val title: String,
    val detail: String
)

class MainActivity : ComponentActivity() {
    private var requestedTab by mutableStateOf<String?>(null)
    private var intentRevision by mutableIntStateOf(0)
    private var resumeRevision by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val startupPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        applyThemePalette(
            startupPrefs.getString(KEY_THEME_PRESET, "Midnight") ?: "Midnight",
            startupPrefs.getString(KEY_ACCENT_NAME, "Lime") ?: "Lime"
        )
        MotionSetting = startupPrefs.getBoolean(KEY_MOTION_ENABLED, true)
        val systemBarStyle = if (ActiveThemePreset == "Light") {
            SystemBarStyle.light(Bg.toArgb(), Bg.toArgb())
        } else {
            SystemBarStyle.dark(Bg.toArgb())
        }
        enableEdgeToEdge(statusBarStyle = systemBarStyle, navigationBarStyle = systemBarStyle)
        setContent { SlateSpaceApp(initialTab = requestedTab ?: intent.getStringExtra(EXTRA_START_TAB) ?: TAB_HOME, refreshKey = resumeRevision, navigationKey = intentRevision) }
        SlateSpaceWidgetProvider.updateAll(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedTab = intent.getStringExtra(EXTRA_START_TAB)
        intentRevision++
    }

    override fun onResume() {
        super.onResume()
        resumeRevision++
        SlateSpaceWidgetProvider.updateAll(this)
    }
}

@Composable
fun SlateSpaceApp(initialTab: String = TAB_HOME, refreshKey: Int = 0, navigationKey: Int = 0) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            Toast.makeText(context, "Notifications are off. Enable SlateSpace notifications in Android settings to receive task reminders.", Toast.LENGTH_LONG).show()
        }
    }
    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var notes by remember { mutableStateOf(loadNotes(prefs.getString(KEY_NOTES, null))) }
    var todos by remember { mutableStateOf(loadTodos(prefs.getString(KEY_TODOS, null))) }
    var drawings by remember {
        mutableStateOf(loadDrawings(prefs.getString(KEY_DRAWINGS, null), prefs.getString(KEY_LEGACY_STROKES, null)))
    }
    var selectedDrawingId by remember {
        mutableStateOf(prefs.getString(KEY_SELECTED_DRAWING, null) ?: drawings.firstOrNull()?.id.orEmpty())
    }
    var tab by remember { mutableStateOf(initialTab) }
    LaunchedEffect(initialTab, navigationKey) {
        if (initialTab in listOf(TAB_HOME, TAB_SPACE, TAB_TASKS, TAB_DRAW, TAB_SEARCH, TAB_SETTINGS)) tab = initialTab
    }
    LaunchedEffect(refreshKey) {
        // Notification actions can update SharedPreferences while the Activity is backgrounded.
        // Refresh the local UI state whenever Android brings the app back to the foreground.
        notes = loadNotes(prefs.getString(KEY_NOTES, null))
        todos = loadTodos(prefs.getString(KEY_TODOS, null))
        drawings = loadDrawings(prefs.getString(KEY_DRAWINGS, null), prefs.getString(KEY_LEGACY_STROKES, null))
        selectedDrawingId = prefs.getString(KEY_SELECTED_DRAWING, null) ?: drawings.firstOrNull()?.id.orEmpty()
    }
    var themePreset by remember { mutableStateOf(prefs.getString(KEY_THEME_PRESET, "Midnight") ?: "Midnight") }
    var accentName by remember { mutableStateOf(prefs.getString(KEY_ACCENT_NAME, "Lime") ?: "Lime") }
    var hapticsEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_HAPTICS_ENABLED, true)) }
    var animationsEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_MOTION_ENABLED, true)) }
    var setupComplete by remember {
        // Existing installations from editions before the setup screen must not be forced through onboarding.
        val hasLegacyState = prefs.all.keys.any { key ->
            key in setOf(KEY_NOTES, KEY_TODOS, KEY_LEGACY_STROKES, KEY_DRAWINGS, KEY_THEME_PRESET, KEY_ACCENT_NAME, KEY_HAPTICS_ENABLED, KEY_MOTION_ENABLED)
        }
        if (!prefs.contains(KEY_SETUP_COMPLETE) && hasLegacyState) {
            val shouldOfferNotificationPermission = !prefs.contains(KEY_REMINDERS_ENABLED)
            prefs.edit()
                .putBoolean(KEY_SETUP_COMPLETE, true)
                .putBoolean(KEY_MIGRATION_PERMISSION_PROMPT_PENDING, shouldOfferNotificationPermission)
                .apply()
        }
        mutableStateOf(prefs.getBoolean(KEY_SETUP_COMPLETE, false))
    }
    var remindersEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_REMINDERS_ENABLED, true)) }
    var reminderInterval by remember { mutableIntStateOf(prefs.getInt(KEY_REMINDER_INTERVAL, 20)) }
    var quietStart by remember { mutableIntStateOf(prefs.getInt(KEY_QUIET_START, 22)) }
    var quietEnd by remember { mutableIntStateOf(prefs.getInt(KEY_QUIET_END, 7)) }
    LaunchedEffect(setupComplete) {
        if (setupComplete) {
            scheduleTaskReminders(context, remindersEnabled, reminderInterval)
            if (prefs.getBoolean(KEY_MIGRATION_PERMISSION_PROMPT_PENDING, false)) {
                prefs.edit().putBoolean(KEY_MIGRATION_PERMISSION_PROMPT_PENDING, false).apply()
                if (remindersEnabled) requestNotificationPermissionIfNeeded()
            }
        }
    }
    var displayName by remember { mutableStateOf(prefs.getString(KEY_DISPLAY_NAME, "") ?: "") }
    var workspaceMode by remember { mutableStateOf(prefs.getString(KEY_WORKSPACE_MODE, "Auto") ?: "Auto") }
    val haptics = LocalHapticFeedback.current
    fun tapHaptic() { if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
    fun updateThemePreset(value: String) {
        themePreset = value
        prefs.edit().putString(KEY_THEME_PRESET, value).apply()
        applyThemePalette(value, accentName)
        (context as? ComponentActivity)?.let { activity ->
            val barStyle = if (value == "Light") SystemBarStyle.light(Bg.toArgb(), Bg.toArgb()) else SystemBarStyle.dark(Bg.toArgb())
            activity.enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
        }
    }
    fun updateAccent(value: String) {
        accentName = value
        prefs.edit().putString(KEY_ACCENT_NAME, value).apply()
        applyThemePalette(themePreset, value)
    }
    fun updateHaptics(value: Boolean) {
        hapticsEnabled = value
        prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, value).apply()
        if (value) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    fun updateAnimations(value: Boolean) {
        animationsEnabled = value
        MotionSetting = value
        prefs.edit().putBoolean(KEY_MOTION_ENABLED, value).apply()
    }
    fun updateWorkspaceMode(value: String) {
        workspaceMode = value.takeIf { it in listOf("Auto", "Focus", "Explore", "Plan") } ?: "Auto"
        prefs.edit().putString(KEY_WORKSPACE_MODE, workspaceMode).apply()
    }
    var searchQuery by remember { mutableStateOf("") }
    var taskFilter by remember { mutableStateOf("Open") }

    var showQuickCapture by remember { mutableStateOf(false) }
    var showCommandPalette by remember { mutableStateOf(false) }
    val quickCaptureIconRotation by animateFloatAsState(
        targetValue = if (showQuickCapture) 45f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "quick-capture-icon-rotation"
    )
    var showNoteEditor by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<StickyNote?>(null) }
    var showTodoEditor by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<Todo?>(null) }
    var showDrawingEditor by remember { mutableStateOf(false) }
    var drawingNameDraft by remember { mutableStateOf("") }
    var pendingRestore by remember { mutableStateOf<BackupPayload?>(null) }

    var brushArgb by remember { mutableIntStateOf(AccentArgb) }
    var eraserMode by remember { mutableStateOf(false) }
    var brushWidthDp by remember { mutableFloatStateOf(3.5f) }

    fun persistNotes(value: List<StickyNote>) {
        notes = value
        prefs.edit().putString(KEY_NOTES, encodeNotes(value).toString()).apply()
    }

    fun persistTodos(value: List<Todo>) {
        todos = value
        prefs.edit().putString(KEY_TODOS, encodeTodos(value).toString()).apply()
        SlateSpaceWidgetProvider.updateAll(context)
    }

    fun persistDrawings(value: List<SavedDrawing>, wantedSelectedId: String?) {
        drawings = if (value.isEmpty()) listOf(SavedDrawing(name = "Sketch 1")) else value
        val resolved = drawings.firstOrNull { it.id == wantedSelectedId }?.id ?: drawings.first().id
        selectedDrawingId = resolved
        prefs.edit()
            .putString(KEY_DRAWINGS, encodeDrawings(drawings).toString())
            .putString(KEY_SELECTED_DRAWING, resolved)
            .apply()
    }

    fun addNote(text: String) {
        val index = notes.size
        val startX = with(density) { 12.dp.toPx() }
        val startY = with(density) { 12.dp.toPx() }
        val stepX = with(density) { 148.dp.toPx() }
        val stepY = with(density) { 132.dp.toPx() }
        val note = StickyNote(
            text = text.trim(),
            x = startX + (index % 2) * stepX,
            y = startY + ((index / 2) % 5) * stepY,
            colorIndex = index % NoteColors.size
        )
        persistNotes(notes + note)
    }

    fun addTask(text: String, priority: String = "Normal", category: String = "General") {
        if (text.isNotBlank()) persistTodos(todos + Todo(text = text.trim(), priority = priority, category = category.ifBlank { "General" }.trim()))
    }

    fun updateTodo(updated: Todo) {
        persistTodos(if (todos.any { it.id == updated.id }) todos.map { if (it.id == updated.id) updated else it } else todos + updated)
    }

    fun selectedDrawing(): SavedDrawing = drawings.firstOrNull { it.id == selectedDrawingId } ?: drawings.first()

    fun changeDrawingStrokes(updatedStrokes: List<DrawStroke>) {
        val selected = selectedDrawing()
        persistDrawings(drawings.map { if (it.id == selected.id) it.copy(strokes = updatedStrokes) else it }, selected.id)
    }

    fun createDrawing(name: String) {
        val drawing = SavedDrawing(name = name.trim().ifBlank { "Sketch ${drawings.size + 1}" })
        persistDrawings(drawings + drawing, drawing.id)
        tab = TAB_DRAW
    }

    fun backupJson(): String = encodeBackup(notes, todos, drawings, selectedDrawingId).toString(2)

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            try {
                val output = context.contentResolver.openOutputStream(uri) ?: throw IOException("Could not open the backup destination")
                output.use { it.write(backupJson().toByteArray(Charsets.UTF_8)) }
                Toast.makeText(context, "Backup exported successfully", Toast.LENGTH_LONG).show()
            } catch (error: Exception) {
                Toast.makeText(context, "Backup failed: ${error.message ?: "unknown error"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Could not read this file")
                val raw = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
                pendingRestore = decodeBackup(raw)
            } catch (error: Exception) {
                Toast.makeText(context, "Restore failed: ${error.message ?: "invalid backup file"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val premiumColorScheme = if (ActiveThemePreset == "Light") lightColorScheme(
        primary = Accent, onPrimary = Color(0xFF111510), primaryContainer = PanelActive,
        onPrimaryContainer = TextMain, secondary = Muted, onSecondary = Color.White,
        secondaryContainer = PanelRaised, onSecondaryContainer = TextMain,
        background = Bg, onBackground = TextMain, surface = Panel, onSurface = TextMain,
        surfaceVariant = PanelRaised, onSurfaceVariant = Muted, surfaceTint = Accent,
        outline = Border, outlineVariant = Border.copy(alpha = 0.72f),
        error = Color(0xFFB3261E), onError = Color.White
    ) else darkColorScheme(
        primary = Accent, onPrimary = Color(0xFF111510), primaryContainer = PanelActive,
        onPrimaryContainer = Accent, secondary = Muted, onSecondary = Bg,
        secondaryContainer = PanelRaised, onSecondaryContainer = TextMain,
        background = Bg, onBackground = TextMain, surface = Panel, onSurface = TextMain,
        surfaceVariant = PanelRaised, onSurfaceVariant = Muted, surfaceTint = Accent,
        outline = Border, outlineVariant = Border.copy(alpha = 0.72f),
        error = Color(0xFFFF8D8D), onError = Bg
    )
    if (!setupComplete) {
        MaterialTheme(colorScheme = premiumColorScheme) {
        PersonalSetupScreen(
            displayName = displayName,
            onDisplayName = { displayName = it },
            remindersEnabled = remindersEnabled,
            onRemindersEnabled = { remindersEnabled = it },
            reminderInterval = reminderInterval,
            onReminderInterval = { reminderInterval = it },
            quietStart = quietStart,
            onQuietStart = { quietStart = it },
            quietEnd = quietEnd,
            onQuietEnd = { quietEnd = it },
            themePreset = themePreset,
            onThemePreset = { updateThemePreset(it) },
            accentName = accentName,
            onAccentName = { updateAccent(it) },
            onFinish = {
                prefs.edit()
                    .putBoolean(KEY_SETUP_COMPLETE, true)
                    .putString(KEY_DISPLAY_NAME, displayName.trim())
                    .putBoolean(KEY_REMINDERS_ENABLED, remindersEnabled)
                    .putInt(KEY_REMINDER_INTERVAL, reminderInterval)
                    .putInt(KEY_QUIET_START, quietStart)
                    .putInt(KEY_QUIET_END, quietEnd)
                    .apply()
                scheduleTaskReminders(context, remindersEnabled, reminderInterval)
                setupComplete = true
                if (remindersEnabled) requestNotificationPermissionIfNeeded()
            }
        )
        }
        return
    }

    MaterialTheme(colorScheme = premiumColorScheme) {
        Scaffold(
            containerColor = Bg,
            floatingActionButton = {
                AnimatedVisibility(
                    visible = tab != TAB_DRAW && tab != TAB_SETTINGS,
                    enter = if (animationsEnabled) fadeIn(tween(180)) + scaleIn(initialScale = 0.88f, animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)) else fadeIn(tween(1)),
                    exit = if (animationsEnabled) fadeOut(tween(120)) + scaleOut(targetScale = 0.92f, animationSpec = tween(120)) else fadeOut(tween(1))
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { tapHaptic(); showQuickCapture = true },
                        icon = {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.graphicsLayer { rotationZ = quickCaptureIconRotation }
                            )
                        },
                        text = { Text("Quick capture", fontWeight = FontWeight.SemiBold) },
                        containerColor = Accent,
                        contentColor = Bg,
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Panel, contentColor = TextMain, tonalElevation = 0.dp) {
                    val items = listOf(
                        Triple(TAB_HOME, Icons.Default.Home, "Home"),
                        Triple(TAB_SPACE, Icons.Default.GridView, "Space"),
                        Triple(TAB_TASKS, Icons.Default.TaskAlt, "Tasks"),
                        Triple(TAB_DRAW, Icons.Default.Brush, "Draw"),
                        Triple(TAB_SEARCH, Icons.Default.Search, "Search")
                    )
                    items.forEach { (key, icon, label) ->
                        val isSelected = tab == key
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.10f else 1f,
                            animationSpec = if (animationsEnabled) spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium) else androidx.compose.animation.core.snap(),
                            label = "nav-icon-scale-$key"
                        )
                        val iconTint by animateColorAsState(
                            targetValue = if (isSelected) Accent else Muted,
                            animationSpec = if (animationsEnabled) tween(180) else androidx.compose.animation.core.snap(),
                            label = "nav-icon-color-$key"
                        )
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { tapHaptic(); tab = key },
                            icon = {
                                Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                                    Icon(
                                        icon,
                                        contentDescription = label,
                                        tint = iconTint,
                                        modifier = Modifier.size(22.dp).graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                    )
                                }
                            },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent,
                                selectedTextColor = Accent,
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted,
                                indicatorColor = PanelActive
                            )
                        )
                    }
                }
            }
        ) { insets ->
            Column(
                Modifier.fillMaxSize()
                    .background(GraphicsBrush.verticalGradient(listOf(Panel, Bg)))
                    .padding(insets)
            ) {
                BrandHeader(
                    noteCount = notes.size,
                    openTaskCount = todos.count { !it.done },
                    onSearch = { tapHaptic(); tab = TAB_SEARCH },
                    onSettings = { tapHaptic(); tab = if (tab == TAB_SETTINGS) TAB_HOME else TAB_SETTINGS },
                    settingsActive = tab == TAB_SETTINGS
                )
                AnimatedContent(
                    targetState = tab,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    transitionSpec = {
                        if (animationsEnabled) {
                            (fadeIn(animationSpec = tween(230, easing = FastOutSlowInEasing)) +
                                slideInHorizontally(
                                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                                    initialOffsetX = { width -> width / 18 }
                                )) togetherWith
                                (fadeOut(animationSpec = tween(120)) +
                                    slideOutHorizontally(
                                        animationSpec = tween(160),
                                        targetOffsetX = { width -> -width / 28 }
                                    ))
                        } else fadeIn(tween(1)) togetherWith fadeOut(tween(1))
                    },
                    label = "slatespace-screen-transition"
                ) { currentTab ->
                    when (currentTab) {
                    TAB_HOME -> CommandCenterHome(
                        notes = notes,
                        todos = todos,
                        drawings = drawings,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                        onCommand = { showCommandPalette = true },
                        onNewNote = { showNoteEditor = true; editingNote = null },
                        onNewTask = { showTodoEditor = true; editingTodo = null },
                        onOpenSpace = { tab = TAB_SPACE },
                        onOpenTasks = { tab = TAB_TASKS },
                        onOpenDraw = { tab = TAB_DRAW },
                        onEditNote = { note -> editingNote = note; showNoteEditor = true },
                        onEditTask = { task -> editingTodo = task; showTodoEditor = true },
                        onToggleTask = { task, checked -> updateTodo(task.copy(done = checked)) }
                    )
                    TAB_SPACE -> WorkspaceScreen(
                        notes = notes,
                        onAdd = { showNoteEditor = true; editingNote = null },
                        onEdit = { note -> editingNote = note; showNoteEditor = true },
                        onMove = { id, deltaX, deltaY, maxX, maxY ->
                            notes = notes.map { note ->
                                if (note.id == id) note.copy(
                                    x = (note.x + deltaX).coerceIn(0f, maxX.coerceAtLeast(0f)),
                                    y = (note.y + deltaY).coerceIn(0f, maxY.coerceAtLeast(0f))
                                ) else note
                            }
                        },
                        onMoveFinished = {
                            // Writing JSON and SharedPreferences on every pointer event caused drag jank.
                            // Persist once at gesture end; cancellation also commits the final position.
                            prefs.edit().putString(KEY_NOTES, encodeNotes(notes).toString()).apply()
                        },
                        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)
                    )
                    TAB_TASKS -> TasksScreen(
                        todos = todos,
                        filter = taskFilter,
                        onFilter = { taskFilter = it },
                        onAdd = { showTodoEditor = true; editingTodo = null },
                        onEdit = { todo -> editingTodo = todo; showTodoEditor = true },
                        onDelete = { todo -> persistTodos(todos.filterNot { it.id == todo.id }) },
                        onToggle = { todo, checked -> updateTodo(todo.copy(done = checked)) },
                        onToggleSubtask = { todo, subtask, checked ->
                            updateTodo(todo.copy(subtasks = todo.subtasks.map { if (it.id == subtask.id) it.copy(done = checked) else it }))
                        },
                        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)
                    )
                    TAB_DRAW -> DrawingsScreen(
                        drawings = drawings,
                        selectedDrawingId = selectedDrawingId,
                        brushArgb = brushArgb,
                        eraserMode = eraserMode,
                        brushWidthDp = brushWidthDp,
                        onSelectDrawing = { id -> persistDrawings(drawings, id) },
                        onNewDrawing = { drawingNameDraft = ""; showDrawingEditor = true },
                        onBrush = { brushArgb = it; eraserMode = false },
                        onEraser = { eraserMode = !eraserMode },
                        onBrushWidth = { brushWidthDp = it },
                        onUndo = { val active = selectedDrawing(); if (active.strokes.isNotEmpty()) changeDrawingStrokes(active.strokes.dropLast(1)) },
                        onClear = { changeDrawingStrokes(emptyList()) },
                        onStrokesChanged = { changeDrawingStrokes(it) },
                        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)
                    )
                    TAB_SEARCH -> SearchScreen(
                        query = searchQuery,
                        onQuery = { searchQuery = it },
                        notes = notes,
                        todos = todos,
                        drawings = drawings,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                        onOpenNote = { id -> notes.firstOrNull { it.id == id }?.let { editingNote = it; showNoteEditor = true } },
                        onOpenTask = { id -> todos.firstOrNull { it.id == id }?.let { editingTodo = it; showTodoEditor = true } },
                        onOpenDrawing = { id -> selectedDrawingId = id; persistDrawings(drawings, id); tab = TAB_DRAW }
                    )
                    TAB_SETTINGS -> SettingsScreen(
                        themePreset = themePreset,
                        accentName = accentName,
                        hapticsEnabled = hapticsEnabled,
                        animationsEnabled = animationsEnabled,
                        remindersEnabled = remindersEnabled,
                        reminderInterval = reminderInterval,
                        quietStart = quietStart,
                        quietEnd = quietEnd,
                        onRemindersEnabledChange = { value ->
                            remindersEnabled = value
                            prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, value).apply()
                            if (value) requestNotificationPermissionIfNeeded()
                            scheduleTaskReminders(context, value, reminderInterval)
                        },
                        onReminderIntervalChange = { value ->
                            reminderInterval = value
                            prefs.edit().putInt(KEY_REMINDER_INTERVAL, value).apply()
                            scheduleTaskReminders(context, remindersEnabled, value)
                        },
                        onQuietHoursChange = { start, end ->
                            quietStart = start; quietEnd = end
                            prefs.edit().putInt(KEY_QUIET_START, start).putInt(KEY_QUIET_END, end).apply()
                        },
                        onThemePresetChange = { updateThemePreset(it) },
                        onAccentChange = { updateAccent(it) },
                        onHapticsChange = { updateHaptics(it) },
                        onAnimationsChange = { updateAnimations(it) },
                        onBackup = { backupLauncher.launch("SlateSpace-backup.json") },
                        onRestore = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)
                    )
                    }
                }
            }
        }
    }

    if (showCommandPalette) {
        CommandPaletteDialog(
            onDismiss = { showCommandPalette = false },
            onSearch = { query ->
                searchQuery = query
                showCommandPalette = false
                tab = TAB_SEARCH
            },
            onCreateNote = { text ->
                addNote(text)
                showCommandPalette = false
                tab = TAB_SPACE
            },
            onCreateTask = { text ->
                addTask(text)
                showCommandPalette = false
                tab = TAB_TASKS
            }
        )
    }

    if (showQuickCapture) {
        QuickCaptureDialog(
            onDismiss = { showQuickCapture = false },
            onCreateNote = { text -> addNote(text); showQuickCapture = false; tab = TAB_SPACE },
            onCreateTask = { text, priority, category -> addTask(text, priority, category); showQuickCapture = false; tab = TAB_TASKS }
        )
    }

    if (showNoteEditor) {
        NoteEditorDialog(
            note = editingNote,
            onDismiss = { showNoteEditor = false; editingNote = null },
            onDelete = { note -> persistNotes(notes.filterNot { it.id == note.id }); showNoteEditor = false; editingNote = null },
            onSave = { updated ->
                if (updated.text.isNotBlank()) {
                    persistNotes(if (notes.any { it.id == updated.id }) notes.map { if (it.id == updated.id) updated else it } else notes + updated)
                }
                showNoteEditor = false; editingNote = null
            }
        )
    }

    if (showTodoEditor) {
        TodoEditorDialog(
            todo = editingTodo,
            onDismiss = { showTodoEditor = false; editingTodo = null },
            onSave = { updated -> updateTodo(updated); showTodoEditor = false; editingTodo = null }
        )
    }

    if (showDrawingEditor) {
        SimpleInputDialog(
            title = "New drawing",
            subtitle = "Keep each sketch on its own canvas.",
            value = drawingNameDraft,
            placeholder = "e.g. Physics diagram",
            onValueChange = { drawingNameDraft = it },
            onDismiss = { showDrawingEditor = false },
            onSave = { createDrawing(drawingNameDraft); showDrawingEditor = false }
        )
    }

    pendingRestore?.let { payload ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restore this backup?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will replace the current notes, tasks, and drawings with ${payload.notes.size} notes, ${payload.todos.size} tasks, and ${payload.drawings.size} drawings from the selected file.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        persistNotes(payload.notes)
                        persistTodos(payload.todos)
                        persistDrawings(payload.drawings, payload.selectedDrawingId)
                        pendingRestore = null
                        Toast.makeText(context, "Backup restored", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)
                ) { Text("Restore backup") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancel", color = Muted) } },
            containerColor = Panel,
            titleContentColor = TextMain,
            textContentColor = Muted
        )
    }
}

@Composable
private fun PersonalSetupScreen(
    displayName: String, onDisplayName: (String) -> Unit,
    remindersEnabled: Boolean, onRemindersEnabled: (Boolean) -> Unit,
    reminderInterval: Int, onReminderInterval: (Int) -> Unit,
    quietStart: Int, onQuietStart: (Int) -> Unit,
    quietEnd: Int, onQuietEnd: (Int) -> Unit,
    themePreset: String, onThemePreset: (String) -> Unit,
    accentName: String, onAccentName: (String) -> Unit,
    onFinish: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(GraphicsBrush.verticalGradient(listOf(Panel, Bg))).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text("SLATESPACE  /  SETUP", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Make it yours.", color = TextMain, fontSize = 34.sp, lineHeight = 39.sp, fontWeight = FontWeight.Bold)
        Text("A few choices now. You can change reminders and appearance later in Settings.", color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel).border(1.dp, Border, RoundedCornerShape(22.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("YOUR SPACE", color = Muted, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
            Text("What should we call you? (optional)", color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(value = displayName, onValueChange = onDisplayName, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Your name") })
            Text("Appearance", color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Midnight", "AMOLED", "Light").forEach { option -> FilterChip(selected = themePreset == option, onClick = { onThemePreset(option) }, label = { Text(option, fontSize = 12.sp) }, modifier = Modifier.weight(1f)) }
            }
            Text("Accent", color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Lime" to Color(0xFFB8F36B), "Sky" to Color(0xFF69C8FF), "Violet" to Color(0xFFC3A4FF), "Coral" to Color(0xFFFF9F8E)).forEach { (name, color) ->
                    Column(Modifier.weight(1f).clickable { onAccentName(name) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(38.dp).clip(CircleShape).background(color).border(if (accentName == name) 3.dp else 1.dp, if (accentName == name) TextMain else Border, CircleShape), contentAlignment = Alignment.Center) { if (accentName == name) Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = Color(0xFF111510), modifier = Modifier.size(17.dp)) }
                        Text(name, color = if (accentName == name) TextMain else Muted, fontSize = 10.sp)
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel).border(1.dp, Border, RoundedCornerShape(22.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("GENTLE FOCUS REMINDERS", color = Muted, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
            Text("Nudge me if tasks are still unfinished", color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("Notifications repeat about every 15–20 minutes while unfinished tasks remain. Android can delay them to save battery.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
            PreferenceSwitchRow(title = "Enable reminders", subtitle = "You can turn these off whenever you want.", checked = remindersEnabled, onCheckedChange = onRemindersEnabled)
            if (remindersEnabled) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 20).forEach { mins -> FilterChip(selected = reminderInterval == mins, onClick = { onReminderInterval(mins) }, label = { Text("Every $mins min") }, modifier = Modifier.weight(1f)) }
                }
                Text("Sleep schedule — no reminders overnight", color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Quiet hours use your phone's local time.", color = Muted, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SLEEP", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = { onQuietStart((quietStart + 23) % 24) }) { Text("−") }; Text("%02d:00".format(quietStart), color = TextMain); TextButton(onClick = { onQuietStart((quietStart + 1) % 24) }) { Text("+") } }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("WAKE", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = { onQuietEnd((quietEnd + 23) % 24) }) { Text("−") }; Text("%02d:00".format(quietEnd), color = TextMain); TextButton(onClick = { onQuietEnd((quietEnd + 1) % 24) }) { Text("+") } }
                    }
                }
            }
        }
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF111510)), shape = RoundedCornerShape(17.dp)) { Text("Finish setup  →", fontWeight = FontWeight.Bold) }
        Text("You stay in control. Reminders and settings can be changed later.", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SettingsScreen(
    themePreset: String,
    accentName: String,
    hapticsEnabled: Boolean,
    animationsEnabled: Boolean,
    remindersEnabled: Boolean,
    reminderInterval: Int,
    quietStart: Int,
    quietEnd: Int,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onReminderIntervalChange: (Int) -> Unit,
    onQuietHoursChange: (Int, Int) -> Unit,
    onThemePresetChange: (String) -> Unit,
    onAccentChange: (String) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onAnimationsChange: (Boolean) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(bottom = 106.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(3.dp))
        Text("MAKE IT YOURS", color = Muted, fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
        Text("Settings", color = TextMain, fontSize = 29.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
        Text("Tune SlateSpace to your eyes, hands, and workflow.", color = Muted, fontSize = 13.sp, lineHeight = 19.sp)

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel).border(1.dp, Border, RoundedCornerShape(22.dp)).padding(16.dp)) {
            Text("APPEARANCE", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("Colour mode", color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("Choose the surface style that feels right.", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Midnight", "AMOLED", "Light").forEach { option ->
                    FilterChip(
                        selected = themePreset == option,
                        onClick = { onThemePresetChange(option) },
                        label = { Text(option, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Accent colour", color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(
                    "Lime" to Color(0xFFB8F36B),
                    "Sky" to Color(0xFF69C8FF),
                    "Violet" to Color(0xFFC3A4FF),
                    "Coral" to Color(0xFFFF9F8E)
                ).forEach { (name, color) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).clickable { onAccentChange(name) }
                                .border(if (accentName == name) 3.dp else 1.dp, if (accentName == name) TextMain else Border, CircleShape)
                                .padding(4.dp).clip(CircleShape).background(color),
                            contentAlignment = Alignment.Center
                        ) {
                            if (accentName == name) Icon(Icons.Default.CheckCircle, contentDescription = "$name selected", tint = Color(0xFF151A14), modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(name, color = if (accentName == name) TextMain else Muted, fontSize = 11.sp, fontWeight = if (accentName == name) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel).border(1.dp, Border, RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 5.dp)) {
            Text("INTERACTION", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            PreferenceSwitchRow(
                title = "Haptic feedback",
                subtitle = "Small tactile response to important taps.",
                checked = hapticsEnabled,
                onCheckedChange = onHapticsChange
            )
            Divider(color = Border.copy(alpha = 0.65f))
            PreferenceSwitchRow(
                title = "Motion and transitions",
                subtitle = "Use subtle movement between screens and cards.",
                checked = animationsEnabled,
                onCheckedChange = onAnimationsChange
            )
        }

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel).border(1.dp, Border, RoundedCornerShape(22.dp)).padding(16.dp)) {
            Text("FOCUS REMINDERS", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            PreferenceSwitchRow(
                title = "Nudge me about unfinished tasks",
                subtitle = "A gentle notification when work is still open. Android may delay reminders to save battery.",
                checked = remindersEnabled,
                onCheckedChange = onRemindersEnabledChange
            )
            if (remindersEnabled) {
                Text("Reminder interval", color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 20).forEach { mins ->
                        FilterChip(selected = reminderInterval == mins, onClick = { onReminderIntervalChange(mins) }, label = { Text("Every $mins min") }, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Quiet hours", color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("No reminders during this window (24-hour clock).", color = Muted, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Sleep starts", color = Muted, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onQuietHoursChange((quietStart + 23) % 24, quietEnd) }) { Text("−") }
                            Text("%02d:00".format(quietStart), color = TextMain, fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { onQuietHoursChange((quietStart + 1) % 24, quietEnd) }) { Text("+") }
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Wake up", color = Muted, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onQuietHoursChange(quietStart, (quietEnd + 23) % 24) }) { Text("−") }
                            Text("%02d:00".format(quietEnd), color = TextMain, fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { onQuietHoursChange(quietStart, (quietEnd + 1) % 24) }) { Text("+") }
                        }
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel).border(1.dp, Border, RoundedCornerShape(22.dp)).padding(16.dp)) {
            Text("YOUR DATA", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Backups that you control", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("Export notes, tasks, and drawings to a JSON file, or restore a previous backup.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onBackup, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF111510))) { Text("Export backup") }
                androidx.compose.material3.OutlinedButton(onClick = onRestore, modifier = Modifier.weight(1f)) { Text("Restore") }
            }
        }

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(PanelRaised).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.GridView, contentDescription = null, tint = Accent)
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text("Home-screen task widget", color = TextMain, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("See open tasks without opening the app.", color = Muted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("To add it: long-press an empty area of your home screen → Widgets → SlateSpace → Tasks.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
        Text("SLATESPACE 2.4 · ADAPTIVE WORKSPACE", color = Muted, fontSize = 10.sp, letterSpacing = 1.4.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp))
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 72.dp).padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun BrandHeader(noteCount: Int, openTaskCount: Int, onSearch: () -> Unit, onSettings: () -> Unit, settingsActive: Boolean) {
    val animatedNoteCount by animateIntAsState(noteCount, animationSpec = tween(320), label = "header-note-count")
    val animatedOpenTasks by animateIntAsState(openTaskCount, animationSpec = tween(320), label = "header-open-task-count")
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(15.dp))
                .background(GraphicsBrush.linearGradient(listOf(PanelActive, PanelRaised, Panel)))
                .border(1.dp, Border.copy(alpha = 0.9f), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.NoteAlt, contentDescription = null, tint = Accent, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SLATE", color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Text("SPACE", color = Accent, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            }
            Text("Plan · Note · Create", color = Muted, fontSize = 11.sp, letterSpacing = 0.4.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("$animatedNoteCount NOTES", color = Muted, fontSize = 9.sp, letterSpacing = 0.8.sp)
            Text("$animatedOpenTasks OPEN", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onSearch) { Icon(Icons.Default.Search, contentDescription = "Search", tint = Muted) }
        IconButton(onClick = onSettings) {
            val gearTint by animateColorAsState(
                targetValue = if (settingsActive) Accent else Muted,
                animationSpec = if (MotionSetting) tween(180) else androidx.compose.animation.core.snap(),
                label = "settings-icon-color"
            )
            Icon(Icons.Default.Settings, contentDescription = if (settingsActive) "Close settings" else "Settings", tint = gearTint)
        }
    }
}

@Composable
private fun CommandCenterHome(
    notes: List<StickyNote>,
    todos: List<Todo>,
    drawings: List<SavedDrawing>,
    modifier: Modifier = Modifier,
    onCommand: () -> Unit,
    onNewNote: () -> Unit,
    onNewTask: () -> Unit,
    onOpenSpace: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenDraw: () -> Unit,
    onEditNote: (StickyNote) -> Unit,
    onEditTask: (Todo) -> Unit,
    onToggleTask: (Todo, Boolean) -> Unit
) {
    val openTasks = remember(todos) {
        todos.filterNot { it.done }
            .sortedWith(compareBy<Todo> { priorityRank(it.priority) }.thenBy { it.text.lowercase() })
    }
    val nextTask = openTasks.firstOrNull()
    val recentNotes = notes.takeLast(3).reversed()
    Column(modifier.verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("YOUR DESK", color = Muted, fontSize = 10.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text("Make room to think.", color = TextMain, fontSize = 29.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text("Capture first. Organize as you go.", color = Muted, fontSize = 12.sp)

        Spacer(Modifier.height(17.dp))
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
                .clip(RoundedCornerShape(17.dp)).background(Panel)
                .border(1.dp, Border, RoundedCornerShape(17.dp))
                .clickable(onClick = onCommand).padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Accent, modifier = Modifier.size(21.dp))
            Column(Modifier.weight(1f)) {
                Text("Search, capture, or run a command", color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Try “task: revise physics” or “note: an idea”", color = Muted, fontSize = 10.sp)
            }
            Icon(Icons.Default.Add, contentDescription = "Open commands", tint = Muted, modifier = Modifier.size(19.dp))
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            androidx.compose.material3.OutlinedButton(
                onClick = onNewNote, modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 11.dp)
            ) {
                Icon(Icons.Default.NoteAlt, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp)); Text("Capture thought", fontSize = 12.sp)
            }
            Button(
                onClick = onNewTask, modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 11.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp)); Text("Add task", fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("NEXT ACTION", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text("Pick up where you left off", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onOpenTasks) { Text("All tasks", color = Accent, fontSize = 12.sp) }
        }
        Spacer(Modifier.height(8.dp))
        if (nextTask == null) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(Panel)
                    .border(1.dp, Border, RoundedCornerShape(19.dp)).padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(PanelActive), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Accent, modifier = Modifier.size(21.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Your task list is clear.", color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(3.dp))
                        Text("Catch the next thing when it arrives.", color = Muted, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onNewTask, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)) { Text("Create a task") }
            }
        } else {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp))
                    .background(GraphicsBrush.linearGradient(listOf(PanelActive.copy(alpha = 0.52f), Panel, PanelRaised)))
                    .border(1.dp, Accent.copy(alpha = 0.22f), RoundedCornerShape(19.dp)).padding(15.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${nextTask.priority.uppercase()} PRIORITY", color = priorityColor(nextTask.priority), fontSize = 9.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.weight(1f))
                    Text(nextTask.category.ifBlank { "General" }, color = Muted, fontSize = 10.sp, maxLines = 1)
                }
                Spacer(Modifier.height(8.dp))
                Text(nextTask.text, color = TextMain, fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
                if (nextTask.subtasks.isNotEmpty()) {
                    Spacer(Modifier.height(5.dp))
                    Text("${nextTask.subtasks.count { it.done }} of ${nextTask.subtasks.size} steps done", color = Muted, fontSize = 11.sp)
                }
                Spacer(Modifier.height(13.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onEditTask(nextTask) }, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)) {
                        Text("Open task", fontWeight = FontWeight.SemiBold)
                    }
                    androidx.compose.material3.OutlinedButton(onClick = { onToggleTask(nextTask, true) }) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp)); Text("Mark done")
                    }
                }
            }
        }

        Spacer(Modifier.height(19.dp))
        Text("YOUR SPACE", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            DeskOverviewCard(
                title = "Canvas", value = "${notes.size}", detail = if (notes.size == 1) "thought saved" else "thoughts saved",
                icon = Icons.Default.GridView, modifier = Modifier.weight(1f), onClick = onOpenSpace
            )
            DeskOverviewCard(
                title = "Tasks", value = "${openTasks.size}", detail = if (openTasks.size == 1) "task open" else "tasks open",
                icon = Icons.Default.TaskAlt, modifier = Modifier.weight(1f), onClick = onOpenTasks
            )
            DeskOverviewCard(
                title = "Sketches", value = "${drawings.size}", detail = "saved", icon = Icons.Default.Brush,
                modifier = Modifier.weight(1f), onClick = onOpenDraw
            )
        }

        Spacer(Modifier.height(21.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("RECENT THINKING", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text("Ideas you've captured", color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onOpenSpace) { Text("Open desk", color = Accent, fontSize = 12.sp) }
        }
        Spacer(Modifier.height(7.dp))
        if (recentNotes.isEmpty()) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Panel).border(1.dp, Border, RoundedCornerShape(17.dp)).padding(16.dp)) {
                Text("No notes yet. Capture a thought now; decide where it belongs later.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
            }
        } else {
            recentNotes.forEach { note ->
                CompactNoteRow(note, onClick = { onEditNote(note) })
                Spacer(Modifier.height(7.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DeskOverviewCard(
    title: String,
    value: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Panel).border(1.dp, Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(19.dp))
        Text(value, color = TextMain, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text(title, color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(detail, color = Muted, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
private fun CommandPaletteDialog(
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit,
    onCreateNote: (String) -> Unit,
    onCreateTask: (String) -> Unit
) {
    var value by remember { mutableStateOf("") }
    val trimmed = value.trim()
    val lower = trimmed.lowercase()
    val mode = when {
        lower.startsWith("note:") || lower.startsWith("/note ") -> "note"
        lower.startsWith("task:") || lower.startsWith("/task ") -> "task"
        lower.startsWith("search:") || lower.startsWith("/search ") -> "search"
        else -> ""
    }
    val payload = when (mode) {
        "note" -> if (lower.startsWith("note:")) trimmed.substringAfter(':').trim() else trimmed.substringAfter(' ').trim()
        "task" -> if (lower.startsWith("task:")) trimmed.substringAfter(':').trim() else trimmed.substringAfter(' ').trim()
        "search" -> if (lower.startsWith("search:")) trimmed.substringAfter(':').trim() else trimmed.substringAfter(' ').trim()
        else -> trimmed
    }
    val canCreateNote = trimmed.isNotBlank() && (mode.isEmpty() || mode == "note") && payload.isNotBlank()
    val canCreateTask = trimmed.isNotBlank() && (mode.isEmpty() || mode == "task") && payload.isNotBlank()
    val primaryLabel = when (mode) {
        "note" -> "Save note"
        "task" -> "Add task"
        else -> "Search"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Command bar", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Search your work or act without leaving the desk. Commands are processed locally.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Type a command or thought") },
                    placeholder = { Text("task: revise physics") },
                    minLines = 1,
                    maxLines = 3,
                    singleLine = false
                )
                Text("Prefixes:  task:   note:   search:", color = Muted, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { if (canCreateNote) onCreateNote(payload) },
                        modifier = Modifier.weight(1f), enabled = canCreateNote,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 9.dp)
                    ) { Icon(Icons.Default.NoteAlt, contentDescription = null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text("Save note", fontSize = 11.sp) }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { if (canCreateTask) onCreateTask(payload) },
                        modifier = Modifier.weight(1f), enabled = canCreateTask,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 9.dp)
                    ) { Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text("Add task", fontSize = 11.sp) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (mode) {
                        "note" -> if (payload.isNotBlank()) onCreateNote(payload)
                        "task" -> if (payload.isNotBlank()) onCreateTask(payload)
                        else -> if (payload.isNotBlank()) onSearch(payload)
                    }
                },
                enabled = payload.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)
            ) { Icon(if (mode == "task") Icons.Default.TaskAlt else if (mode == "note") Icons.Default.NoteAlt else Icons.Default.Search, contentDescription = null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text(primaryLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Muted) } },
        containerColor = Panel,
        titleContentColor = TextMain,
        textContentColor = TextMain
    )
}

@Composable
private fun HomeScreen(
    notes: List<StickyNote>,
    todos: List<Todo>,
    drawings: List<SavedDrawing>,
    workspaceMode: String,
    onWorkspaceModeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onNewNote: () -> Unit,
    onNewTask: () -> Unit,
    onOpenSpace: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenDraw: () -> Unit,
    onOpenSearch: () -> Unit,
    onEditNote: (StickyNote) -> Unit,
    onEditTask: (Todo) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit
) {
    val openTasks = todos.filterNot { it.done }
    val automaticMode = when {
        openTasks.size >= 5 -> "Focus"
        notes.isNotEmpty() && notes.size >= maxOf(3, openTasks.size * 2) -> "Explore"
        else -> "Plan"
    }
    val effectiveMode = if (workspaceMode == "Auto") automaticMode else workspaceMode
    val suggestedTask = openTasks.maxByOrNull { task ->
        val priorityScore = when (task.priority.lowercase()) {
            "urgent" -> 4
            "high" -> 3
            "normal" -> 2
            "low" -> 1
            else -> 0
        }
        priorityScore * 100 + task.subtasks.count { !it.done }
    }
    val doneCount = todos.count { it.done }
    val progress = if (todos.isEmpty()) 0f else doneCount.toFloat() / todos.size.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "daily-progress"
    )
    val animatedDoneCount by animateIntAsState(doneCount, tween(320), label = "daily-completed-count")
    val animatedTodoCount by animateIntAsState(todos.size, tween(320), label = "daily-total-count")
    Column(modifier.verticalScroll(rememberScrollState()).padding(bottom = 90.dp)) {
        Spacer(Modifier.height(8.dp))
        MotionEntry(delayMillis = 0L) {
            Column {
                Text("YOUR SPACE,", color = Muted, fontSize = 11.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
                Text("clear your mind.", color = TextMain, fontSize = 30.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text("One workspace. Adapted to what you need.", color = Muted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(15.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel)
                .border(1.dp, Border, RoundedCornerShape(20.dp)).padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("WORKSPACE MODE", color = Muted, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (workspaceMode == "Auto") "Auto · leaning toward $effectiveMode" else "$effectiveMode · chosen by you",
                        color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                }
                if (workspaceMode == "Auto") {
                    Text("AUTO", color = Accent, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
            }
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Auto", "Focus", "Explore", "Plan").forEach { mode ->
                    FilterChip(
                        selected = workspaceMode == mode,
                        onClick = { onWorkspaceModeChange(mode) },
                        label = { Text(mode, fontSize = 11.sp, maxLines = 1) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (workspaceMode == "Auto") {
                val reason = when (effectiveMode) {
                    "Focus" -> "You have ${openTasks.size} unfinished tasks. SlateSpace is keeping the next action prominent."
                    "Explore" -> "You have a lot of loose notes. SlateSpace is putting idea capture and the canvas first."
                    else -> "Your workspace is balanced. SlateSpace is keeping progress and planning visible."
                }
                Text(reason, color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        when (effectiveMode) {
            "Focus" -> MotionEntry(delayMillis = 50L) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp))
                        .background(GraphicsBrush.linearGradient(listOf(PanelActive.copy(alpha = 0.68f), Panel, PanelRaised)))
                        .border(1.dp, Accent.copy(alpha = 0.22f), RoundedCornerShape(23.dp)).padding(17.dp)
                ) {
                    Text("YOUR NEXT MOVE", color = Accent, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    if (suggestedTask == null) {
                        Text("Nothing competing for your attention.", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("Add a task when something needs doing. For now, your space is clear.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = onNewTask, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)) { Text("Add a task") }
                    } else {
                        Text(suggestedTask.text, color = TextMain, fontSize = 21.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text("${suggestedTask.priority} priority · ${suggestedTask.category}", color = Muted, fontSize = 12.sp)
                        if (suggestedTask.subtasks.isNotEmpty()) {
                            Text("${suggestedTask.subtasks.count { it.done }} of ${suggestedTask.subtasks.size} steps complete", color = Muted, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(11.dp))
                        Text("Suggested from open status and priority. You stay in control.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { onEditTask(suggestedTask) }, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)) { Text("Open this task") }
                    }
                }
            }
            "Explore" -> MotionEntry(delayMillis = 50L) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp)).background(Panel)
                        .border(1.dp, Color(0xFF8CCFFF).copy(alpha = 0.35f), RoundedCornerShape(23.dp)).padding(16.dp)
                ) {
                    Text("IDEAS FIRST", color = Color(0xFF8CCFFF), fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text("Catch the thought before organizing it.", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text("Start with a note, spread ideas across your space, or sketch what words can't explain yet.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onNewNote, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)) { Text("Capture idea") }
                        androidx.compose.material3.OutlinedButton(onClick = onOpenSpace, modifier = Modifier.weight(1f)) { Text("Open canvas") }
                    }
                }
            }
            else -> MotionEntry(delayMillis = 50L) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp))
                        .background(GraphicsBrush.linearGradient(listOf(PanelActive.copy(alpha = 0.42f), Panel, PanelRaised)))
                        .border(1.dp, Accent.copy(alpha = 0.16f), RoundedCornerShape(23.dp)).padding(17.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("YOUR MOMENTUM", color = Muted, fontSize = 10.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(5.dp))
                            Text("$doneCount of ${todos.size} tasks done", color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("${(animatedProgress * 100).roundToInt()}%", color = Accent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.height(13.dp))
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(PanelRaised)) {
                        Box(Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().clip(CircleShape)
                            .background(GraphicsBrush.horizontalGradient(listOf(Color(0xFF8DD848), Accent, Color(0xFFD7FF9E))))
                        )
                    }
                    Spacer(Modifier.height(9.dp))
                    Text("Plan the work, then let the workspace get out of the way.", color = Muted, fontSize = 11.sp)
                }
            }
        }
        if (effectiveMode != "Focus") {
            Spacer(Modifier.height(14.dp))
            MotionEntry(delayMillis = 90L) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCard("OPEN TASKS", "${openTasks.size}", "one thing at a time", Modifier.weight(1f), Accent)
                    SummaryCard("LOOSE THOUGHTS", "${notes.size}", "notes on your canvas", Modifier.weight(1f), Color(0xFF8CCFFF))
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        MotionEntry(delayMillis = 145L) {
            Column {
                SectionTitle(
                    when (effectiveMode) { "Focus" -> "QUICK ACTIONS"; "Explore" -> "CAPTURE & EXPLORE"; else -> "QUICK CAPTURE" },
                    when (effectiveMode) { "Focus" -> "Keep the next move simple"; "Explore" -> "Let ideas arrive messy"; else -> "Start with one small thing" }
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    if (effectiveMode == "Explore") {
                        QuickAction("New note", "Catch an idea", Icons.Default.NoteAlt, Modifier.weight(1f), onNewNote)
                        QuickAction("Sketch", "Think visually", Icons.Default.Brush, Modifier.weight(1f), onOpenDraw)
                    } else {
                        QuickAction("Add task", "Name the next step", Icons.Default.CheckCircle, Modifier.weight(1f), onNewTask)
                        QuickAction("New note", "Catch an idea", Icons.Default.NoteAlt, Modifier.weight(1f), onNewNote)
                    }
                }
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    QuickAction("Open space", "Arrange your notes", Icons.Default.GridView, Modifier.weight(1f), onOpenSpace)
                    QuickAction("${if (effectiveMode == "Focus") "Find task" else "Sketch"}", if (effectiveMode == "Focus") "Search your work" else "Work it out visually", if (effectiveMode == "Focus") Icons.Default.Search else Icons.Default.Brush, Modifier.weight(1f), if (effectiveMode == "Focus") onOpenSearch else onOpenDraw)
                }
            }
        }
        Spacer(Modifier.height(19.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("UP NEXT", "Open tasks")
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onOpenTasks) { Text("View all", color = Accent) }
        }
        val nextTasks = openTasks.take(if (effectiveMode == "Focus") 1 else 3)
        if (nextTasks.isEmpty()) {
            EmptyPanel("Nothing pressing", "Add a small task when you're ready.")
        } else {
            nextTasks.forEach { task ->
                CompactTaskRow(task = task, onClick = { onEditTask(task) })
                Spacer(Modifier.height(7.dp))
            }
        }
        if (effectiveMode != "Focus") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("RECENT NOTES", "Your latest thoughts")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onOpenSpace) { Text("Canvas", color = Accent) }
            }
            val recentNotes = notes.takeLast(3).reversed()
            if (recentNotes.isEmpty()) EmptyPanel("Start with a sticky note", "Tap New note to get the first thought down.")
            else recentNotes.forEach { note -> CompactNoteRow(note, onClick = { onEditNote(note) }) }
        }
        Spacer(Modifier.height(18.dp))
        Column(
            Modifier.fillMaxWidth().animateContentSize(spring(dampingRatio = Spring.DampingRatioNoBouncy))
                .clip(RoundedCornerShape(20.dp))
                .background(GraphicsBrush.linearGradient(listOf(PanelRaised.copy(alpha = 0.58f), Panel)))
                .border(1.dp, Border, RoundedCornerShape(20.dp)).padding(15.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("YOUR DATA, YOURS", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Export a backup before changing phones or reinstalling.", color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onBackup, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)) {
                    Text("Export backup", fontWeight = FontWeight.SemiBold)
                }
                Button(onClick = onRestore, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = PanelRaised, contentColor = TextMain)) {
                    Text("Restore")
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("${drawings.size} saved ${if (drawings.size == 1) "drawing" else "drawings"} · stored on this device", color = Muted, fontSize = 10.sp)
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onOpenSearch, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Accent, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp)); Text("Search everything", color = Accent)
        }
    }
}

/**
 * Short, non-looping entrance used for the home dashboard's content hierarchy.
 * Compose animation respects the system animator-duration scale; no perpetual motion is used.
 */
@Composable
private fun MotionEntry(delayMillis: Long, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(delayMillis) {
        if (delayMillis > 0L) delay(delayMillis)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = if (MotionSetting) fadeIn(tween(250, easing = FastOutSlowInEasing)) +
            slideInVertically(
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                initialOffsetY = { height -> height / 18 }
            ) else fadeIn(tween(1)),
        exit = if (MotionSetting) fadeOut(tween(120)) + slideOutVertically(targetOffsetY = { height -> -height / 24 }, animationSpec = tween(120)) else fadeOut(tween(1))
    ) {
        content()
    }
}

@Composable
private fun SummaryCard(title: String, number: String, footnote: String, modifier: Modifier, tint: Color) {
    val parsedNumber = number.toIntOrNull()
    val animatedNumber by animateIntAsState(
        targetValue = parsedNumber ?: 0,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "metric-$title"
    )
    Column(
        modifier.animateContentSize(spring(dampingRatio = Spring.DampingRatioNoBouncy))
            .clip(RoundedCornerShape(20.dp))
            .background(GraphicsBrush.linearGradient(listOf(tint.copy(alpha = 0.12f), Panel, PanelRaised.copy(alpha = 0.72f))))
            .border(1.dp, tint.copy(alpha = 0.20f), RoundedCornerShape(20.dp))
            .padding(15.dp)
    ) {
        Box(Modifier.width(24.dp).height(3.dp).clip(CircleShape).background(tint.copy(alpha = 0.9f)))
        Spacer(Modifier.height(10.dp))
        Text(title, color = Muted, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(parsedNumber?.let { animatedNumber.toString() } ?: number, color = tint, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
        Text(footnote, color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun QuickAction(label: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "quick-action-press-$label"
    )
    Column(
        modifier.graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioNoBouncy))
            .clip(RoundedCornerShape(19.dp))
            .background(GraphicsBrush.linearGradient(listOf(PanelRaised, Panel, Bg)))
            .border(1.dp, if (pressed) Accent.copy(alpha = 0.44f) else Border, RoundedCornerShape(19.dp))
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(14.dp)
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Accent.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(11.dp))
        Text(label, color = TextMain, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(3.dp))
        Text(description, color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(3.dp))
        Text(subtitle, color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun EmptyPanel(title: String, subtitle: String) {
    Column(
        Modifier.fillMaxWidth().animateContentSize(spring(dampingRatio = Spring.DampingRatioNoBouncy))
            .clip(RoundedCornerShape(18.dp))
            .background(GraphicsBrush.linearGradient(listOf(PanelRaised.copy(alpha = 0.60f), Panel)))
            .border(1.dp, Border.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
            .padding(19.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = TextMain, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(5.dp))
        Text(subtitle, color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CompactTaskRow(task: Todo, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().animateContentSize(spring(dampingRatio = Spring.DampingRatioNoBouncy))
            .clip(RoundedCornerShape(15.dp)).background(Panel).border(1.dp, Border, RoundedCornerShape(15.dp))
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).background(priorityColor(task.priority), CircleShape))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(task.text, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text("${task.category} · ${task.priority} priority", color = Muted, fontSize = 10.sp)
        }
        if (task.subtasks.isNotEmpty()) Text("${task.subtasks.count { it.done }}/${task.subtasks.size}", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CompactNoteRow(note: StickyNote, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 7.dp).clip(RoundedCornerShape(15.dp)).background(Panel).border(1.dp, Border, RoundedCornerShape(15.dp)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(4.dp).height(36.dp).clip(CircleShape).background(NoteColors[note.colorIndex.coerceIn(NoteColors.indices)]))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(note.text, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 2)
            Text("Sticky note", color = Muted, fontSize = 10.sp)
        }
        Icon(Icons.Default.Edit, contentDescription = "Edit note", tint = Muted, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun WorkspaceScreen(
    notes: List<StickyNote>,
    onAdd: () -> Unit,
    onEdit: (StickyNote) -> Unit,
    onMove: (String, Float, Float, Float, Float) -> Unit,
    onMoveFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("THE CANVAS", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Move thoughts around.", color = TextMain, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("${notes.size} ${if (notes.size == 1) "note" else "notes"} · hold and drag to arrange", color = Muted, fontSize = 11.sp)
            }
            Button(onClick = onAdd, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg), contentPadding = PaddingValues(horizontal = 13.dp, vertical = 9.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("Note")
            }
        }
        Spacer(Modifier.height(12.dp))
        WorkspaceCanvas(
            notes = notes,
            onMove = onMove,
            onMoveFinished = onMoveFinished,
            onEdit = onEdit,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text("DRAG TO MOVE  ·  TAP TO EDIT  ·  SAVES AUTOMATICALLY", color = Muted, fontSize = 9.sp, letterSpacing = 0.7.sp, modifier = Modifier.padding(bottom = 6.dp))
    }
}

@Composable
private fun WorkspaceCanvas(
    notes: List<StickyNote>,
    onMove: (String, Float, Float, Float, Float) -> Unit,
    onMoveFinished: () -> Unit,
    onEdit: (StickyNote) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val latestOnMove by rememberUpdatedState(onMove)
    val latestOnMoveFinished by rememberUpdatedState(onMoveFinished)
    val noteWidthPx = with(density) { 136.dp.toPx() }
    val noteHeightPx = with(density) { 118.dp.toPx() }
    BoxWithConstraints(
        modifier.clip(RoundedCornerShape(22.dp)).background(Board).border(1.dp, Border, RoundedCornerShape(22.dp))
    ) {
        val maxX = (constraints.maxWidth.toFloat() - noteWidthPx - with(density) { 6.dp.toPx() }).coerceAtLeast(0f)
        val maxY = (constraints.maxHeight.toFloat() - noteHeightPx - with(density) { 6.dp.toPx() }).coerceAtLeast(0f)
        Canvas(Modifier.fillMaxSize()) {
            val step = 24.dp.toPx()
            var x = 12f
            while (x < size.width) {
                var y = 12f
                while (y < size.height) { drawCircle(Border.copy(alpha = if (ActiveThemePreset == "Light") 0.8f else 0.72f), 1.1f, Offset(x, y)); y += step }
                x += step
            }
        }
        notes.forEach { note ->
            key(note.id) {
            var isDragging by remember { mutableStateOf(false) }
            val noteScale by animateFloatAsState(
                targetValue = if (isDragging) 1.045f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "note-scale-${note.id}"
            )
            val noteRotation by animateFloatAsState(
                targetValue = if (isDragging) -1.2f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                label = "note-rotation-${note.id}"
            )
            val noteElevation by animateDpAsState(
                targetValue = if (isDragging) 12.dp else 1.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                label = "note-elevation-${note.id}"
            )
            Box(
                Modifier.offset {
                    IntOffset(note.x.roundToInt().coerceIn(0, maxX.roundToInt()), note.y.roundToInt().coerceIn(0, maxY.roundToInt()))
                }.shadow(noteElevation, RoundedCornerShape(13.dp), clip = false)
                    .graphicsLayer {
                            scaleX = noteScale
                            scaleY = noteScale
                            rotationZ = noteRotation
                        }
                        .size(136.dp, 118.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(NoteColors[note.colorIndex.coerceIn(NoteColors.indices)])
                        .border(1.dp, Color(0x55333333), RoundedCornerShape(13.dp))
                        .pointerInput(note.id, maxX, maxY) {
                            detectDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd = {
                                    isDragging = false
                                    latestOnMoveFinished()
                                },
                                onDragCancel = {
                                    isDragging = false
                                    latestOnMoveFinished()
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    latestOnMove(note.id, dragAmount.x, dragAmount.y, maxX, maxY)
                                }
                            )
                        }
                        .clickable { onEdit(note) }
                        .padding(11.dp)
                ) {
                    Text(note.text, color = Color(0xFF24262A), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 6, lineHeight = 15.sp)
                    Text("NOTE", Modifier.align(Alignment.BottomStart), color = Color(0x8824262A), fontSize = 8.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        if (notes.isEmpty()) {
            Column(Modifier.align(Alignment.Center).padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(PanelRaised), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.NoteAlt, contentDescription = null, tint = Accent, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("Your space, your pace.", color = TextMain, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("Create a note and drag it anywhere on the canvas.", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun TasksScreen(
    todos: List<Todo>,
    filter: String,
    onFilter: (String) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Todo) -> Unit,
    onDelete: (Todo) -> Unit,
    onToggle: (Todo, Boolean) -> Unit,
    onToggleSubtask: (Todo, SubTask, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = when (filter) {
        "Done" -> todos.filter { it.done }
        "Open" -> todos.filterNot { it.done }
        else -> todos
    }
    val done = todos.count { it.done }
    val groupedVisible = remember(visible) {
        visible.groupBy { it.category.ifBlank { "General" } }
            .toSortedMap()
            .mapValues { (_, tasks) -> tasks.sortedBy { priorityRank(it.priority) } }
    }
    Column(modifier) {
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("TASK MANAGER", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("One thing at a time.", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("$done of ${todos.size} completed", color = Muted, fontSize = 11.sp)
            }
            Button(onClick = onAdd, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text("Task")
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("Open", "All", "Done").forEach { item ->
                FilterChip(selected = filter == item, onClick = { onFilter(item) }, label = { Text(item) })
            }
            FilterChip(selected = false, onClick = onAdd, label = { Text("+ New task") })
        }
        Spacer(Modifier.height(10.dp))
        if (visible.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyPanel(if (filter == "Done") "Nothing completed yet" else "A clear list is a good start", "Add one clear next step, then work through it.")
            }
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().padding(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedVisible.forEach { (category, grouped) ->
                    item(key = "category:$category") {
                        Text(category.uppercase(), color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    items(items = grouped, key = { it.id }) { todo ->
                        TaskCard(todo, onEdit, onDelete, onToggle, onToggleSubtask)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    todo: Todo,
    onEdit: (Todo) -> Unit,
    onDelete: (Todo) -> Unit,
    onToggle: (Todo, Boolean) -> Unit,
    onToggleSubtask: (Todo, SubTask, Boolean) -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue = if (todo.done) PanelRaised else Panel,
        animationSpec = tween(220),
        label = "task-card-color-${todo.id}"
    )
    val titleColor by animateColorAsState(
        targetValue = if (todo.done) Muted else TextMain,
        animationSpec = tween(220),
        label = "task-title-color-${todo.id}"
    )
    Column(
        Modifier.fillMaxWidth().animateContentSize(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow))
            .clip(RoundedCornerShape(18.dp)).background(cardColor)
            .border(1.dp, if (todo.done) Border.copy(alpha = 0.62f) else Border, RoundedCornerShape(18.dp)).padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = todo.done,
                onCheckedChange = { onToggle(todo, it) },
                colors = CheckboxDefaults.colors(checkedColor = Accent, uncheckedColor = Muted, checkmarkColor = Bg)
            )
            Column(Modifier.weight(1f).clickable { onEdit(todo) }.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)) {
                Text(todo.text, color = titleColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None)
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(priorityColor(todo.priority).copy(alpha = 0.17f)).padding(horizontal = 7.dp, vertical = 3.dp)) {
                        Text(todo.priority.uppercase(), color = priorityColor(todo.priority), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
                    }
                    if (todo.subtasks.isNotEmpty()) Text("${todo.subtasks.count { it.done }}/${todo.subtasks.size} steps", color = Muted, fontSize = 10.sp)
                }
            }
            IconButton(onClick = { onEdit(todo) }) { Icon(Icons.Default.Edit, contentDescription = "Edit task", tint = Muted, modifier = Modifier.size(18.dp)) }
            IconButton(onClick = { onDelete(todo) }) { Icon(Icons.Default.Delete, contentDescription = "Delete task", tint = Muted, modifier = Modifier.size(18.dp)) }
        }
        AnimatedVisibility(
            visible = todo.subtasks.isNotEmpty(),
            enter = expandVertically(animationSpec = tween(190, easing = FastOutSlowInEasing)) + fadeIn(tween(150)),
            exit = shrinkVertically(animationSpec = tween(150)) + fadeOut(tween(100))
        ) {
            Column {
                Divider(color = Border, modifier = Modifier.padding(vertical = 5.dp))
                todo.subtasks.forEach { subtask ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { onToggleSubtask(todo, subtask, !subtask.done) }.padding(vertical = 1.dp)
                    ) {
                        Checkbox(
                            checked = subtask.done,
                            onCheckedChange = { onToggleSubtask(todo, subtask, it) },
                            colors = CheckboxDefaults.colors(checkedColor = Accent, uncheckedColor = Muted, checkmarkColor = Bg),
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            subtask.text,
                            color = if (subtask.done) Muted else TextMain,
                            fontSize = 12.sp,
                            textDecoration = if (subtask.done) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                }
            }
        }
    }
}

private fun priorityRank(priority: String): Int = when (priority.lowercase()) {
    "urgent" -> -1
    "high" -> 0
    "normal", "medium" -> 1
    else -> 2
}

private fun priorityColor(priority: String): Color = when (priority.lowercase()) {
    "urgent", "high" -> Color(0xFFFF8D8D)
    "low" -> Color(0xFF80CFFF)
    else -> Accent
}

@Composable
private fun DrawingsScreen(
    drawings: List<SavedDrawing>,
    selectedDrawingId: String,
    brushArgb: Int,
    eraserMode: Boolean,
    brushWidthDp: Float,
    onSelectDrawing: (String) -> Unit,
    onNewDrawing: () -> Unit,
    onBrush: (Int) -> Unit,
    onEraser: () -> Unit,
    onBrushWidth: (Float) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onStrokesChanged: (List<DrawStroke>) -> Unit,
    modifier: Modifier = Modifier
) {
    val active = drawings.firstOrNull { it.id == selectedDrawingId } ?: drawings.first()
    Column(modifier) {
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("SKETCHBOOK", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(active.name, color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("${active.strokes.size} saved strokes", color = Muted, fontSize = 11.sp)
            }
            Button(onClick = onNewDrawing, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text("Page")
            }
        }
        Spacer(Modifier.height(11.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            drawings.forEach { drawing ->
                FilterChip(selected = drawing.id == selectedDrawingId, onClick = { onSelectDrawing(drawing.id) }, label = { Text(drawing.name, maxLines = 1) })
            }
        }
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            BrushColors.forEach { (name, color) ->
                BrushDot(color = color, selected = !eraserMode && brushArgb == color, onClick = { onBrush(color) }, label = name)
            }
            IconButton(onClick = onUndo, enabled = active.strokes.isNotEmpty()) {
                Icon(Icons.Default.Undo, contentDescription = "Undo last stroke", tint = if (active.strokes.isEmpty()) Muted.copy(alpha = 0.4f) else Muted)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("BRUSH", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
            Spacer(Modifier.width(10.dp))
            listOf(2.5f, 4.5f, 7f).forEach { size ->
                FilterChip(selected = brushWidthDp == size, onClick = { onBrushWidth(size) }, label = { Text(when (size) { 2.5f -> "Fine"; 4.5f -> "Medium"; else -> "Bold" }) })
                Spacer(Modifier.width(3.dp))
            }
            Spacer(Modifier.weight(1f))
            FilterChip(selected = eraserMode, onClick = onEraser, label = { Text(if (eraserMode) "Eraser on" else "Eraser") })
            IconButton(onClick = onClear, enabled = active.strokes.isNotEmpty()) {
                Icon(Icons.Default.Delete, contentDescription = "Clear drawing", tint = if (active.strokes.isEmpty()) Muted.copy(alpha = 0.4f) else Muted)
            }
        }
        Spacer(Modifier.height(6.dp))
        DrawingCanvas(
            strokes = active.strokes,
            brushArgb = brushArgb,
            brushWidthDp = brushWidthDp,
            eraserMode = eraserMode,
            onStrokesChanged = onStrokesChanged,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        Spacer(Modifier.height(7.dp))
        Text("Separate pages · multi-colour brush · erase · undo", color = Muted, fontSize = 9.sp, letterSpacing = 0.3.sp, modifier = Modifier.padding(bottom = 5.dp))
    }
}

@Composable
private fun BrushDot(color: Int, selected: Boolean, onClick: () -> Unit, label: String) {
    val dotScale by animateFloatAsState(
        targetValue = if (selected) 1.10f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "brush-dot-scale-$label"
    )
    val outlineColor by animateColorAsState(
        targetValue = if (selected) Accent else Border,
        animationSpec = tween(160),
        label = "brush-dot-outline-$label"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).clickable(onClick = onClick)
    ) {
        Box(
            Modifier.size(24.dp).graphicsLayer { scaleX = dotScale; scaleY = dotScale }
                .border(if (selected) 2.dp else 1.dp, outlineColor, CircleShape)
                .padding(4.dp).background(colorFromArgb(color), CircleShape)
        )
        Text(label, color = if (selected) Accent else Muted, fontSize = 8.sp, maxLines = 1)
    }
}

private data class CachedStroke(val stroke: DrawStroke, val path: Path)

private fun smoothStrokePath(points: List<Offset>): Path = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points.first().x, points.first().y)
    if (points.size == 1) return@apply
    for (index in 1 until points.size) {
        val point = points[index]
        val next = points.getOrNull(index + 1)
        if (next != null) {
            quadraticTo(point.x, point.y, (point.x + next.x) / 2f, (point.y + next.y) / 2f)
        } else {
            lineTo(point.x, point.y)
        }
    }
}

@Composable
private fun DrawingCanvas(
    strokes: List<DrawStroke>,
    brushArgb: Int,
    brushWidthDp: Float,
    eraserMode: Boolean,
    onStrokesChanged: (List<DrawStroke>) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { brushWidthDp.dp.toPx() }
    val livePath = remember { Path() }
    var isDrawing by remember { mutableStateOf(false) }
    var liveStrokeRevision by remember { mutableIntStateOf(0) }
    // Existing paths are rebuilt only when saved strokes change, not on every touch sample.
    val cachedStrokes = remember(strokes) {
        strokes.map { stroke -> CachedStroke(stroke, smoothStrokePath(stroke.points)) }
    }
    Box(
        modifier.clip(RoundedCornerShape(22.dp)).background(Board)
            .border(1.dp, Border.copy(alpha = 0.9f), RoundedCornerShape(22.dp))
    ) {
        Canvas(
            Modifier.fillMaxSize().pointerInput(strokes, brushArgb, eraserMode, strokeWidthPx) {
                val points = mutableListOf<Offset>()
                detectDragGestures(
                    onDragStart = { start ->
                        points.clear()
                        points.add(start)
                        livePath.reset()
                        livePath.moveTo(start.x, start.y)
                        isDrawing = true
                        liveStrokeRevision++
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val point = change.position
                        points.add(point) // Amortized O(1), rather than copying the full list per sample.
                        livePath.lineTo(point.x, point.y)
                        liveStrokeRevision++
                    },
                    onDragEnd = {
                        if (points.isNotEmpty()) {
                            onStrokesChanged(strokes + DrawStroke(points.toList(), brushArgb, strokeWidthPx, eraserMode))
                        }
                        points.clear()
                        livePath.reset()
                        isDrawing = false
                        liveStrokeRevision++
                    },
                    onDragCancel = {
                        points.clear()
                        livePath.reset()
                        isDrawing = false
                        liveStrokeRevision++
                    }
                )
            }
        ) {
            val step = 24.dp.toPx()
            var x = 12f
            while (x < size.width) {
                var y = 12f
                while (y < size.height) {
                    drawCircle(Border.copy(alpha = if (ActiveThemePreset == "Light") 0.8f else 0.72f), 1.05f, Offset(x, y))
                    y += step
                }
                x += step
            }
            cachedStrokes.forEach { cached ->
                val stroke = cached.stroke
                val drawColor = if (stroke.eraser) Board else colorFromArgb(stroke.colorArgb)
                if (stroke.points.size > 1) {
                    drawPath(cached.path, drawColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke.width, cap = StrokeCap.Round))
                } else if (stroke.points.isNotEmpty()) {
                    drawCircle(drawColor, stroke.width / 2f, stroke.points.first())
                }
            }
            // Reading this state invalidates only the drawing layer while a live stroke changes.
            if (isDrawing && liveStrokeRevision >= 0) {
                val liveColor = if (eraserMode) Board else colorFromArgb(brushArgb)
                drawPath(livePath, liveColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx, cap = StrokeCap.Round))
            }
        }
        if (strokes.isEmpty() && !isDrawing) {
            Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Draw, contentDescription = null, tint = Accent, modifier = Modifier.size(30.dp))
                Spacer(Modifier.height(8.dp))
                Text("A blank page", color = TextMain, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("Sketch a diagram, thought, or calculation.", color = Muted, textAlign = TextAlign.Center, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SearchScreen(
    query: String,
    onQuery: (String) -> Unit,
    notes: List<StickyNote>,
    todos: List<Todo>,
    drawings: List<SavedDrawing>,
    modifier: Modifier = Modifier,
    onOpenNote: (String) -> Unit,
    onOpenTask: (String) -> Unit,
    onOpenDrawing: (String) -> Unit
) {
    val q = query.trim().lowercase()
    val hits = remember(q, notes, todos, drawings) {
        buildList {
            notes.filter { q.isNotEmpty() && it.text.lowercase().contains(q) }.forEach { add(SearchHit(it.id, "Note", it.text, "Sticky note")) }
            todos.filter { q.isNotEmpty() && (it.text.lowercase().contains(q) || it.category.lowercase().contains(q) || it.subtasks.any { sub -> sub.text.lowercase().contains(q) }) }
                .forEach { add(SearchHit(it.id, "Task", it.text, "${it.category} · ${it.priority} priority")) }
            drawings.filter { q.isNotEmpty() && it.name.lowercase().contains(q) }.forEach { add(SearchHit(it.id, "Drawing", it.name, "${it.strokes.size} strokes")) }
        }
    }
    Column(modifier) {
        Spacer(Modifier.height(5.dp))
        Text("FIND IT FAST", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Search your space.", color = TextMain, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search notes, tasks, drawings…", color = Muted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Accent) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Default.Close, contentDescription = "Clear search") } },
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(8.dp))
        if (q.isBlank()) {
            EmptyPanel("Search everything", "Type a word or phrase to find notes, tasks, subtasks, and drawings.")
        } else if (hits.isEmpty()) {
            EmptyPanel("No matches", "Try a different word or a shorter phrase.")
        } else {
            Text("${hits.size} ${if (hits.size == 1) "RESULT" else "RESULTS"}", color = Muted, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 7.dp))
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().padding(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items(items = hits, key = { "${it.type}:${it.id}" }) { hit ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel).border(1.dp, Border, RoundedCornerShape(15.dp)).clickable {
                            when (hit.type) {
                                "Note" -> onOpenNote(hit.id)
                                "Task" -> onOpenTask(hit.id)
                                else -> onOpenDrawing(hit.id)
                            }
                        }.padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(PanelRaised), contentAlignment = Alignment.Center) {
                            Icon(when (hit.type) { "Note" -> Icons.Default.NoteAlt; "Task" -> Icons.Default.TaskAlt; else -> Icons.Default.Brush }, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(hit.title, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                            Text("${hit.type} · ${hit.detail}", color = Muted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickCaptureDialog(
    onDismiss: () -> Unit,
    onCreateNote: (String) -> Unit,
    onCreateTask: (String, String, String) -> Unit
) {
    var kind by remember { mutableStateOf("Note") }
    var text by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Normal") }
    var category by remember { mutableStateOf("General") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick capture", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("Note", "Task").forEach { option -> FilterChip(selected = kind == option, onClick = { kind = option }, label = { Text(option) }) }
                }
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), label = { Text(if (kind == "Note") "Capture a thought" else "What's the next step?") }, minLines = 2, maxLines = 4)
                if (kind == "Task") {
                    OutlinedTextField(value = category, onValueChange = { category = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Category") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("High", "Normal", "Low").forEach { p -> FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p) }) }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (text.isNotBlank()) { if (kind == "Note") onCreateNote(text) else onCreateTask(text, priority, category) } }, enabled = text.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)) { Text("Capture") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Muted) } },
        containerColor = Panel,
        titleContentColor = TextMain,
        textContentColor = TextMain
    )
}

@Composable
private fun NoteEditorDialog(
    note: StickyNote?,
    onDismiss: () -> Unit,
    onDelete: (StickyNote) -> Unit,
    onSave: (StickyNote) -> Unit
) {
    var text by remember(note?.id) { mutableStateOf(note?.text.orEmpty()) }
    var colorIndex by remember(note?.id) { mutableIntStateOf(note?.colorIndex ?: 0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (note == null) "New sticky note" else "Edit sticky note", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Keep it short enough to scan on your canvas.", color = Muted, fontSize = 12.sp)
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Your thought") }, minLines = 3, maxLines = 6)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("COLOUR", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
                    NoteColors.forEachIndexed { index, color ->
                        Box(
                            Modifier.size(28.dp).border(if (colorIndex == index) 2.dp else 1.dp, if (colorIndex == index) Accent else Border, CircleShape).padding(4.dp).background(color, CircleShape).clickable { colorIndex = index }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val old = note
                onSave(if (old == null) StickyNote(text = text.trim(), colorIndex = colorIndex) else old.copy(text = text.trim(), colorIndex = colorIndex))
            }, enabled = text.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)) { Text(if (note == null) "Create note" else "Save changes") }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note != null) TextButton(onClick = { onDelete(note) }) { Text("Delete", color = Color(0xFFFF8D8D)) }
                TextButton(onClick = onDismiss) { Text("Cancel", color = Muted) }
            }
        },
        containerColor = Panel,
        titleContentColor = TextMain,
        textContentColor = TextMain
    )
}

@Composable
private fun TodoEditorDialog(todo: Todo?, onDismiss: () -> Unit, onSave: (Todo) -> Unit) {
    var title by remember(todo?.id) { mutableStateOf(todo?.text.orEmpty()) }
    var priority by remember(todo?.id) { mutableStateOf(todo?.priority ?: "Normal") }
    var category by remember(todo?.id) { mutableStateOf(todo?.category ?: "General") }
    var subtaskLines by remember(todo?.id) { mutableStateOf(todo?.subtasks?.joinToString("\n") { it.text }.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (todo == null) "Add a task" else "Edit task", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Task") }, minLines = 1, maxLines = 3)
                OutlinedTextField(value = category, onValueChange = { category = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Category") }, singleLine = true)
                Text("PRIORITY", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("High", "Normal", "Low").forEach { option -> FilterChip(selected = priority == option, onClick = { priority = option }, label = { Text(option) }) }
                }
                Text("SUBTASKS · one per line", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = subtaskLines, onValueChange = { subtaskLines = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Read chapter\nSolve five questions", color = Muted) }, minLines = 3, maxLines = 6)
            }
        },
        confirmButton = {
            Button(onClick = {
                val oldSubs = todo?.subtasks.orEmpty()
                val newSubs = subtaskLines.lines().map { it.trim() }.filter { it.isNotBlank() }.map { line ->
                    oldSubs.firstOrNull { it.text.equals(line, ignoreCase = true) } ?: SubTask(text = line)
                }
                onSave(Todo(
                    id = todo?.id ?: UUID.randomUUID().toString(),
                    text = title.trim(),
                    done = todo?.done ?: false,
                    priority = priority,
                    category = category.trim().ifBlank { "General" },
                    subtasks = newSubs
                ))
            }, enabled = title.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)) { Text(if (todo == null) "Add task" else "Save task") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Muted) } },
        containerColor = Panel,
        titleContentColor = TextMain,
        textContentColor = TextMain
    )
}

@Composable
private fun SimpleInputDialog(
    title: String,
    subtitle: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(subtitle, color = Muted, fontSize = 12.sp)
                OutlinedTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), label = { Text(placeholder) }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = onSave, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Muted) } },
        containerColor = Panel,
        titleContentColor = TextMain,
        textContentColor = TextMain
    )
}

private fun encodeNotes(notes: List<StickyNote>): JSONArray = JSONArray().apply {
    notes.forEach { note -> put(JSONObject().apply {
        put("id", note.id); put("text", note.text); put("x", note.x); put("y", note.y); put("color", note.colorIndex)
    }) }
}

private fun encodeTodos(todos: List<Todo>): JSONArray = JSONArray().apply {
    todos.forEach { task -> put(JSONObject().apply {
        put("id", task.id); put("text", task.text); put("done", task.done)
        put("priority", task.priority); put("category", task.category)
        put("subtasks", JSONArray().apply { task.subtasks.forEach { sub -> put(JSONObject().apply { put("id", sub.id); put("text", sub.text); put("done", sub.done) }) } })
    }) }
}

private fun encodeStrokes(strokes: List<DrawStroke>): JSONArray = JSONArray().apply {
    strokes.forEach { stroke -> put(JSONObject().apply {
        put("color", if (stroke.colorArgb == 0xFFFFFFFF.toInt()) "white" else "accent")
        put("colorArgb", stroke.colorArgb.toLong() and 0xFFFFFFFFL)
        put("width", stroke.width); put("eraser", stroke.eraser)
        put("points", JSONArray().apply { stroke.points.forEach { p -> put(JSONObject().apply { put("x", p.x); put("y", p.y) }) } })
    }) }
}

private fun encodeDrawings(drawings: List<SavedDrawing>): JSONArray = JSONArray().apply {
    drawings.forEach { drawing -> put(JSONObject().apply {
        put("id", drawing.id); put("name", drawing.name); put("strokes", encodeStrokes(drawing.strokes))
    }) }
}

private fun encodeBackup(notes: List<StickyNote>, todos: List<Todo>, drawings: List<SavedDrawing>, selectedDrawingId: String): JSONObject = JSONObject().apply {
    put("format", "slatespace-backup")
    put("version", 2)
    put("createdAt", System.currentTimeMillis())
    put("notes", encodeNotes(notes))
    put("todos", encodeTodos(todos))
    put("drawings", encodeDrawings(drawings))
    put("selectedDrawingId", selectedDrawingId)
}

private fun decodeBackup(raw: String): BackupPayload {
    val root = JSONObject(raw)
    val notesArray = root.optJSONArray("notes") ?: throw IllegalArgumentException("Backup has no notes section")
    val todosArray = root.optJSONArray("todos") ?: throw IllegalArgumentException("Backup has no tasks section")
    val drawingsArray = root.optJSONArray("drawings")
    val legacyStrokes = root.optJSONArray("strokes")?.toString()
    return BackupPayload(
        notes = loadNotes(notesArray.toString()),
        todos = loadTodos(todosArray.toString()),
        drawings = loadDrawings(drawingsArray?.toString(), legacyStrokes),
        selectedDrawingId = root.optString("selectedDrawingId", null)
    )
}

private fun loadNotes(raw: String?): List<StickyNote> = try {
    if (raw.isNullOrBlank()) emptyList() else {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i -> arr.getJSONObject(i).let { obj ->
            StickyNote(
                id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                text = obj.optString("text", ""),
                x = obj.optDouble("x", 24.0).toFloat(),
                y = obj.optDouble("y", 24.0).toFloat(),
                colorIndex = obj.optInt("color", 0).coerceIn(NoteColors.indices)
            )
        } }
    }
} catch (_: Exception) { emptyList() }

private fun loadTodos(raw: String?): List<Todo> = try {
    if (raw.isNullOrBlank()) emptyList() else {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i -> arr.getJSONObject(i).let { obj ->
            val subArray = obj.optJSONArray("subtasks")
            val subs = if (subArray == null) emptyList() else (0 until subArray.length()).map { j ->
                val sub = subArray.getJSONObject(j)
                SubTask(sub.optString("id").ifBlank { UUID.randomUUID().toString() }, sub.optString("text", ""), sub.optBoolean("done", false))
            }
            Todo(
                id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                text = obj.optString("text", ""),
                done = obj.optBoolean("done", false),
                priority = obj.optString("priority", "Normal").let { stored ->
                    listOf("Urgent", "High", "Normal", "Low").firstOrNull { it.equals(stored, ignoreCase = true) } ?: "Normal"
                },
                category = obj.optString("category", "General").ifBlank { "General" },
                subtasks = subs
            )
        } }
    }
} catch (_: Exception) { emptyList() }

private fun loadStrokes(raw: String?): List<DrawStroke> = try {
    if (raw.isNullOrBlank()) emptyList() else {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val pointsArray = obj.optJSONArray("points") ?: JSONArray()
            val points = (0 until pointsArray.length()).map { j ->
                val point = pointsArray.getJSONObject(j)
                Offset(point.optDouble("x", 0.0).toFloat(), point.optDouble("y", 0.0).toFloat())
            }
            val defaultArgb = if (obj.optString("color") == "white") 0xFFFFFFFF.toInt() else AccentArgb
            val argb = if (obj.has("colorArgb")) obj.optLong("colorArgb", defaultArgb.toLong()).toInt() else defaultArgb
            DrawStroke(points, argb, obj.optDouble("width", 5.0).toFloat().coerceAtLeast(1f), obj.optBoolean("eraser", false))
        }
    }
} catch (_: Exception) { emptyList() }

private fun loadDrawings(rawDrawings: String?, legacyStrokes: String?): List<SavedDrawing> {
    if (!rawDrawings.isNullOrBlank()) {
        try {
            val arr = JSONArray(rawDrawings)
            val parsed = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                SavedDrawing(
                    id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                    name = obj.optString("name", "Sketch ${i + 1}").ifBlank { "Sketch ${i + 1}" },
                    strokes = loadStrokes(obj.optJSONArray("strokes")?.toString())
                )
            }
            if (parsed.isNotEmpty()) return parsed
        } catch (_: Exception) { /* Fall through to the legacy single canvas. */ }
    }
    val legacy = loadStrokes(legacyStrokes)
    return listOf(SavedDrawing(id = "legacy-sketch", name = "Sketch 1", strokes = legacy))
}
