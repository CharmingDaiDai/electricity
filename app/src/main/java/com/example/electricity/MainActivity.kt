package com.example.electricity

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.util.Locale

private enum class Screen {
    Home,
    Records,
    Settings,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ElectricityTheme {
                ElectricityApp()
            }
        }
    }
}

@Composable
private fun ElectricityTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = Color(0xFF5EEAD4),
            primaryContainer = Color(0xFF134E4A),
            secondaryContainer = Color(0xFF334155),
            surface = Color(0xFF111827),
            background = Color(0xFF0B1120),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF0F766E),
            primaryContainer = Color(0xFFCCFBF1),
            secondaryContainer = Color(0xFFE2E8F0),
            surface = Color(0xFFFFFBF7),
            background = Color(0xFFF8FAFC),
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
    var manualPrices by remember { mutableStateOf<Map<Int, Double>>(emptyMap()) }
    var screen by remember { mutableStateOf(Screen.Home) }
    var editingEntry by remember { mutableStateOf<UsageEntry?>(null) }
    var overridingCycle by remember { mutableStateOf<CycleSummary?>(null) }
    var showEntryDialog by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        entries.clear()
        entries.addAll(loadEntries(context))
        manualPrices = loadManualPrices(context)
    }

    val summaries = buildCycleSummaries(entries, settings, manualPrices = manualPrices)
    val currentIndex = cycleIndexOf(settings.cycleStartDate, LocalDate.now())
    val current = summaries.lastOrNull { it.index == currentIndex } ?: summaries.last()
    val totalDifference = money(summaries.sumOf { it.difference })

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (screen) {
                            Screen.Home -> "电费概览"
                            Screen.Records -> "用电记录"
                            Screen.Settings -> "设置"
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    IconButton(onClick = { showBatchDialog = true }) {
                        Icon(Icons.Default.Upload, contentDescription = "批量录入")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = screen == Screen.Home,
                    onClick = { screen = Screen.Home },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("概览") },
                )
                NavigationBarItem(
                    selected = screen == Screen.Records,
                    onClick = { screen = Screen.Records },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("记录") },
                )
                NavigationBarItem(
                    selected = screen == Screen.Settings,
                    onClick = { screen = Screen.Settings },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("设置") },
                )
            }
        },
        floatingActionButton = {
            if (screen != Screen.Settings) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingEntry = null
                        showEntryDialog = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("录入") },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
        ) {
            when (screen) {
                Screen.Home -> HomeScreen(
                    current = current,
                    summaries = summaries,
                    totalDifference = totalDifference,
                    manualPrices = manualPrices,
                    onOverridePrice = { overridingCycle = it },
                )
                Screen.Records -> RecordsScreen(
                    entries = entries.sortedByDescending { it.date.toEpochDay() },
                    onEdit = {
                        editingEntry = it
                        showEntryDialog = true
                    },
                    onDelete = {
                        entries.removeAll { entry -> entry.date == it.date }
                        saveEntries(context, entries)
                    },
                )
                Screen.Settings -> SettingsScreen(
                    settings = settings,
                    entries = entries,
                    onSave = {
                        settings = it
                        saveSettings(context, it)
                    },
                )
            }
        }
    }

    if (showEntryDialog) {
        EntryDialog(
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
            onSave = { price ->
                manualPrices = manualPrices + (summary.index to price)
                saveManualPrices(context, manualPrices)
                overridingCycle = null
            },
        )
    }
}

@Composable
private fun HomeScreen(
    current: CycleSummary,
    summaries: List<CycleSummary>,
    totalDifference: Double,
    manualPrices: Map<Int, Double>,
    onOverridePrice: (CycleSummary) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(Modifier.height(4.dp)) }
        item { CurrentCycleCard(current, totalDifference) }
        item { ThresholdCard(current.totalKwh) }
        item {
            SectionTitle("历史周期")
        }
        items(summaries.reversed(), key = { it.index }) { summary ->
            CycleCard(
                summary = summary,
                isOverridden = manualPrices.containsKey(summary.index),
                onOverridePrice = onOverridePrice,
            )
        }
        item { Spacer(Modifier.height(96.dp)) }
    }
}

@Composable
private fun RecordsScreen(
    entries: List<UsageEntry>,
    onEdit: (UsageEntry) -> Unit,
    onDelete: (UsageEntry) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Spacer(Modifier.height(4.dp)) }
        item {
            SummaryStrip(
                leftTitle = "已记录",
                leftValue = "${entries.size} 天",
                rightTitle = "总用电",
                rightValue = "${fmt(entries.sumOf { it.kwh })} 度",
            )
        }
        if (entries.isEmpty()) {
            item {
                EmptyState("还没有记录。点右下角“录入”添加第一天用电量。")
            }
        } else {
            items(entries, key = { it.date.toString() }) { entry ->
                EntryRow(entry, onEdit, onDelete)
            }
        }
        item { Spacer(Modifier.height(96.dp)) }
    }
}

@Composable
private fun SettingsScreen(settings: AppSettings, entries: List<UsageEntry>, onSave: (AppSettings) -> Unit) {
    val clipboard = LocalClipboardManager.current
    var startDate by remember(settings) { mutableStateOf(settings.cycleStartDate.toString()) }
    var initialPrice by remember(settings) { mutableStateOf(fmt(settings.initialPrice)) }
    var actualPrice by remember(settings) { mutableStateOf(fmt(settings.actualPrice)) }
    var message by remember { mutableStateOf("修改后点保存，历史周期会立即重新计算。") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(Modifier.height(4.dp)) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("计费规则", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("周期开始日期") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = initialPrice,
                        onValueChange = { initialPrice = it },
                        label = { Text("首个周期电价") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = actualPrice,
                        onValueChange = { actualPrice = it },
                        label = { Text("公寓实际扣费单价") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            val date = runCatching { LocalDate.parse(startDate) }.getOrNull()
                            val initial = initialPrice.toDoubleOrNull()
                            val actual = actualPrice.toDoubleOrNull()
                            if (date == null || initial == null || actual == null) {
                                message = "请检查日期和价格格式。"
                            } else {
                                onSave(AppSettings(date, twoDecimals(initial), twoDecimals(actual)))
                                message = "已保存。"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("保存设置")
                    }
                    Text(message, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("阶梯说明", fontWeight = FontWeight.SemiBold)
                    Text("上一周期 0-60 度：下一周期 0.80 元/度")
                    Text("上一周期 60.01-120 度：下一周期 1.00 元/度")
                    Text("上一周期超过 120 度：下一周期 1.20 元/度")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("数据导出", fontWeight = FontWeight.SemiBold)
                    Text("把每日记录复制为 CSV，可直接粘贴到 Excel。")
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(entriesToCsv(entries)))
                            message = "CSV 已复制。"
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("复制 CSV")
                    }
                }
            }
        }
        item { Spacer(Modifier.height(96.dp)) }
    }
}

@Composable
private fun CurrentCycleCard(current: CycleSummary, totalDifference: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("当前周期", style = MaterialTheme.typography.labelLarge)
                Text("${current.startDate} 至 ${current.endDate}", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = "${fmt(current.totalKwh)} 度",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
            )
            LinearProgressIndicator(
                progress = (current.totalKwh / 120.0).coerceIn(0.0, 1.0).toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("当前电价", "${fmt(current.expectedPrice)} 元/度", Modifier.weight(1f))
                MetricTile("下周期预计", "${fmt(nextCycleEstimatedPrice(current.totalKwh))} 元/度", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("预计电费", "${fmt(money(current.totalKwh * current.expectedPrice))} 元", Modifier.weight(1f))
                MetricTile(
                    title = if (totalDifference >= 0) "累计应退" else "累计应补",
                    value = "${fmt(kotlin.math.abs(totalDifference))} 元",
                    modifier = Modifier.weight(1f),
                    valueColor = if (totalDifference >= 0) Color(0xFF047857) else Color(0xFFB91C1C),
                )
            }
            if (!current.isComplete) {
                Text(
                    "数据缺失：本周期还有 ${current.missingDates.size} 天未录入",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("用电阈值", fontWeight = FontWeight.SemiBold)
            Text("60 度和 120 度会影响下一个 15 天周期电价。")
            Text(
                text = if (nextLimit == null) {
                    "当前周期已超过 120 度，下周期预计按最高档。"
                } else {
                    "距离 ${fmt(nextLimit)} 度还剩 ${fmt(nextLimit - totalKwh)} 度。"
                },
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun CycleCard(
    summary: CycleSummary,
    isOverridden: Boolean,
    onOverridePrice: (CycleSummary) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("${summary.startDate} - ${summary.endDate}", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = buildString {
                            append(if (summary.isComplete) "数据完整" else "缺 ${summary.missingDates.size} 天")
                            if (isOverridden) append(" · 手动电价")
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (summary.difference >= 0) "+${fmt(summary.difference)}" else "-${fmt(kotlin.math.abs(summary.difference))}",
                        color = if (summary.difference >= 0) Color(0xFF047857) else Color(0xFFB91C1C),
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = { onOverridePrice(summary) }) {
                        Icon(Icons.Default.Edit, contentDescription = "覆盖电价")
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("总用电", "${fmt(summary.totalKwh)} 度", Modifier.weight(1f))
                MetricTile("应执行", "${fmt(summary.expectedPrice)} 元", Modifier.weight(1f))
                MetricTile("实际扣费", "${fmt(summary.actualPrice)} 元", Modifier.weight(1f))
            }
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (summary.entries.isEmpty()) {
                        Text("本周期还没有记录。")
                    } else {
                        summary.entries.forEach {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(it.date.toString())
                                Text("${fmt(it.kwh)} 度")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: UsageEntry, onEdit: (UsageEntry) -> Unit, onDelete: (UsageEntry) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(entry.date.toString(), fontWeight = FontWeight.SemiBold)
                Text("${fmt(entry.kwh)} 度", style = MaterialTheme.typography.bodyMedium)
            }
            Row {
                IconButton(onClick = { onEdit(entry) }) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = { onDelete(entry) }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        }
    }
}

@Composable
private fun MetricTile(title: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.Unspecified) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
private fun SummaryStrip(leftTitle: String, leftValue: String, rightTitle: String, rightValue: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(leftTitle, leftValue, Modifier.weight(1f))
            MetricTile(rightTitle, rightValue, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun EmptyState(text: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun EntryDialog(entry: UsageEntry?, onDismiss: () -> Unit, onSave: (UsageEntry) -> Unit) {
    var date by remember(entry) { mutableStateOf(entry?.date?.toString() ?: LocalDate.now().toString()) }
    var kwh by remember(entry) { mutableStateOf(entry?.kwh?.let { fmt(it) } ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "录入用电量" else "编辑用电量") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("日期 YYYY-MM-DD") },
                    singleLine = true,
                )
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
                val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
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
                if (result.entries.isEmpty()) {
                    message = "没有可导入的数据。"
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
                if (parsed == null || parsed < 0.0) {
                    error = "请输入正确电价。"
                } else {
                    onSave(twoDecimals(parsed))
                }
            }) { Text("保存") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isOverridden) {
                    OutlinedButton(onClick = onClear) { Text("恢复自动") }
                }
                OutlinedButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}

private fun loadSettings(context: Context): AppSettings {
    val prefs = context.getSharedPreferences("electricity", Context.MODE_PRIVATE)
    return AppSettings(
        cycleStartDate = runCatching {
            prefs.getString("cycleStartDate", null)?.let { LocalDate.parse(it) }
        }.getOrNull() ?: LocalDate.now().minusDays(14),
        initialPrice = readDouble(prefs = prefs, key = "initialPrice", defaultValue = 1.2),
        actualPrice = readDouble(prefs = prefs, key = "actualPrice", defaultValue = 1.2),
    )
}

private fun saveSettings(context: Context, settings: AppSettings) {
    context.getSharedPreferences("electricity", Context.MODE_PRIVATE)
        .edit()
        .putString("cycleStartDate", settings.cycleStartDate.toString())
        .putString("initialPrice", fmt(settings.initialPrice))
        .putString("actualPrice", fmt(settings.actualPrice))
        .apply()
}

private fun loadEntries(context: Context): List<UsageEntry> {
    val raw = context.getSharedPreferences("electricity", Context.MODE_PRIVATE)
        .getString("entries", "")
        .orEmpty()
    return raw.lineSequence().mapNotNull { line ->
        val parts = line.split('\t')
        val date = parts.getOrNull(0)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val kwh = parts.getOrNull(1)?.toDoubleOrNull()
        if (date != null && kwh != null) UsageEntry(date, kwh) else null
    }.sortedBy { it.date.toEpochDay() }.toList()
}

private fun saveEntries(context: Context, entries: List<UsageEntry>) {
    val raw = entries.sortedBy { it.date.toEpochDay() }.joinToString("\n") { "${it.date}\t${fmt(it.kwh)}" }
    context.getSharedPreferences("electricity", Context.MODE_PRIVATE)
        .edit()
        .putString("entries", raw)
        .apply()
}

private fun loadManualPrices(context: Context): Map<Int, Double> {
    val raw = context.getSharedPreferences("electricity", Context.MODE_PRIVATE)
        .getString("manualPrices", "")
        .orEmpty()
    return raw.lineSequence().mapNotNull { line ->
        val parts = line.split('\t')
        val index = parts.getOrNull(0)?.toIntOrNull()
        val price = parts.getOrNull(1)?.toDoubleOrNull()
        if (index != null && price != null) index to twoDecimals(price) else null
    }.toMap()
}

private fun saveManualPrices(context: Context, prices: Map<Int, Double>) {
    val raw = prices.toSortedMap().entries.joinToString("\n") { "${it.key}\t${fmt(it.value)}" }
    context.getSharedPreferences("electricity", Context.MODE_PRIVATE)
        .edit()
        .putString("manualPrices", raw)
        .apply()
}

private fun entriesToCsv(entries: List<UsageEntry>): String {
    return buildString {
        appendLine("date,kwh")
        entries.sortedBy { it.date.toEpochDay() }.forEach {
            appendLine("${it.date},${fmt(it.kwh)}")
        }
    }
}

private fun readDouble(prefs: android.content.SharedPreferences, key: String, defaultValue: Double): Double {
    return when (val value = prefs.all[key]) {
        is String -> value.toDoubleOrNull() ?: defaultValue
        is Float -> twoDecimals(value.toDouble())
        is Double -> twoDecimals(value)
        is Int -> value.toDouble()
        else -> defaultValue
    }
}

private fun fmt(value: Double): String = String.format(Locale.US, "%.2f", value)
