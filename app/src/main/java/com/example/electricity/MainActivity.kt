package com.example.electricity

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

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
    val colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colors, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ElectricityApp() {
    val context = LocalContext.current
    val entries = remember { mutableStateListOf<UsageEntry>() }
    var settings by remember { mutableStateOf(loadSettings(context)) }
    var showAdd by remember { mutableStateOf(false) }
    var showBatch by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        entries.clear()
        entries.addAll(loadEntries(context))
    }

    val summaries = buildCycleSummaries(entries, settings)
    val currentIndex = cycleIndexOf(settings.cycleStartDate, LocalDate.now())
    val current = summaries.lastOrNull { it.index == currentIndex } ?: summaries.last()
    val totalDifference = money(summaries.sumOf { it.difference })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("电费记录") },
                actions = {
                    IconButton(onClick = { showBatch = true }) {
                        Icon(Icons.Default.Upload, contentDescription = "批量录入")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("录入") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CurrentCycleCard(current = current, totalDifference = totalDifference)
            }
            item {
                Text(
                    text = "历史周期",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(summaries.reversed(), key = { it.index }) { summary ->
                CycleCard(summary)
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showAdd) {
        EntryDialog(
            onDismiss = { showAdd = false },
            onSave = { entry ->
                entries.removeAll { it.date == entry.date }
                entries += entry
                entries.sortBy { it.date.toEpochDay() }
                saveEntries(context, entries)
                showAdd = false
            },
        )
    }

    if (showBatch) {
        BatchDialog(
            onDismiss = { showBatch = false },
            onImport = { parsed ->
                parsed.entries.forEach { entry ->
                    entries.removeAll { it.date == entry.date }
                    entries += entry
                }
                entries.sortBy { it.date.toEpochDay() }
                saveEntries(context, entries)
            },
        )
    }

    if (showSettings) {
        SettingsDialog(
            settings = settings,
            onDismiss = { showSettings = false },
            onSave = {
                settings = it
                saveSettings(context, it)
                showSettings = false
            },
        )
    }
}

@Composable
private fun CurrentCycleCard(current: CycleSummary, totalDifference: Double) {
    val progress = (current.totalKwh / 120.0).coerceIn(0.0, 1.0).toFloat()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${current.startDate} 至 ${current.endDate}", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "${fmt(current.totalKwh)} 度",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
            )
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("当前电价 ${fmt(current.expectedPrice)} 元/度")
                Text("下周期预计 ${fmt(nextCycleEstimatedPrice(current.totalKwh))} 元/度")
            }
            Text("当前预计电费 ${fmt(money(current.totalKwh * current.expectedPrice))} 元")
            Text(
                text = if (totalDifference >= 0) "累计应退 ${fmt(totalDifference)} 元" else "累计应补 ${fmt(-totalDifference)} 元",
                color = if (totalDifference >= 0) Color(0xFF047857) else Color(0xFFB91C1C),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
            if (!current.isComplete) {
                Text("数据缺失：本周期还有 ${current.missingDates.size} 天未录入")
            }
        }
    }
}

@Composable
private fun CycleCard(summary: CycleSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${summary.startDate} - ${summary.endDate}", fontWeight = FontWeight.SemiBold)
                Text(if (summary.isComplete) "完整" else "缺 ${summary.missingDates.size} 天")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("总用电 ${fmt(summary.totalKwh)} 度")
                Text("电价 ${fmt(summary.expectedPrice)} / ${fmt(summary.actualPrice)}")
            }
            Text(
                text = if (summary.difference >= 0) "应退 ${fmt(summary.difference)} 元" else "应补 ${fmt(-summary.difference)} 元",
                color = if (summary.difference >= 0) Color(0xFF047857) else Color(0xFFB91C1C),
                fontWeight = FontWeight.Bold,
            )
            summary.entries.takeLast(5).forEach {
                Text("${it.date}  ${fmt(it.kwh)} 度", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EntryDialog(onDismiss: () -> Unit, onSave: (UsageEntry) -> Unit) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var kwh by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("录入用电量") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("日期 YYYY-MM-DD") })
                OutlinedTextField(
                    value = kwh,
                    onValueChange = { kwh = it },
                    label = { Text("用电量（度）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
                val parsedKwh = kwh.toDoubleOrNull()
                if (parsedDate == null || parsedKwh == null || parsedKwh < 0.0) {
                    error = "请输入正确日期和非负电量"
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
                if (result.entries.isEmpty() || result.errors.isNotEmpty()) {
                    message = result.errors.joinToString("\n").ifBlank { "没有可导入的数据" }
                } else {
                    onImport(result)
                    onDismiss()
                }
            }) { Text("导入") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun SettingsDialog(settings: AppSettings, onDismiss: () -> Unit, onSave: (AppSettings) -> Unit) {
    var startDate by remember { mutableStateOf(settings.cycleStartDate.toString()) }
    var initialPrice by remember { mutableStateOf(settings.initialPrice.toString()) }
    var actualPrice by remember { mutableStateOf(settings.actualPrice.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("周期开始日期") })
                OutlinedTextField(value = initialPrice, onValueChange = { initialPrice = it }, label = { Text("初始周期电价") })
                OutlinedTextField(value = actualPrice, onValueChange = { actualPrice = it }, label = { Text("实际扣费单价") })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val date = runCatching { LocalDate.parse(startDate) }.getOrNull()
                val initial = initialPrice.toDoubleOrNull()
                val actual = actualPrice.toDoubleOrNull()
                if (date == null || initial == null || actual == null) {
                    error = "请检查日期和价格"
                } else {
                    onSave(AppSettings(date, initial, actual))
                }
            }) { Text("保存") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun loadSettings(context: Context): AppSettings {
    val prefs = context.getSharedPreferences("electricity", Context.MODE_PRIVATE)
    return AppSettings(
        cycleStartDate = runCatching {
            prefs.getString("cycleStartDate", null)?.let { LocalDate.parse(it) }
        }.getOrNull() ?: LocalDate.now().minusDays(14),
        initialPrice = prefs.getFloat("initialPrice", 1.2f).toDouble(),
        actualPrice = prefs.getFloat("actualPrice", 1.2f).toDouble(),
    )
}

private fun saveSettings(context: Context, settings: AppSettings) {
    context.getSharedPreferences("electricity", Context.MODE_PRIVATE)
        .edit()
        .putString("cycleStartDate", settings.cycleStartDate.toString())
        .putFloat("initialPrice", settings.initialPrice.toFloat())
        .putFloat("actualPrice", settings.actualPrice.toFloat())
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
    val raw = entries.sortedBy { it.date.toEpochDay() }.joinToString("\n") { "${it.date}\t${it.kwh}" }
    context.getSharedPreferences("electricity", Context.MODE_PRIVATE)
        .edit()
        .putString("entries", raw)
        .apply()
}

private fun fmt(value: Double): String = "%.2f".format(value)
