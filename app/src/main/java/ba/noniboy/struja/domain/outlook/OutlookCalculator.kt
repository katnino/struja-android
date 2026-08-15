package ba.noniboy.struja.domain.outlook

import ba.noniboy.struja.domain.models.BillResult
import ba.noniboy.struja.domain.models.DEFAULT_RATES
import ba.noniboy.struja.domain.models.TariffRates
import ba.noniboy.struja.domain.tariff.TariffCalculator
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max

/**
 * Port of outlook.ts — monthly electricity consumption projection.
 *
 * This module computes a month outlook by:
 * 1. Determining actual month-to-date consumption (from readings within the month).
 * 2. Deriving a daily run-rate from recent readings.
 * 3. Projecting the remaining month to estimate total consumption.
 * 4. Running the projected totals through TariffCalculator.
 */

data class MonthKey(val year: Int, val month: Int)

data class ActualMonthProgress(
    val vtKwh: Double,
    val mtKwh: Double,
    val daysElapsed: Int,
    val lastReadingDate: String
)

data class RunRate(
    val vtPerDay: Double,
    val mtPerDay: Double,
    val source: RunRateSource,
    val basedOnDays: Int
)

enum class RunRateSource { ROLLING_AVERAGE, LAST_INTERVAL }

data class MonthOutlook(
    val monthKey: MonthKey,
    val daysInMonth: Int,
    val actual: ActualMonthProgress?,
    val runRate: RunRate?,
    val projectedRemainingVt: Double,
    val projectedRemainingMt: Double,
    val totalEstimatedVt: Double,
    val totalEstimatedMt: Double,
    val bill: BillResult?,
    val confidence: OutlookConfidence
)

enum class OutlookConfidence { MEASURED, PROJECTED, INSUFFICIENT_DATA }

private const val ROLLING_WINDOW_DAYS = 3

/**
 * Interface for reading data — implemented by ReadingEntity and test doubles.
 */
interface ReadingData {
    val id: String
    val recordedAt: String
    val vt: Double?
    val mt: Double?
}

object OutlookCalculator {

    fun currentMonthKey(now: LocalDate = LocalDate.now()): MonthKey {
        return MonthKey(now.year, now.monthValue)
    }

    private fun daysInMonth(key: MonthKey): Int {
        val firstDay = LocalDate.of(key.year, key.month, 1)
        return firstDay.lengthOfMonth()
    }

    private fun monthStart(key: MonthKey): LocalDate {
        return LocalDate.of(key.year, key.month, 1)
    }

    private fun monthEnd(key: MonthKey): LocalDate {
        return LocalDate.of(key.year, key.month, daysInMonth(key))
    }

    private fun parseDate(iso: String): LocalDate {
        return LocalDate.parse(iso)
    }

    /**
     * Calculate the number of days between two dates.
     * Uses ChronoUnit (correct leap year handling), unlike the original port which used
     * a simplified calculation that didn't account for leap years.
     */
    private fun dateDiffDays(a: LocalDate, b: LocalDate): Int {
        return ChronoUnit.DAYS.between(a, b).toInt()
    }

    private fun sortedByDateAsc(readings: List<ReadingData>): List<ReadingData> {
        return readings
            .filter { it.vt != null && it.mt != null }
            .sortedBy { parseDate(it.recordedAt) }
    }

    /**
     * Get actual consumption for a given month from readings.
     *
     * Finds readings within the target month, determines a baseline
     * (last reading before month start if available), and computes the
     * delta in VT and MT consumption.
     */
    fun getActualMonthProgress(
        readings: List<ReadingData>,
        monthKey: MonthKey
    ): ActualMonthProgress? {
        val sorted = sortedByDateAsc(readings)
        if (sorted.isEmpty()) return null

        val start = monthStart(monthKey)
        val end = monthEnd(monthKey)

        val monthReadings = sorted.filter {
            val d = parseDate(it.recordedAt)
            !d.isBefore(start) && !d.isAfter(end)
        }
        if (monthReadings.isEmpty()) return null

        // Baseline = last reading strictly before this month started, if any.
        val baseline = sorted.asReversed().find {
            parseDate(it.recordedAt).isBefore(start)
        }

        val lastReading = monthReadings.last()
        val effectiveStart = baseline ?: monthReadings.first()

        // No baseline and only one reading this month → no delta to measure yet.
        if (effectiveStart.id == lastReading.id) return null

        val vtKwh = max(
            (lastReading.vt!! - effectiveStart.vt!!),
            0.0
        )
        val mtKwh = max(
            (lastReading.mt!! - effectiveStart.mt!!),
            0.0
        )
        val daysElapsed = max(
            dateDiffDays(parseDate(effectiveStart.recordedAt), parseDate(lastReading.recordedAt)),
            1
        )

        return ActualMonthProgress(
            vtKwh = vtKwh,
            mtKwh = mtKwh,
            daysElapsed = daysElapsed,
            lastReadingDate = lastReading.recordedAt
        )
    }

    /**
     * Derive a daily run-rate from recent readings.
     *
     * Uses a rolling window (default 3 days) of readings ending at the latest.
     * Falls back to the immediately preceding reading if there aren't enough
     * readings in the window.
     */
    fun deriveRunRate(
        readings: List<ReadingData>,
        windowDays: Int = ROLLING_WINDOW_DAYS
    ): RunRate? {
        val sorted = sortedByDateAsc(readings)
        if (sorted.size < 2) return null

        val latest = sorted.last()
        val latestDate = parseDate(latest.recordedAt)
        val windowStart = latestDate.minusDays(windowDays.toLong())

        // Walk backwards while readings still fall inside the window;
        // default to just the immediately preceding reading (last interval).
        var ref = sorted[sorted.size - 2]
        for (i in sorted.size - 2 downTo 0) {
            if (!parseDate(sorted[i].recordedAt).isBefore(windowStart)) {
                ref = sorted[i]
            } else {
                break
            }
        }

        val days = dateDiffDays(parseDate(ref.recordedAt), latestDate)
        if (days <= 0) return null // same-day readings — can't derive a safe rate

        val vtPerDay = max(
            ((latest.vt!! - ref.vt!!) / days),
            0.0
        )
        val mtPerDay = max(
            ((latest.mt!! - ref.mt!!) / days),
            0.0
        )

        return RunRate(
            vtPerDay = vtPerDay,
            mtPerDay = mtPerDay,
            source = if (days >= windowDays) RunRateSource.ROLLING_AVERAGE else RunRateSource.LAST_INTERVAL,
            basedOnDays = days
        )
    }

    /**
     * Build a full month outlook by combining actual consumption with
     * projected run-rate estimates, then computing the bill.
     */
    fun buildMonthOutlook(
        readings: List<ReadingData>,
        monthKey: MonthKey,
        rates: TariffRates = DEFAULT_RATES,
        approvedKw: Double = 3.3
    ): MonthOutlook {
        val totalDaysInMonth = daysInMonth(monthKey)
        val actual = getActualMonthProgress(readings, monthKey)
        val runRate = deriveRunRate(readings)

        if (actual == null && runRate == null) {
            return MonthOutlook(
                monthKey = monthKey,
                daysInMonth = totalDaysInMonth,
                confidence = OutlookConfidence.INSUFFICIENT_DATA,
                actual = null,
                runRate = null,
                projectedRemainingVt = 0.0,
                projectedRemainingMt = 0.0,
                totalEstimatedVt = 0.0,
                totalEstimatedMt = 0.0,
                bill = null
            )
        }

        val daysElapsed = actual?.daysElapsed ?: 0
        val daysRemaining = max(totalDaysInMonth - daysElapsed, 0)

        val projectedRemainingVt = if (runRate != null) runRate.vtPerDay * daysRemaining else 0.0
        val projectedRemainingMt = if (runRate != null) runRate.mtPerDay * daysRemaining else 0.0

        val totalEstimatedVt = (actual?.vtKwh ?: 0.0) + projectedRemainingVt
        val totalEstimatedMt = (actual?.mtKwh ?: 0.0) + projectedRemainingMt

        val bill = TariffCalculator.calculateBill(
            totalEstimatedVt,
            totalEstimatedMt,
            approvedKw,
            rates
        )

        val confidence: OutlookConfidence = when {
            actual != null && daysRemaining == 0 -> OutlookConfidence.MEASURED
            actual != null || runRate != null -> OutlookConfidence.PROJECTED
            else -> OutlookConfidence.INSUFFICIENT_DATA
        }

        return MonthOutlook(
            monthKey = monthKey,
            daysInMonth = totalDaysInMonth,
            actual = actual,
            runRate = runRate,
            projectedRemainingVt = projectedRemainingVt,
            projectedRemainingMt = projectedRemainingMt,
            totalEstimatedVt = totalEstimatedVt,
            totalEstimatedMt = totalEstimatedMt,
            bill = bill,
            confidence = confidence
        )
    }
}
