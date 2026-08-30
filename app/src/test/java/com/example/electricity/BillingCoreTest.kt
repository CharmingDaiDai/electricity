package com.example.electricity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BillingCoreTest {
    @Test
    fun priceBoundariesAreInclusive() {
        assertEquals(0.8, priceFromPreviousUsage(60.0), 0.0)
        assertEquals(1.0, priceFromPreviousUsage(60.01), 0.0)
        assertEquals(1.0, priceFromPreviousUsage(120.0), 0.0)
        assertEquals(1.2, priceFromPreviousUsage(120.01), 0.0)
    }

    @Test
    fun currentCyclePriceComesFromPreviousCycle() {
        val settings = AppSettings(
            cycleStartDate = LocalDate.parse("2026-08-01"),
            initialPrice = 0.8,
            actualPrice = 1.2,
        )
        val entries = listOf(
            UsageEntry(LocalDate.parse("2026-08-01"), 61.0),
            UsageEntry(LocalDate.parse("2026-08-16"), 10.0),
        )

        val summaries = buildCycleSummaries(
            entries = entries,
            settings = settings,
            today = LocalDate.parse("2026-08-16"),
        )

        assertEquals(0.8, summaries[0].expectedPrice, 0.0)
        assertEquals(1.0, summaries[1].expectedPrice, 0.0)
    }

    @Test
    fun batchParserAcceptsSpaceAndCommaSeparatedRows() {
        val result = parseBatchEntries(
            """
            2026-08-01 12.34
            2026-08-02,15.67
            """.trimIndent()
        )

        assertTrue(result.errors.isEmpty())
        assertEquals(2, result.entries.size)
        assertEquals(15.67, result.entries[1].kwh, 0.0)
    }
}
