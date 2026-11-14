package com.example.electricity

import java.time.LocalDate
import java.util.Locale

private const val BACKUP_HEADER = "ELECTRICITY_BACKUP"
private const val BACKUP_VERSION = "1"

data class BackupSnapshot(
    val settings: AppSettings,
    val entries: List<UsageEntry>,
    val manualPrices: Map<Int, Double>,
    val settlements: Map<Int, Double>,
    val isLegacyCsv: Boolean = false,
)

fun encodeBackup(
    settings: AppSettings,
    entries: List<UsageEntry>,
    manualPrices: Map<Int, Double>,
    settlements: Map<Int, Double>,
): String = buildString {
    appendLine("$BACKUP_HEADER\t$BACKUP_VERSION")
    appendLine("[settings]")
    appendLine("cycleStartDate\t${settings.cycleStartDate}")
    appendLine("initialPrice\t${formatBackupDouble(settings.initialPrice)}")
    appendLine("actualPrice\t${formatBackupDouble(settings.actualPrice)}")
    appendLine("[entries]")
    entries.sortedBy { it.date.toEpochDay() }.forEach { entry ->
        appendLine("${entry.date}\t${formatBackupDouble(entry.kwh)}")
    }
    appendLine("[manualPrices]")
    manualPrices.toSortedMap().forEach { (index, value) ->
        appendLine("$index\t${formatBackupDouble(value)}")
    }
    appendLine("[settlements]")
    settlements.toSortedMap().forEach { (index, value) ->
        appendLine("$index\t${formatBackupDouble(value)}")
    }
}

fun decodeBackup(raw: String): BackupSnapshot? {
    val lines = raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
    val header = lines.firstOrNull()?.split('\t')
    if (header?.getOrNull(0) != BACKUP_HEADER || header.getOrNull(1) != BACKUP_VERSION) {
        return decodeLegacyCsv(lines)
    }

    var section = ""
    var cycleStartDate: LocalDate? = null
    var initialPrice = 1.2
    var actualPrice = 1.2
    val entries = mutableListOf<UsageEntry>()
    val manualPrices = mutableMapOf<Int, Double>()
    val settlements = mutableMapOf<Int, Double>()

    lines.drop(1).forEach { line ->
        if (line.startsWith("[") && line.endsWith("]")) {
            section = line.removePrefix("[").removeSuffix("]")
            return@forEach
        }
        val parts = line.split('\t')
        when (section) {
            "settings" -> when (parts.getOrNull(0)) {
                "cycleStartDate" -> cycleStartDate = runCatching { LocalDate.parse(parts.getOrNull(1).orEmpty()) }.getOrNull()
                "initialPrice" -> initialPrice = parts.getOrNull(1)?.toDoubleOrNull() ?: return@forEach
                "actualPrice" -> actualPrice = parts.getOrNull(1)?.toDoubleOrNull() ?: return@forEach
            }
            "entries" -> {
                val date = runCatching { LocalDate.parse(parts.getOrNull(0).orEmpty()) }.getOrNull()
                val kwh = parts.getOrNull(1)?.toDoubleOrNull()
                if (date != null && kwh != null && kwh >= 0.0) entries += UsageEntry(date, twoDecimals(kwh))
            }
            "manualPrices", "settlements" -> {
                val index = parts.getOrNull(0)?.toIntOrNull()
                val value = parts.getOrNull(1)?.toDoubleOrNull()
                if (index != null && value != null) {
                    if (section == "manualPrices") manualPrices[index] = twoDecimals(value)
                    else settlements[index] = money(value)
                }
            }
        }
    }

    val date = cycleStartDate ?: return null
    return BackupSnapshot(
        settings = AppSettings(date, twoDecimals(initialPrice), twoDecimals(actualPrice)),
        entries = entries.distinctBy { it.date }.sortedBy { it.date.toEpochDay() },
        manualPrices = manualPrices.toMap(),
        settlements = settlements.toMap(),
    )
}

private fun decodeLegacyCsv(lines: List<String>): BackupSnapshot? {
    if (lines.firstOrNull()?.lowercase(Locale.US) != "date,kwh") return null
    val entries = lines.drop(1).mapNotNull { line ->
        val parts = line.split(',', '，')
        val date = runCatching { LocalDate.parse(parts.getOrNull(0).orEmpty()) }.getOrNull()
        val kwh = parts.getOrNull(1)?.toDoubleOrNull()
        if (date != null && kwh != null && kwh >= 0.0) UsageEntry(date, twoDecimals(kwh)) else null
    }.distinctBy { it.date }.sortedBy { it.date.toEpochDay() }
    if (entries.isEmpty()) return null
    return BackupSnapshot(
        settings = AppSettings(LocalDate.now().minusDays(14)),
        entries = entries,
        manualPrices = emptyMap(),
        settlements = emptyMap(),
        isLegacyCsv = true,
    )
}

private fun formatBackupDouble(value: Double): String = String.format(Locale.US, "%.2f", value)
