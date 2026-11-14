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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
            primary = Color(0xFFF2F4F5),
            onPrimary = Color(0xFF17191C),
            primaryContainer = Color(0xFF30343A),
            onPrimaryContainer = Color(0xFFF2F4F5),
            secondary = Color(0xFFE0A27E),
            secondaryContainer = Color(0xFF4B3025),
            surface = Color(0xFF151719),
            surfaceVariant = Color(0xFF26292D),
            background = Color(0xFF0D0F10),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF202428),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE4E7E9),
            onPrimaryContainer = Color(0xFF202428),
            secondary = Color(0xFFB85C36),
            secondaryContainer = Color(0xFFF5E1D7),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFF0F1F2),
            background = Color(0xFFF6F6F4),
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
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
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            when (screen) {
                                Screen.Home -> "概览"
                                Screen.Records -> "记录"
                                Screen.Settings -> "设置"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                        )
                        Text("电费记录", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {},
            )
        },
        bottomBar = {
            NavigationBar {
                Row(Modifier.fillMaxWidth()) {
                    NavItem(Modifier.weight(1f), screen == Screen.Home, { screen = Screen.Home }, Icons.Default.Home, "概览")
                    NavItem(Modifier.weight(1f), screen == Screen.Records, { screen = Screen.Records }, Icons.Default.List, "日历")
                    NavItem(Modifier.weight(1f), screen == Screen.Settings, { screen = Screen.Settings }, Icons.Default.Settings, "设置")
                }
            }
        },
        floatingActionButton = {
            if (screen != Screen.Settings) {
                ExtendedFloatingActionButton(
                    onClick = { showAddSheet = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("新增记录") },
                )
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
        ) {
            when (screen) {
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
            Text("新增记录", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            ListItem(
                headlineContent = { Text("单日录入") },
                supportingContent = { Text("为某一天添加或修改用电量") },
                leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                modifier = Modifier.clickable {
                    showAddSheet = false
                    editingEntry = entries.firstOrNull { it.date == selectedDate }
                    showEntryDialog = true
                },
            )
            ListItem(
                headlineContent = { Text("批量录入") },
                supportingContent = { Text("粘贴多行日期和用电量") },
                leadingContent = { Icon(Icons.Default.Upload, contentDescription = null) },
                modifier = Modifier.clickable {
                    showAddSheet = false
                    showBatchDialog = true
                },
            )
            Spacer(Modifier.height(24.dp))
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
private fun NavItem(
    modifier: Modifier,
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                label,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
        item { Spacer(Modifier.height(96.dp)) }
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
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 0.dp else 1.dp,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            Modifier
                .background(MaterialTheme.colorScheme.primary)
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(scopeLabel, color = Color.White.copy(alpha = 0.82f), fontWeight = FontWeight.Medium)
                    Text(periodLabel, color = Color.White.copy(alpha = 0.88f))
                }
                Text("${fmt(totalKwh)} 度", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(
                    progress = (totalKwh / 120.0).coerceIn(0.0, 1.0).toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.24f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassMetric("执行电价", "${fmt(summary.expectedPrice)} 元/度", Modifier.weight(1f))
                    GlassMetric("实际扣费", "${fmt(summary.actualPrice)} 元/度", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassMetric(if (totalDifference >= 0) "应退合计" else "应补合计", "${fmt(abs(totalDifference))} 元", Modifier.weight(1f))
                    GlassMetric(if (remaining >= 0) "未处理" else "多处理", "${fmt(abs(remaining))} 元", Modifier.weight(1f))
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

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(Modifier.height(2.dp)) }
        item {
            ModeSelector(mode) { mode = it }
        }
        item {
            CalendarHeader(
                title = calendarTitle(mode, anchor),
                onPrev = { anchor = moveAnchor(anchor, mode, -1) },
                onNext = { anchor = moveAnchor(anchor, mode, 1) },
            )
        }
        item {
            when (mode) {
                RecordMode.Day -> DayPanel(anchor, byDate[anchor], onAddForDate, onEdit, onDelete)
                RecordMode.Week -> WeekPanel(anchor, byDate, onSelectedDate, onAddForDate)
                RecordMode.Month -> MonthPanel(anchor, byDate, onSelectedDate, onAddForDate)
                RecordMode.Year -> YearPanel(anchor, entries) { month ->
                    anchor = month.atDay(1)
                    mode = RecordMode.Month
                }
            }
        }
        item {
            val visible = visibleEntries(mode, anchor, entries)
            RecordsSummary(visible)
        }
        items(visibleEntries(mode, anchor, entries).sortedByDescending { it.date.toEpochDay() }, key = { it.date.toString() }) {
            EntryRow(it, onEdit, onDelete)
        }
        item { Spacer(Modifier.height(96.dp)) }
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

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                        if (date == null || initial == null || actual == null) {
                            message = "请检查日期和价格格式。"
                        } else {
                            onSave(AppSettings(date, twoDecimals(initial), twoDecimals(actual), rentDay))
                            message = "已保存。"
                        }
                    },
                ) { Text("保存设置") }
                Text(message, style = MaterialTheme.typography.bodySmall)
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
                        message = "CSV 已复制。"
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("复制 CSV") }
                Text("系统会尝试自动备份应用数据；换手机或更换签名时，建议同时导出一份完整备份。", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onExportBackup, modifier = Modifier.weight(1f)) { Text("导出完整备份") }
                    OutlinedButton(onClick = onImportBackup, modifier = Modifier.weight(1f)) { Text("导入完整备份") }
                }
                backupMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
            }
        }
        item { Spacer(Modifier.height(96.dp)) }
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
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ModeButton("日", mode == RecordMode.Day, Modifier.weight(1f)) { onMode(RecordMode.Day) }
            ModeButton("周", mode == RecordMode.Week, Modifier.weight(1f)) { onMode(RecordMode.Week) }
            ModeButton("月", mode == RecordMode.Month, Modifier.weight(1f)) { onMode(RecordMode.Month) }
            ModeButton("年", mode == RecordMode.Year, Modifier.weight(1f)) { onMode(RecordMode.Year) }
        }
    }
}

@Composable
private fun ModeButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 10.dp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CalendarHeader(title: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = onPrev) { Icon(Icons.Default.ChevronLeft, contentDescription = "上一个") }
        Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, contentDescription = "下一个") }
    }
}

@Composable
private fun DayPanel(date: LocalDate, entry: UsageEntry?, onAdd: (LocalDate) -> Unit, onEdit: (UsageEntry) -> Unit, onDelete: (UsageEntry) -> Unit) {
    ModernCard {
        Text(date.toString(), fontWeight = FontWeight.Bold, fontSize = 24.sp)
        if (entry == null) {
            Text("这一天还没有记录。")
            Button(onClick = { onAdd(date) }, modifier = Modifier.fillMaxWidth()) { Text("录入这一天") }
        } else {
            Text("${fmt(entry.kwh)} 度", fontWeight = FontWeight.Bold, fontSize = 34.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { onEdit(entry) }, modifier = Modifier.weight(1f)) { Text("编辑") }
                OutlinedButton(onClick = { onDelete(entry) }, modifier = Modifier.weight(1f)) { Text("删除") }
            }
        }
    }
}

@Composable
private fun WeekPanel(anchor: LocalDate, byDate: Map<LocalDate, UsageEntry>, onSelectedDate: (LocalDate) -> Unit, onAdd: (LocalDate) -> Unit) {
    val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    ModernCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..6).forEach { offset ->
                val date = start.plusDays(offset.toLong())
                DayTile(date, byDate[date], Modifier.weight(1f)) {
                    onSelectedDate(date)
                    onAdd(date)
                }
            }
        }
    }
}

@Composable
private fun MonthPanel(anchor: LocalDate, byDate: Map<LocalDate, UsageEntry>, onSelectedDate: (LocalDate) -> Unit, onAdd: (LocalDate) -> Unit) {
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
                    DayTile(date, byDate[date], Modifier.weight(1f), muted = !inMonth) {
                        onSelectedDate(date)
                        onAdd(date)
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
                            .clickable { onMonth(month) },
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
private fun DayTile(date: LocalDate, entry: UsageEntry?, modifier: Modifier, muted: Boolean = false, onClick: () -> Unit) {
    val filled = entry != null
    Surface(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = when {
            filled -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (muted) 0.42f else 1f)
        },
    ) {
        Column(Modifier.padding(7.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                date.dayOfMonth.toString(),
                fontWeight = FontWeight.Bold,
                color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f) else Color.Unspecified,
            )
            if (entry != null) {
                Text("${fmt(entry.kwh)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("${summary.startDate} - ${summary.endDate}", fontWeight = FontWeight.Bold)
                    Text(
                        buildString {
                            append(if (summary.isComplete) "数据完整" else "缺 ${summary.missingDates.size} 天")
                            if (isOverridden) append(" · 手动电价")
                            if (settled != null) append(if (settled >= 0.0) " · 已登记退还" else " · 已登记补缴")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    if (summary.difference >= 0) "+${fmt(summary.difference)}" else "-${fmt(abs(summary.difference))}",
                    color = amountColor(summary.difference),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoftMetric("用电", "${fmt(summary.totalKwh)} 度", Modifier.weight(1f))
                SoftMetric("应执行", "${fmt(summary.expectedPrice)}", Modifier.weight(1f))
                SoftMetric("实扣", "${fmt(summary.actualPrice)}", Modifier.weight(1f))
            }
            if (remaining != null) {
                Text(
                    "已处理 ${fmt(abs(settled))} 元，剩余 ${fmt(abs(remaining))} 元",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onOverridePrice(summary) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("电价")
                }
                Button(onClick = { onSettle(summary) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("结算")
                }
            }
            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    summary.entries.forEach {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(it.date.toString())
                            Text("${fmt(it.kwh)} 度")
                        }
                    }
                    if (summary.entries.isEmpty()) Text("本周期还没有记录。")
                }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: UsageEntry, onEdit: (UsageEntry) -> Unit, onDelete: (UsageEntry) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(entry.date.toString(), fontWeight = FontWeight.Bold)
                Text("${fmt(entry.kwh)} 度", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row {
                IconButton(onClick = { onEdit(entry) }) { Icon(Icons.Default.Edit, contentDescription = "编辑") }
                IconButton(onClick = { onDelete(entry) }) { Icon(Icons.Default.Delete, contentDescription = "删除") }
            }
        }
    }
}

@Composable
private fun ThresholdCard(totalKwh: Double) {
    val nextLimit = when {
        totalKwh <= 60.0 -> 60.0
        totalKwh <= 120.0 -> 120.0
        else -> null
    }
    ModernCard {
        Text("阈值提醒", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("60 度和 120 度会决定下一个 15 天周期电价。")
        Text(
            if (nextLimit == null) "当前周期已超过 120 度，下周期预计按最高档。"
            else "距离 ${fmt(nextLimit)} 度还剩 ${fmt(nextLimit - totalKwh)} 度。",
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ModernCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun GlassMetric(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.16f),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SoftMetric(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(top = 4.dp))
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
