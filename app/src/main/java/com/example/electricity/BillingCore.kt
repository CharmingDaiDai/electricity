package com.example.electricity

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.round

const val CYCLE_DAYS = 15L

data class UsageEntry(
    val date: LocalDate,
    val kwh: Double,
)

data class AppSettings(
    val cycleStartDate: LocalDate,
    val initialPrice: Double = 1.2,
    val actualPrice: Double = 1.2,
    val rentDay: Int = 1,
)

data class CycleSummary(
    val index: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val entries: List<UsageEntry>,
    val missingDates: List<LocalDate>,
    val totalKwh: Double,
    val expectedPrice: Double,
    val actualPrice: Double,
) {
    val difference: Double = money((actualPrice - expectedPrice) * totalKwh)
    val isComplete: Boolean = missingDates.isEmpty()
}

data class BatchParseResult(
    val entries: List<UsageEntry>,
    val errors: List<String>,
)

fun priceFromPreviousUsage(totalKwh: Double): Double = when {
    totalKwh <= 60.0 -> 0.8
    totalKwh <= 120.0 -> 1.0
    else -> 1.2
}

fun cycleIndexOf(cycleStartDate: LocalDate, date: LocalDate): Int {
    val days = ChronoUnit.DAYS.between(cycleStartDate, date)
    return Math.floorDiv(days, CYCLE_DAYS).toInt()
}

fun cycleStartOf(cycleStartDate: LocalDate, index: Int): LocalDate =
    cycleStartDate.plusDays(index * CYCLE_DAYS)

fun cycleEndOf(cycleStartDate: LocalDate, index: Int): LocalDate =
    cycleStartOf(cycleStartDate, index).plusDays(CYCLE_DAYS - 1)

fun nextCycleEstimatedPrice(currentCycleTotalKwh: Double): Double =
    priceFromPreviousUsage(currentCycleTotalKwh)

fun parseBatchEntries(text: String): BatchParseResult {
    val entries = mutableListOf<UsageEntry>()
    val errors = mutableListOf<String>()

    text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .forEachIndexed { index, line ->
            val parts = line.split(Regex("[,，\\s]+")).filter { it.isNotBlank() }
            if (parts.size < 2) {
                errors += "第 ${index + 1} 行格式不正确：$line"
                return@forEachIndexed
            }

            val date = runCatching { LocalDate.parse(parts[0]) }.getOrNull()
            val kwh = parts[1].toDoubleOrNull()
            when {
                date == null -> errors += "第 ${index + 1} 行日期不正确：${parts[0]}"
                kwh == null || kwh < 0.0 -> errors += "第 ${index + 1} 行电量不正确：${parts[1]}"
                else -> entries += UsageEntry(date, twoDecimals(kwh))
            }
        }

    return BatchParseResult(entries, errors)
}

fun buildCycleSummaries(
    entries: List<UsageEntry>,
    settings: AppSettings,
    today: LocalDate = LocalDate.now(),
    manualPrices: Map<Int, Double> = emptyMap(),
): List<CycleSummary> {
    val byDate = entries.associateBy { it.date }
    val maxEntryDate = entries.maxByOrNull { it.date.toEpochDay() }?.date ?: today
    val lastDate = if (today.isAfter(maxEntryDate)) today else maxEntryDate
    val firstIndex = minOf(0, entries.minOfOrNull { cycleIndexOf(settings.cycleStartDate, it.date) } ?: 0)
    val lastIndex = maxOf(0, cycleIndexOf(settings.cycleStartDate, lastDate))

    val summaries = mutableListOf<CycleSummary>()
    for (index in firstIndex..lastIndex) {
        val start = cycleStartOf(settings.cycleStartDate, index)
        val end = cycleEndOf(settings.cycleStartDate, index)
        val visibleEnd = if (end.isBefore(today)) end else today
        val cycleEntries = entries
            .filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
            .sortedBy { it.date.toEpochDay() }
        val missingDates = if (!start.isAfter(today)) {
            generateSequence(start) { it.plusDays(1) }
                .takeWhile { !it.isAfter(visibleEnd) }
                .filter { byDate[it] == null }
                .toList()
        } else {
            emptyList()
        }

        val total = twoDecimals(cycleEntries.sumOf { it.kwh })
        val automaticPrice = if (index == firstIndex) {
            settings.initialPrice
        } else {
            priceFromPreviousUsage(summaries.last().totalKwh)
        }
        summaries += CycleSummary(
            index = index,
            startDate = start,
            endDate = end,
            entries = cycleEntries,
            missingDates = missingDates,
            totalKwh = total,
            expectedPrice = manualPrices[index] ?: automaticPrice,
            actualPrice = settings.actualPrice,
        )
    }

    return summaries
}

fun twoDecimals(value: Double): Double = round(value * 100.0) / 100.0

fun money(value: Double): Double = twoDecimals(value)
