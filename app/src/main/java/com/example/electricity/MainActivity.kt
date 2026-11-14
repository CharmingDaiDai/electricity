package com.example.electricity

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs

private enum class Screen { Home, Records, Settings }
private enum class RecordMode { Day, Week, Month, Year }
private enum class OverviewMode { Cycle, MonthToDate, SinceRentDay, RentWindow, All }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferHighestRefreshRate()
        setContent {
            ElectricityTheme {
                ElectricityApp()
            }
        }
    }

    private fun preferHighestRefreshRate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val mode = windowManager.defaultDisplay.supportedModes.maxByOrNull { it.refreshRate } ?: return
        val attributes = window.attributes
        attributes.preferredDisplayModeId = mode.modeId
        window.attributes = attributes
    }
}

@Composable
private fun ElectricityTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = Color(0xFFE9ECF0),
            onPrimary = Color(0xFF121417),
            primaryContainer = Color(0xFF272B30),
            onPrimaryContainer = Color(0xFFE9ECF0),
            secondary = Color(0xFF7DD3FC),
            secondaryContainer = Color(0xFF123142),
            surface = Color(0xFF15171A),
            surfaceVariant = Color(0xFF24272C),
            background = Color(0xFF0B0D0F),
            outline = Color(0xFF555B63),
            outlineVariant = Color(0xFF373C43),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF171A1F),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE8EBEE),
            onPrimaryContainer = Color(0xFF171A1F),
            secondary = Color(0xFF0C78A0),
            secondaryContainer = Color(0xFFD9F0F9),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFF1F3F5),
            background = Color(0xFFF5F6F8),
            outline = Color(0xFF727981),
            outlineVariant = Color(0xFFDCE0E4),
        )
    }
    MaterialTheme(
        colorScheme = colors,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(22.dp),
            extraLarge = RoundedCornerShape(28.dp),
        ),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ElectricityApp() {
    val context = LocalContext.current
    val entries = remember { mutableStateListOf<UsageEntry>() }
    var settings by remember { mutableStateOf(loadSettings(context)) }
    val currentIndex = cycleIndexOf(settings.cycleStartDate, LocalDate.now())
    var manualPrices by remember { mutableStateOf<Map<Int, Double>>(emptyMap()) }
    var settlements by remember { mutableStateOf<Map<Int, Double>>(emptyMap()) }
    var settlementFocus by remember { mutableStateOf<Int?>(currentIndex) }
    var overviewMode by remember { mutableStateOf(OverviewMode.Cycle) }
    var screen by remember { mutableStateOf(Screen.Home) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var editingEntry by remember { mutableStateOf<UsageEntry?>(null) }
    var overridingCycle by remember { mutableStateOf<CycleSummary?>(null) }
    var settlingCycle by remember { mutableStateOf<CycleSummary?>(null) }
    var showEntryDialog by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(encodeBackup(settings, entries, manualPrices, settlements).toByteArray(Charsets.UTF_8))
                } ?: error("无法写入备份文件")
            }.onSuccess {
                backupMessage = "备份已导出。"
            }.onFailure {
                backupMessage = "备份导出失败：${it.message ?: "无法写入文件"}"
            }
        }
    }
    val importBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingImportUri = uri
    }

    LaunchedEffect(Unit) {
        entries.clear()
        entries.addAll(loadEntries(context))
        manualPrices = loadManualPrices(context)
        settlements = loadSettlements(context)
    }

    val summaries = buildCycleSummaries(entries, settings, manualPrices = manualPrices)
    val current = summaries.lastOrNull { it.index == currentIndex } ?: summaries.last()

    Scaffold(
        topBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text("电费账本", style = MaterialTheme.typography.labelSmall, letterSpacing = 0.5.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = { fadeIn(tween(130)) togetherWith fadeOut(tween(90)) },
                    label = "page-heading",
                ) { page ->
                    Text(
                        when (page) {
                            Screen.Home -> "概览"
                            Screen.Records -> "用电日历"
                            Screen.Settings -> "计费与数据"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                    )
                }
            }
        },
        bottomBar = {
            BottomDock(screen = screen, onScreen = { screen = it })
        },
        floatingActionButton = {
            AnimatedVisibility(visible = screen != Screen.Settings, enter = fadeIn(tween(150)), exit = fadeOut(tween(100))) {
                Surface(
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).pressScale { showAddSheet = true },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 8.dp,
                ) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("新增记录", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
        ) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (fadeIn(tween(180)) + slideInHorizontally(tween(220)) { direction * it / 30 }) togetherWith
                        (fadeOut(tween(100)) + slideOutHorizontally(tween(150)) { -direction * it / 36 })
                },
                label = "main-screen",
            ) { targetScreen ->
              when (targetScreen) {
                Screen.Home -> HomeScreen(
                    current = current,
                    summaries = summaries,
                    manualPrices = manualPrices,
                    settlements = settlements,
                    settlementFocus = settlementFocus,
                    overviewMode = overviewMode,
                    rentDay = settings.rentDay,
                    onSettlementFocus = { settlementFocus = it },
                    onOverviewMode = { overviewMode = it },
                    onOverridePrice = { overridingCycle = it },
                    onSettle = { settlingCycle = it },
                    onQuickAdd = { showAddSheet = true },
                )
                Screen.Records -> RecordsScreen(
                    entries = entries,
                    selectedDate = selectedDate,
                    onSelectedDate = { selectedDate = it },
                    onEdit = {
                        selectedDate = it.date
                        editingEntry = it
                        showEntryDialog = true
                    },
                    onDelete = {
                        entries.removeAll { entry -> entry.date == it.date }
                        saveEntries(context, entries)
                    },
                    onAddForDate = {
                        selectedDate = it
                        editingEntry = entries.firstOrNull { entry -> entry.date == it }
                        showEntryDialog = true
                    },
                )
                Screen.Settings -> SettingsScreen(
                    settings = settings,
                    entries = entries,
                    backupMessage = backupMessage,
                    onSave = {
                        settings = it
                        saveSettings(context, it)
                    },
                    onExportBackup = { exportBackupLauncher.launch("electricity-backup.electricity") },
                    onImportBackup = { importBackupLauncher.launch(arrayOf("application/octet-stream", "text/plain", "*/*")) },
                )
              }
            }
        }
    }

    if (showEntryDialog) {
        EntryDialog(
            date = selectedDate,
            entry = editingEntry,
            onDismiss = { showEntryDialog = false },
            onSave = { entry ->
                entries.removeAll { it.date == entry.date }
                entries += entry
                entries.sortBy { it.date.toEpochDay() }
                saveEntries(context, entries)
                showEntryDialog = false
            },
        )
    }

    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }) {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("新增用电记录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("选择录入方式 · 单日或批量", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ActionChoice(
                    title = "单日录入",
                    detail = "${selectedDate} · ${if (entries.any { it.date == selectedDate }) "编辑已有记录" else "填写当天用电量"}",
                    icon = Icons.Default.Add,
                    onClick = {
                    showAddSheet = false
                    editingEntry = entries.firstOrNull { it.date == selectedDate }
                    showEntryDialog = true
                    },
                )
                ActionChoice(
                    title = "批量录入",
                    detail = "粘贴多行日期和用电量",
                    icon = Icons.Default.Upload,
                    onClick = {
                    showAddSheet = false
                    showBatchDialog = true
                    },
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showBatchDialog) {
        BatchDialog(
            onDismiss = { showBatchDialog = false },
            onImport = { parsed ->
                parsed.entries.forEach { entry ->
                    entries.removeAll { it.date == entry.date }
                    entries += entry
                }
                entries.sortBy { it.date.toEpochDay() }
                saveEntries(context, entries)
                showBatchDialog = false
            },
        )
    }

    overridingCycle?.let { summary ->
        PriceOverrideDialog(
            summary = summary,
            isOverridden = manualPrices.containsKey(summary.index),
            onDismiss = { overridingCycle = null },
            onClear = {
                manualPrices = manualPrices - summary.index
                saveManualPrices(context, manualPrices)
                overridingCycle = null
            },
            onSave = {
                manualPrices = manualPrices + (summary.index to it)
                saveManualPrices(context, manualPrices)
                overridingCycle = null
            },
        )
    }

    settlingCycle?.let { summary ->
        SettlementDialog(
            summary = summary,
            currentValue = settlements[summary.index],
            onDismiss = { settlingCycle = null },
            onClear = {
                settlements = settlements - summary.index
                saveSettlements(context, settlements)
                settlingCycle = null
            },
            onSave = {
                settlements = settlements + (summary.index to it)
                saveSettlements(context, settlements)
                settlingCycle = null
            },
        )
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("导入备份") },
            text = { Text("导入会覆盖当前的设置、每日记录、电价覆盖和退补记录。确认继续吗？") },
            confirmButton = {
                Button(onClick = {
                    val result = runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                            decodeBackup(reader.readText())
                        } ?: error("无法读取备份文件")
                    }
                    val snapshot = result.getOrNull()
                    if (snapshot == null) {
                        backupMessage = "备份文件无效或版本不兼容。"
                    } else if (snapshot.isLegacyCsv) {
                        entries.clear()
                        entries.addAll(snapshot.entries)
                        saveEntries(context, entries)
                        backupMessage = "旧版 CSV 已导入，每日记录已恢复，其他设置保持不变。"
                    } else {
                        settings = snapshot.settings
                        entries.clear()
                        entries.addAll(snapshot.entries)
                        manualPrices = snapshot.manualPrices
                        settlements = snapshot.settlements
                        saveSettings(context, settings)
                        saveEntries(context, entries)
                        saveManualPrices(context, manualPrices)
                        saveSettlements(context, settlements)
                        backupMessage = "备份已恢复。"
                    }
                    pendingImportUri = null
                }) { Text("确认导入") }
            },
            dismissButton = { OutlinedButton(onClick = { pendingImportUri = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun BottomDock(screen: Screen, onScreen: (Screen) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Row(Modifier.padding(4.dp)) {
            NavItem(Modifier.weight(1f), screen == Screen.Home, { onScreen(Screen.Home) }, Icons.Default.Home, "概览")
            NavItem(Modifier.weight(1f), screen == Screen.Records, { onScreen(Screen.Records) }, Icons.Default.DateRange, "日历")
            NavItem(Modifier.weight(1f), screen == Screen.Settings, { onScreen(Screen.Settings) }, Icons.Default.Settings, "设置")
        }
    }
}

private fun Modifier.pressScale(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = tween(110),
        label = "press-scale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

@Composable
private fun ActionChoice(
    title: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().pressScale(onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp).size(20.dp), tint = MaterialTheme.colorScheme.onPrimary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NavItem(
    modifier: Modifier,
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(180),
        label = "navigation-selection",
    )
    Surface(
        modifier = modifier
            .padding(3.dp)
            .clip(RoundedCornerShape(16.dp))
            .pressScale(onClick),
        color = containerColor,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                label,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private data class OverviewWindow(
    val title: String,
    val start: LocalDate,
    val end: LocalDate,
    val totalKwh: Double,
    val difference: Double,
    val settled: Double,
    val reference: CycleSummary? = null,
)

private fun buildOverviewWindow(
    mode: OverviewMode,
    selectedCycle: Int?,
    current: CycleSummary,
    summaries: List<CycleSummary>,
    entries: List<UsageEntry>,
    settlements: Map<Int, Double>,
    rentDay: Int,
    today: LocalDate = LocalDate.now(),
): OverviewWindow {
    val selected = selectedCycle?.let { index -> summaries.firstOrNull { it.index == index } } ?: current
    val (title, start, end) = when (mode) {
        OverviewMode.Cycle -> Triple("计费周期", selected.startDate, selected.endDate)
        OverviewMode.MonthToDate -> Triple("本月至今", today.withDayOfMonth(1), today)
        OverviewMode.SinceRentDay -> {
            val rentDate = latestRentDate(today, rentDay)
            Triple("交租以来", rentDate, today)
        }
        OverviewMode.RentWindow -> {
            val rentDate = latestRentDate(today, rentDay)
            val nextRentDate = rentDateInMonth(YearMonth.from(rentDate).plusMonths(1), rentDay)
            Triple("交租周期", rentDate, nextRentDate.minusDays(1))
        }
        OverviewMode.All -> Triple("全部记录", summaries.firstOrNull()?.startDate ?: today, today)
    }
    val visibleEntries = entries.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
    val completeCycles = summaries.filter { !it.startDate.isBefore(start) && !it.endDate.isAfter(end) }
    val difference = if (mode == OverviewMode.Cycle) selected.difference else money(completeCycles.sumOf { it.difference })
    val settled = if (mode == OverviewMode.Cycle) {
        settlements[selected.index] ?: 0.0
    } else {
        money(completeCycles.sumOf { settlements[it.index] ?: 0.0 })
    }
    return OverviewWindow(
        title = title,
        start = start,
        end = end,
        totalKwh = twoDecimals(visibleEntries.sumOf { it.kwh }),
        difference = difference,
        settled = settled,
        reference = selected,
    )
}

private fun latestRentDate(date: LocalDate, rentDay: Int): LocalDate {
    val safeDay = rentDay.coerceIn(1, 31)
    val thisMonth = rentDateInMonth(YearMonth.from(date), safeDay)
    return if (thisMonth.isAfter(date)) thisMonth.minusMonths(1) else thisMonth
}

private fun rentDateInMonth(month: YearMonth, rentDay: Int): LocalDate =
    month.atDay(rentDay.coerceIn(1, 31).coerceAtMost(month.lengthOfMonth()))

@Composable
private fun HomeScreen(
    current: CycleSummary,
    summaries: List<CycleSummary>,
    manualPrices: Map<Int, Double>,
    settlements: Map<Int, Double>,
    settlementFocus: Int?,
    overviewMode: OverviewMode,
    rentDay: Int,
    onSettlementFocus: (Int?) -> Unit,
    onOverviewMode: (OverviewMode) -> Unit,
    onOverridePrice: (CycleSummary) -> Unit,
    onSettle: (CycleSummary) -> Unit,
    onQuickAdd: () -> Unit,
) {
    val window = buildOverviewWindow(
        mode = overviewMode,
        selectedCycle = settlementFocus,
        current = current,
        summaries = summaries,
        entries = summaries.flatMap { it.entries }.distinctBy { it.date },
        settlements = settlements,
        rentDay = rentDay,
    )
    val displaySummary = window.reference ?: current
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(Modifier.height(2.dp)) }
        item {
            SettlementScopePicker(
                summaries = summaries,
                selectedIndex = settlementFocus,
                mode = overviewMode,
                rentDay = rentDay,
                onMode = onOverviewMode,
                onSelected = onSettlementFocus,
            )
        }
        item {
            HeroCard(
                summary = displaySummary,
                scopeLabel = window.title,
                periodLabel = "${window.start} - ${window.end}",
                totalKwh = window.totalKwh,
                totalDifference = window.difference,
                totalSettled = window.settled,
            )
        }
        if (summaries.none { it.entries.isNotEmpty() }) {
            item { GettingStartedBanner(onQuickAdd) }
        }
        item { ThresholdCard(current.totalKwh) }
        item { SectionTitle("周期账单") }
        items(summaries.reversed(), key = { it.index }) { summary ->
            CycleCard(
                summary = summary,
                isOverridden = manualPrices.containsKey(summary.index),
                settled = settlements[summary.index],
                onOverridePrice = onOverridePrice,
                onSettle = onSettle,
            )
        }
        item { Spacer(Modifier.height(116.dp)) }
    }
}

@Composable
private fun GettingStartedBanner(onQuickAdd: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("从第一条用电记录开始", fontWeight = FontWeight.Bold)
                Text("录入每日用电量后，这里会自动计算周期电价和退补金额。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onQuickAdd) {
                Icon(Icons.Default.Add, contentDescription = "新增第一条记录", tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun SettlementScopePicker(
    summaries: List<CycleSummary>,
    selectedIndex: Int?,
    mode: OverviewMode,
    rentDay: Int,
    onMode: (OverviewMode) -> Unit,
    onSelected: (Int?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("查看范围", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ScopeChip("计费周期", mode == OverviewMode.Cycle) { onMode(OverviewMode.Cycle) }
            ScopeChip("本月至今", mode == OverviewMode.MonthToDate) { onMode(OverviewMode.MonthToDate) }
            ScopeChip("交租以来", mode == OverviewMode.SinceRentDay) { onMode(OverviewMode.SinceRentDay) }
            ScopeChip("交租周期", mode == OverviewMode.RentWindow) { onMode(OverviewMode.RentWindow) }
            ScopeChip("全部", mode == OverviewMode.All) { onMode(OverviewMode.All) }
        }
        if (mode == OverviewMode.Cycle) {
            Text("选择具体计费周期", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                summaries.reversed().forEach { summary ->
                    ScopeChip(
                        label = "${summary.startDate.monthValue}/${summary.startDate.dayOfMonth}",
                        selected = selectedIndex == summary.index,
                    ) { onSelected(summary.index) }
                }
            }
        } else {
            Text("交租日：每月 ${rentDay} 日", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ScopeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val chipColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        animationSpec = tween(160),
        label = "scope-chip",
    )
    Surface(
        modifier = Modifier.pressScale(onClick),
        shape = RoundedCornerShape(10.dp),
        color = chipColor,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f)),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun HeroCard(
    summary: CycleSummary,
    scopeLabel: String,
    periodLabel: String,
    totalKwh: Double,
    totalDifference: Double,
    totalSettled: Double,
) {
    val remaining = money(totalDifference - totalSettled)
    val displayedKwh by animateFloatAsState(
        targetValue = totalKwh.toFloat(),
        animationSpec = tween(420),
        label = "overview-usage",
    )
    val progress by animateFloatAsState(
        targetValue = (totalKwh / 120.0).coerceIn(0.0, 1.0).toFloat(),
        animationSpec = tween(420),
        label = "usage-threshold-progress",
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = Color(0xFF171A1F),
        shadowElevation = 10.dp,
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(end = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(scopeLabel, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(periodLabel, color = Color.White.copy(alpha = 0.66f), style = MaterialTheme.typography.bodySmall)
                }
                Surface(shape = RoundedCornerShape(10.dp), color = Color.White.copy(alpha = 0.1f)) {
                    Text("电费账本", modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                BoxWithConstraints {
                    Text(
                        "${fmt(displayedKwh.toDouble())}",
                        color = Color.White,
                        fontSize = when {
                            maxWidth < 260.dp -> 42.sp
                            maxWidth < 320.dp -> 48.sp
                            else -> 54.sp
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                    )
                }
                Text("本段累计用电 · 度", color = Color.White.copy(alpha = 0.66f), style = MaterialTheme.typography.bodyMedium)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = Color.White.copy(alpha = 0.16f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassMetric("执行", "${fmt(summary.expectedPrice)} / 度", Modifier.weight(1f))
                GlassMetric("实扣", "${fmt(summary.actualPrice)} / 度", Modifier.weight(1f))
            }
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.1f)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(if (totalDifference >= 0) "预计应退" else "预计应补", color = Color.White.copy(alpha = 0.66f), style = MaterialTheme.typography.labelSmall)
                        Text("${fmt(abs(totalDifference))} 元", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (remaining >= 0) "尚未处理" else "已多处理", color = Color.White.copy(alpha = 0.66f), style = MaterialTheme.typography.labelSmall)
                        Text("${fmt(abs(remaining))} 元", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordsScreen(
    entries: List<UsageEntry>,
    selectedDate: LocalDate,
    onSelectedDate: (LocalDate) -> Unit,
    onEdit: (UsageEntry) -> Unit,
    onDelete: (UsageEntry) -> Unit,
    onAddForDate: (LocalDate) -> Unit,
) {
    var mode by remember { mutableStateOf(RecordMode.Month) }
    var anchor by remember(selectedDate) { mutableStateOf(selectedDate) }
    val byDate = entries.associateBy { it.date }
    val visible = visibleEntries(mode, anchor, entries).sortedByDescending { it.date.toEpochDay() }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(Modifier.height(2.dp)) }
        item {
            ModeSelector(mode) { mode = it }
        }
        item {
            CalendarHeader(
                title = calendarTitle(mode, anchor),
                onPrev = {
                    val previous = moveAnchor(anchor, mode, -1)
                    anchor = previous
                    onSelectedDate(previous)
                },
                onNext = {
                    val next = moveAnchor(anchor, mode, 1)
                    anchor = next
                    onSelectedDate(next)
                },
            )
        }
        item {
            AnimatedContent(
                targetState = Triple(mode, anchor, selectedDate),
                transitionSpec = {
                    val direction = if (targetState.second.isAfter(initialState.second)) 1 else -1
                    (fadeIn(tween(180)) + slideInHorizontally(tween(200)) { direction * it / 36 }) togetherWith
                        (fadeOut(tween(100)) + slideOutHorizontally(tween(140)) { -direction * it / 42 })
                },
                label = "calendar-period",
            ) { (displayMode, displayAnchor, displaySelection) ->
                when (displayMode) {
                    RecordMode.Day -> DayPanel(displayAnchor, byDate[displayAnchor], onAddForDate, onEdit)
                    RecordMode.Week -> WeekPanel(displayAnchor, byDate, displaySelection, onSelectedDate)
                    RecordMode.Month -> MonthPanel(displayAnchor, byDate, displaySelection, onSelectedDate)
                    RecordMode.Year -> YearPanel(displayAnchor, entries) { month ->
                        anchor = month.atDay(1)
                        onSelectedDate(month.atDay(1))
                        mode = RecordMode.Month
                    }
                }
            }
        }
        if (mode == RecordMode.Week || mode == RecordMode.Month) {
            item { SelectedDatePanel(selectedDate, byDate[selectedDate], onAddForDate) }
        }
        item {
            RecordsSummary(visible)
        }
        if (visible.isEmpty() && mode == RecordMode.Year) {
            item { EmptyRecordsState(selectedDate, onAddForDate) }
        }
        items(visible, key = { it.date.toString() }) {
            EntryRow(it, onEdit, onDelete)
        }
        item { Spacer(Modifier.height(116.dp)) }
    }
}

@Composable
private fun SettingsScreen(
    settings: AppSettings,
    entries: List<UsageEntry>,
    backupMessage: String?,
    onSave: (AppSettings) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var startDate by remember(settings) { mutableStateOf(settings.cycleStartDate.toString()) }
    var initialPrice by remember(settings) { mutableStateOf(fmt(settings.initialPrice)) }
    var actualPrice by remember(settings) { mutableStateOf(fmt(settings.actualPrice)) }
    var rentDay by remember(settings) { mutableStateOf(settings.rentDay) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("设置会立即影响所有周期计算。") }
    var dataMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(Modifier.height(2.dp)) }
        item {
            ModernCard {
                Text("计费规则", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                DatePickerField("周期开始日期", startDate) { showStartDatePicker = true }
                RentDayPicker(rentDay, onChange = { rentDay = it })
                SettingField(initialPrice, { initialPrice = it }, "首个周期电价")
                SettingField(actualPrice, { actualPrice = it }, "公寓实际扣费单价")
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val date = runCatching { LocalDate.parse(startDate) }.getOrNull()
                        val initial = initialPrice.toDoubleOrNull()
                        val actual = actualPrice.toDoubleOrNull()
                        if (date == null || initial == null || actual == null || initial < 0.0 || actual < 0.0) {
                            message = "日期和单价须为有效的非负数值。"
                        } else {
                            onSave(AppSettings(date, twoDecimals(initial), twoDecimals(actual), rentDay))
                            message = "已保存。"
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                ) { Text("保存计费设置") }
                AnimatedContent(targetState = message, label = "settings-feedback") { status ->
                    Text(
                        status,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            status == "已保存。" -> MaterialTheme.colorScheme.secondary
                            status.contains("须为") -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
        item {
            ModernCard {
                Text("阶梯规则", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                RuleLine("0-60 度", "下周期 0.80 元/度")
                RuleLine("60.01-120 度", "下周期 1.00 元/度")
                RuleLine("超过 120 度", "下周期 1.20 元/度")
            }
        }
        item {
            ModernCard {
                Text("数据", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("每日记录可复制为 CSV，粘贴到 Excel 或备忘录。")
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(entriesToCsv(entries)))
                        dataMessage = "CSV 已复制到剪贴板。"
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("复制 CSV") }
                dataMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
                Text("系统会尝试自动备份应用数据；换手机或更换签名时，建议同时导出一份完整备份。", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onExportBackup, modifier = Modifier.weight(1f)) { Text("导出完整备份") }
                    OutlinedButton(onClick = onImportBackup, modifier = Modifier.weight(1f)) { Text("导入完整备份") }
                }
                backupMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
            }
        }
        item { Spacer(Modifier.height(116.dp)) }
    }

    if (showStartDatePicker) {
        DateSelectDialog(
            initialDate = runCatching { LocalDate.parse(startDate) }.getOrNull() ?: LocalDate.now(),
            onDismiss = { showStartDatePicker = false },
            onSelected = {
                startDate = it.toString()
                showStartDatePicker = false
            },
        )
    }
}

@Composable
private fun ModeSelector(mode: RecordMode, onMode: (RecordMode) -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ModeButton("日", mode == RecordMode.Day, Modifier.weight(1f)) { onMode(RecordMode.Day) }
            ModeButton("周", mode == RecordMode.Week, Modifier.weight(1f)) { onMode(RecordMode.Week) }
            ModeButton("月", mode == RecordMode.Month, Modifier.weight(1f)) { onMode(RecordMode.Month) }
            ModeButton("年", mode == RecordMode.Year, Modifier.weight(1f)) { onMode(RecordMode.Year) }
        }
    }
}

@Composable
private fun ModeButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(160),
        label = "record-range-tab",
    )
    Surface(
        modifier = modifier.pressScale(onClick),
        shape = RoundedCornerShape(13.dp),
        color = backgroundColor,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 10.dp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CalendarHeader(title: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Surface(shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))) {
            IconButton(onClick = onPrev) { Icon(Icons.Default.ChevronLeft, contentDescription = "上一个") }
        }
        AnimatedContent(
            targetState = title,
            transitionSpec = {
                (fadeIn(tween(140)) + slideInVertically(tween(160)) { it / 8 }) togetherWith fadeOut(tween(90))
            },
            label = "calendar-title",
        ) { period ->
            Text(period, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, fontFamily = FontFamily.Monospace)
        }
        Surface(shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))) {
            IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, contentDescription = "下一个") }
        }
    }
}

@Composable
private fun DayPanel(date: LocalDate, entry: UsageEntry?, onAdd: (LocalDate) -> Unit, onEdit: (UsageEntry) -> Unit) {
    ModernCard {
        Text(date.toString(), fontWeight = FontWeight.Bold, fontSize = 24.sp)
        if (entry == null) {
            Text("这一天还没有记录。")
            Button(onClick = { onAdd(date) }, modifier = Modifier.fillMaxWidth()) { Text("录入这一天") }
        } else {
            Text("${fmt(entry.kwh)} 度", fontWeight = FontWeight.Bold, fontSize = 34.sp)
            Button(onClick = { onEdit(entry) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("编辑当天记录") }
        }
    }
}

@Composable
private fun SelectedDatePanel(date: LocalDate, entry: UsageEntry?, onAdd: (LocalDate) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("${date.monthValue}月${date.dayOfMonth}日 · ${weekdayLabel(date)}", fontWeight = FontWeight.Bold)
                Text(
                    entry?.let { "已记录 ${fmt(it.kwh)} 度" } ?: "尚无记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = { onAdd(date) }, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (entry == null) "录入" else "编辑")
            }
        }
    }
}

@Composable
private fun EmptyRecordsState(date: LocalDate, onAdd: (LocalDate) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("这段时间还没有记录", fontWeight = FontWeight.Bold)
        Text("从选中的日期开始补上用电量", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { onAdd(date) }) { Text("录入 ${date.monthValue}月${date.dayOfMonth}日") }
    }
}

private fun weekdayLabel(date: LocalDate): String =
    listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")[date.dayOfWeek.value - 1]

@Composable
private fun WeekPanel(anchor: LocalDate, byDate: Map<LocalDate, UsageEntry>, selectedDate: LocalDate, onSelectedDate: (LocalDate) -> Unit) {
    val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    ModernCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..6).forEach { offset ->
                val date = start.plusDays(offset.toLong())
                DayTile(date, byDate[date], Modifier.weight(1f), selected = date == selectedDate) {
                    onSelectedDate(date)
                }
            }
        }
    }
}

@Composable
private fun MonthPanel(anchor: LocalDate, byDate: Map<LocalDate, UsageEntry>, selectedDate: LocalDate, onSelectedDate: (LocalDate) -> Unit) {
    val month = YearMonth.from(anchor)
    val first = month.atDay(1)
    val gridStart = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    ModernCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf<String>("一", "二", "三", "四", "五", "六", "日").forEach { dayName: String ->
                Text(dayName, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        (0..5).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0..6).forEach { day ->
                    val date = gridStart.plusDays((week * 7 + day).toLong())
                    val inMonth = YearMonth.from(date) == month
                    DayTile(date, byDate[date], Modifier.weight(1f), muted = !inMonth, selected = date == selectedDate) {
                        onSelectedDate(date)
                    }
                }
            }
        }
    }
}

@Composable
private fun YearPanel(anchor: LocalDate, entries: List<UsageEntry>, onMonth: (YearMonth) -> Unit) {
    val year = anchor.year
    val byMonth = entries.groupBy { YearMonth.from(it.date) }
    ModernCard {
        (0..3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..3).forEach { col ->
                    val month = YearMonth.of(year, row * 3 + col)
                    val total = byMonth[month].orEmpty().sumOf { it.kwh }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .pressScale { onMonth(month) },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${month.monthValue} 月", fontWeight = FontWeight.Bold)
                            Text("${fmt(total)} 度", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayTile(date: LocalDate, entry: UsageEntry?, modifier: Modifier, muted: Boolean = false, selected: Boolean = false, onClick: () -> Unit) {
    val filled = entry != null
    val tileColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary
            filled -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (muted) 0.42f else 1f)
        },
        animationSpec = tween(160),
        label = "calendar-day-selection",
    )
    Surface(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .pressScale(onClick),
        shape = RoundedCornerShape(12.dp),
        color = tileColor,
    ) {
        Column(Modifier.padding(7.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                date.dayOfMonth.toString(),
                fontWeight = FontWeight.Bold,
                color = when {
                    selected -> MaterialTheme.colorScheme.onPrimary
                    filled -> MaterialTheme.colorScheme.onSecondaryContainer
                    muted -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    else -> Color.Unspecified
                },
            )
            if (entry != null) {
                Text("${fmt(entry.kwh)}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer)
            } else {
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f))
                )
            }
        }
    }
}

@Composable
private fun RecordsSummary(entries: List<UsageEntry>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SoftMetric("记录", "${entries.size} 天", Modifier.weight(1f))
        SoftMetric("用电", "${fmt(entries.sumOf { it.kwh })} 度", Modifier.weight(1f))
    }
}

@Composable
private fun CycleCard(
    summary: CycleSummary,
    isOverridden: Boolean,
    settled: Double?,
    onOverridePrice: (CycleSummary) -> Unit,
    onSettle: (CycleSummary) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val remaining = settled?.let { money(summary.difference - it) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(tween(220))
            .pressScale { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("${summary.startDate.monthValue} 月 ${summary.startDate.dayOfMonth} 日 - ${summary.endDate.monthValue} 月 ${summary.endDate.dayOfMonth} 日", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(
                        buildString {
                            append(if (summary.isComplete) "记录完整" else "缺 ${summary.missingDates.size} 天")
                            if (isOverridden) append(" · 已改电价")
                            if (settled != null) append(if (settled >= 0.0) " · 已退" else " · 已补")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (summary.difference >= 0) "应退" else "应补", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (summary.difference >= 0) "+${fmt(summary.difference)}" else "-${fmt(abs(summary.difference))}",
                        color = amountColor(summary.difference),
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 20.sp,
                    )
                }
            }
            Row(Modifier.fillMaxWidth()) {
                LedgerMetric("用电", "${fmt(summary.totalKwh)} 度", Modifier.weight(1.2f))
                LedgerMetric("执行", "${fmt(summary.expectedPrice)}", Modifier.weight(1f))
                LedgerMetric("实扣", "${fmt(summary.actualPrice)}", Modifier.weight(1f))
            }
            if (remaining != null) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("已处理 ${fmt(abs(settled))} 元 · 剩余 ${fmt(abs(remaining))} 元", modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (expanded) "收起明细" else "查看明细", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Row {
                    TextButton(onClick = { onOverridePrice(summary) }) { Text("调整电价") }
                    TextButton(onClick = { onSettle(summary) }) { Text("登记结算") }
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(150)) + expandVertically(tween(210)),
                exit = fadeOut(tween(90)) + shrinkVertically(tween(140)),
            ) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        summary.entries.forEach {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(it.date.toString(), style = MaterialTheme.typography.bodySmall)
                                Text("${fmt(it.kwh)} 度", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (summary.entries.isEmpty()) Text("本周期还没有记录。", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
    }
}

@Composable
private fun EntryRow(entry: UsageEntry, onEdit: (UsageEntry) -> Unit, onDelete: (UsageEntry) -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("${entry.date.monthValue}月${entry.date.dayOfMonth}日 · ${weekdayLabel(entry.date)}", fontWeight = FontWeight.Bold)
                    Text("每日用电", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Text("${fmt(entry.kwh)} 度", fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                Box {
                    IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = "记录操作") }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("编辑") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = { menuExpanded = false; onEdit(entry) },
                        )
                        DropdownMenuItem(
                            text = { Text("删除") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = { menuExpanded = false; confirmDelete = true },
                        )
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除这条记录？") },
            text = { Text("${entry.date} 的 ${fmt(entry.kwh)} 度用电记录将被删除。") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(entry) }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun ThresholdCard(totalKwh: Double) {
    val nextLimit = when {
        totalKwh <= 60.0 -> 60.0
        totalKwh <= 120.0 -> 120.0
        else -> null
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.padding(8.dp).size(18.dp), tint = MaterialTheme.colorScheme.onSecondary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("下一周期电价提醒", fontWeight = FontWeight.Bold)
                Text(
                    if (nextLimit == null) "当前已超过 120 度，下周期预计按最高档。"
                    else "距 ${fmt(nextLimit)} 度门槛还剩 ${fmt(nextLimit - totalKwh)} 度。",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ModernCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

@Composable
private fun GlassMetric(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.08f),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Color.White.copy(alpha = 0.62f), fontSize = 11.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun SoftMetric(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(value, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text.uppercase(), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun SettingField(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Decimal,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
    )
}

@Composable
private fun DatePickerField(label: String, value: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun RentDayPicker(rentDay: Int, onChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = Alignment.Start) {
                Text("交租日", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("每月 ${rentDay} 日", style = MaterialTheme.typography.bodyLarge)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (1..31).forEach { day ->
                DropdownMenuItem(
                    text = { Text("每月 $day 日") },
                    onClick = {
                        onChange(day)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelectDialog(initialDate: LocalDate, onDismiss: () -> Unit, onSelected: (LocalDate) -> Unit) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate.toUtcMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { onSelected(it.toLocalDateUtc()) } ?: onDismiss()
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
private fun RuleLine(left: String, right: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(left, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(right, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EntryDialog(date: LocalDate, entry: UsageEntry?, onDismiss: () -> Unit, onSave: (UsageEntry) -> Unit) {
    var dateText by remember(date, entry) { mutableStateOf(entry?.date?.toString() ?: date.toString()) }
    var kwh by remember(entry) { mutableStateOf(entry?.kwh?.let { fmt(it) } ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "录入用电量" else "编辑用电量") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DatePickerField("日期", dateText) { showDatePicker = true }
                OutlinedTextField(
                    value = kwh,
                    onValueChange = { kwh = it },
                    label = { Text("用电量（度）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsedDate = runCatching { LocalDate.parse(dateText) }.getOrNull()
                val parsedKwh = kwh.toDoubleOrNull()
                if (parsedDate == null || parsedKwh == null || parsedKwh < 0.0) {
                    error = "请输入正确日期和非负电量。"
                } else {
                    onSave(UsageEntry(parsedDate, twoDecimals(parsedKwh)))
                }
            }) { Text("保存") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("取消") } },
    )

    if (showDatePicker) {
        DateSelectDialog(
            initialDate = runCatching { LocalDate.parse(dateText) }.getOrNull() ?: date,
            onDismiss = { showDatePicker = false },
            onSelected = {
                dateText = it.toString()
                showDatePicker = false
            },
        )
    }
}

@Composable
private fun BatchDialog(onDismiss: () -> Unit, onImport: (BatchParseResult) -> Unit) {
    var text by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批量录入") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("每行：2026-08-01 12.34") },
                    minLines = 6,
                    shape = RoundedCornerShape(14.dp),
                )
                message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val result = parseBatchEntries(text)
                if (result.entries.isEmpty() || result.errors.isNotEmpty()) {
                    message = result.errors.joinToString("\n").ifBlank { "没有可导入的数据。" }
                } else {
                    onImport(result)
                }
            }) { Text("导入") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun PriceOverrideDialog(
    summary: CycleSummary,
    isOverridden: Boolean,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onSave: (Double) -> Unit,
) {
    var price by remember(summary) { mutableStateOf(fmt(summary.expectedPrice)) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("覆盖周期电价") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${summary.startDate} 至 ${summary.endDate}")
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("应执行电价") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = price.toDoubleOrNull()
                if (parsed == null || parsed < 0.0) error = "请输入正确电价。" else onSave(twoDecimals(parsed))
            }) { Text("保存") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isOverridden) OutlinedButton(onClick = onClear) { Text("恢复自动") }
                OutlinedButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}

@Composable
private fun SettlementDialog(
    summary: CycleSummary,
    currentValue: Double?,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onSave: (Double) -> Unit,
) {
    var amount by remember(summary, currentValue) { mutableStateOf(fmt(currentValue ?: summary.difference)) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("登记实际退补") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("默认填入本周期计算差额：${fmt(summary.difference)} 元")
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("实际退补金额") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                Text("正数表示已退，负数表示已补。", style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = amount.toDoubleOrNull()
                if (parsed == null) error = "请输入正确金额。" else onSave(money(parsed))
            }) { Text("保存") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentValue != null) OutlinedButton(onClick = onClear) { Text("清除") }
                OutlinedButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}

private fun calendarTitle(mode: RecordMode, anchor: LocalDate): String {
    return when (mode) {
        RecordMode.Day -> anchor.toString()
        RecordMode.Week -> {
            val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            "${start} - ${start.plusDays(6)}"
        }
        RecordMode.Month -> "${anchor.year} 年 ${anchor.monthValue} 月"
        RecordMode.Year -> "${anchor.year} 年"
    }
}

private fun moveAnchor(anchor: LocalDate, mode: RecordMode, amount: Long): LocalDate {
    return when (mode) {
        RecordMode.Day -> anchor.plusDays(amount)
        RecordMode.Week -> anchor.plusWeeks(amount)
        RecordMode.Month -> anchor.plusMonths(amount)
        RecordMode.Year -> anchor.plusYears(amount)
    }
}

private fun visibleEntries(mode: RecordMode, anchor: LocalDate, entries: List<UsageEntry>): List<UsageEntry> {
    return entries.filter {
        when (mode) {
            RecordMode.Day -> it.date == anchor
            RecordMode.Week -> {
                val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                !it.date.isBefore(start) && !it.date.isAfter(start.plusDays(6))
            }
            RecordMode.Month -> YearMonth.from(it.date) == YearMonth.from(anchor)
            RecordMode.Year -> it.date.year == anchor.year
        }
    }
}

private fun amountColor(amount: Double): Color {
    return if (amount >= 0) Color(0xFF059669) else Color(0xFFDC2626)
}

private fun loadSettings(context: Context): AppSettings {
    val prefs = prefs(context)
    return AppSettings(
        cycleStartDate = runCatching {
            prefs.getString("cycleStartDate", null)?.let { LocalDate.parse(it) }
        }.getOrNull() ?: LocalDate.now().minusDays(14),
        initialPrice = readDouble(prefs, "initialPrice", 1.2),
        actualPrice = readDouble(prefs, "actualPrice", 1.2),
        rentDay = prefs.getInt("rentDay", 1).coerceIn(1, 31),
    )
}

private fun saveSettings(context: Context, settings: AppSettings) {
    prefs(context).edit()
        .putString("cycleStartDate", settings.cycleStartDate.toString())
        .putString("initialPrice", fmt(settings.initialPrice))
        .putString("actualPrice", fmt(settings.actualPrice))
        .putInt("rentDay", settings.rentDay.coerceIn(1, 31))
        .apply()
}

private fun loadEntries(context: Context): List<UsageEntry> {
    return prefs(context).getString("entries", "").orEmpty().lineSequence().mapNotNull { line ->
        val parts = line.split('\t')
        val date = parts.getOrNull(0)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val kwh = parts.getOrNull(1)?.toDoubleOrNull()
        if (date != null && kwh != null) UsageEntry(date, kwh) else null
    }.sortedBy { it.date.toEpochDay() }.toList()
}

private fun saveEntries(context: Context, entries: List<UsageEntry>) {
    val raw = entries.sortedBy { it.date.toEpochDay() }.joinToString("\n") { "${it.date}\t${fmt(it.kwh)}" }
    prefs(context).edit().putString("entries", raw).apply()
}

private fun loadManualPrices(context: Context): Map<Int, Double> = loadIndexedDoubles(context, "manualPrices")

private fun saveManualPrices(context: Context, prices: Map<Int, Double>) = saveIndexedDoubles(context, "manualPrices", prices)

private fun loadSettlements(context: Context): Map<Int, Double> = loadIndexedDoubles(context, "settlements")

private fun saveSettlements(context: Context, prices: Map<Int, Double>) = saveIndexedDoubles(context, "settlements", prices)

private fun loadIndexedDoubles(context: Context, key: String): Map<Int, Double> {
    return prefs(context).getString(key, "").orEmpty().lineSequence().mapNotNull { line ->
        val parts = line.split('\t')
        val index = parts.getOrNull(0)?.toIntOrNull()
        val value = parts.getOrNull(1)?.toDoubleOrNull()
        if (index != null && value != null) index to twoDecimals(value) else null
    }.toMap()
}

private fun saveIndexedDoubles(context: Context, key: String, values: Map<Int, Double>) {
    val raw = values.toSortedMap().entries.joinToString("\n") { "${it.key}\t${fmt(it.value)}" }
    prefs(context).edit().putString(key, raw).apply()
}

private fun entriesToCsv(entries: List<UsageEntry>): String {
    return buildString {
        appendLine("date,kwh")
        entries.sortedBy { it.date.toEpochDay() }.forEach {
            appendLine("${it.date},${fmt(it.kwh)}")
        }
    }
}

private fun readDouble(prefs: SharedPreferences, key: String, defaultValue: Double): Double {
    return when (val value = prefs.all[key]) {
        is String -> value.toDoubleOrNull() ?: defaultValue
        is Float -> twoDecimals(value.toDouble())
        is Double -> twoDecimals(value)
        is Int -> value.toDouble()
        else -> defaultValue
    }
}

private fun prefs(context: Context): SharedPreferences =
    context.getSharedPreferences("electricity", Context.MODE_PRIVATE)

private fun fmt(value: Double): String = String.format(Locale.US, "%.2f", value)

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDateUtc(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
