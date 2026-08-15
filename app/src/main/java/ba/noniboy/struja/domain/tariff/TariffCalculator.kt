package ba.noniboy.struja.domain.tariff

import ba.noniboy.struja.domain.models.BillResult
import ba.noniboy.struja.domain.models.BlockBreakdown
import ba.noniboy.struja.domain.models.DEFAULT_RATES
import ba.noniboy.struja.domain.models.TariffRates
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * Port of tariff.ts billing algorithm.
 *
 * Computes REERS dual-tariff (VT/MT) block pricing bills.
 *
 * 1. Split total kWh (VT+MT) into 3 blocks:
 *    Block I:   0-500 kWh
 *    Block II:  501-1500 kWh
 *    Block III: 1500+ kWh
 * 2. In each block, blend VT and MT rates by proportion.
 * 3. Add transmission + distribution + OIE per block.
 * 4. Fixed charges: service fee, power charge (transmission + distribution * approvedKw).
 * 5. Partial period (<29 days): omit fixed charges, VAT, and power fees.
 * 6. Subtotal -> VAT (17%) -> Total.
 */
object TariffCalculator {

    private const val EPSILON = 1e-9

    /**
     * Round money to 2 decimal places.
     * Uses a small epsilon to avoid floating-point rounding issues.
     * In Kotlin, Double.MIN_VALUE is the smallest positive value (~4.9e-324),
     * not negative infinity. We use that as epsilon (matches JS Number.EPSILON approach).
     */
    fun roundMoney(value: Double): Double {
        return round((value + Double.MIN_VALUE) * 100) / 100
    }

    /**
     * Split total kWh into three blocks based on tariffs rates.
     * Block I: 0 to blockI
     * Block II: blockI to blockII
     * Block III: above blockII
     */
    fun splitBlocks(
        total: Double,
        rates: TariffRates = DEFAULT_RATES
    ): Triple<Double, Double, Double> {
        val i = min(total, rates.blockI.toDouble())
        val ii = min(max(total - rates.blockI, 0.0), (rates.blockII - rates.blockI).toDouble())
        val iii = max(total - rates.blockII, 0.0)
        return Triple(i, ii, iii)
    }

    /**
     * Calculate the full bill for the given VT and MT consumption.
     *
     * @param vtKwh Peak (visokotarifa) energy consumption in kWh.
     * @param mtKwh Off-peak (niskotarifa) energy consumption in kWh.
     * @param approvedKw Approved power in kW (obračunska snaga), defaults to 3.3.
     * @param rates Tariff rates, defaults to DEFAULT_RATES.
     * @param daysInPeriod Number of days in the billing period. If < 29, fixed charges are omitted.
     * @return BillResult with full breakdown.
     */
    fun calculateBill(
        vtKwh: Double,
        mtKwh: Double,
        approvedKw: Double = 3.3,
        rates: TariffRates = DEFAULT_RATES,
        daysInPeriod: Int? = null
    ): BillResult {
        // Invalid inputs (negative, NaN, infinite)
        if (!vtKwh.isFinite() || !mtKwh.isFinite() || vtKwh < 0 || mtKwh < 0) {
            return buildResult(emptyList(), 0.0, approvedKw, rates, daysInPeriod)
        }

        val consumptionKwh = vtKwh + mtKwh

        if (kotlin.math.abs(consumptionKwh) < EPSILON) {
            return buildResult(emptyList(), 0.0, approvedKw, rates, daysInPeriod)
        }

        val vtRatio = vtKwh / consumptionKwh
        val mtRatio = mtKwh / consumptionKwh
        val (i, ii, iii) = splitBlocks(consumptionKwh, rates)
        val blocks = mutableListOf<BlockBreakdown>()

        // Block I (0 - 500 kWh)
        if (i > 0) {
            val activeEnergyCost = roundMoney(i * vtRatio * rates.vt.i + i * mtRatio * rates.mt.i)
            val transmissionCost = roundMoney(i * vtRatio * rates.transmission.vt) +
                    roundMoney(i * mtRatio * rates.transmission.mt)
            val distributionCost = roundMoney(i * vtRatio * rates.distribution.vt) +
                    roundMoney(i * mtRatio * rates.distribution.mt)
            val oieCost = roundMoney(i * vtRatio * rates.oieRate) +
                    roundMoney(i * mtRatio * rates.oieRate)
            blocks.add(
                BlockBreakdown(
                    label = "Blok I (0-500 kWh)",
                    kwh = i,
                    rate = activeEnergyCost / i,
                    activeEnergyCost = activeEnergyCost,
                    transmissionCost = transmissionCost,
                    distributionCost = distributionCost,
                    oieCost = oieCost,
                    totalCost = activeEnergyCost + transmissionCost + distributionCost + oieCost
                )
            )
        }

        // Block II (501 - 1500 kWh)
        if (ii > 0) {
            val activeEnergyCost = roundMoney(ii * vtRatio * rates.vt.ii + ii * mtRatio * rates.mt.ii)
            val transmissionCost = roundMoney(ii * vtRatio * rates.transmission.vt) +
                    roundMoney(ii * mtRatio * rates.transmission.mt)
            val distributionCost = roundMoney(ii * vtRatio * rates.distribution.vt) +
                    roundMoney(ii * mtRatio * rates.distribution.mt)
            val oieCost = roundMoney(ii * vtRatio * rates.oieRate) +
                    roundMoney(ii * mtRatio * rates.oieRate)
            blocks.add(
                BlockBreakdown(
                    label = "Blok II (501-1500 kWh)",
                    kwh = ii,
                    rate = activeEnergyCost / ii,
                    activeEnergyCost = activeEnergyCost,
                    transmissionCost = transmissionCost,
                    distributionCost = distributionCost,
                    oieCost = oieCost,
                    totalCost = activeEnergyCost + transmissionCost + distributionCost + oieCost
                )
            )
        }

        // Block III (1501+ kWh)
        if (iii > 0) {
            val activeEnergyCost = roundMoney(iii * vtRatio * rates.vt.iii + iii * mtRatio * rates.mt.iii)
            val transmissionCost = roundMoney(iii * vtRatio * rates.transmission.vt) +
                    roundMoney(iii * mtRatio * rates.transmission.mt)
            val distributionCost = roundMoney(iii * vtRatio * rates.distribution.vt) +
                    roundMoney(iii * mtRatio * rates.distribution.mt)
            val oieCost = roundMoney(iii * vtRatio * rates.oieRate) +
                    roundMoney(iii * mtRatio * rates.oieRate)
            blocks.add(
                BlockBreakdown(
                    label = "Blok III (1501+ kWh)",
                    kwh = iii,
                    rate = activeEnergyCost / iii,
                    activeEnergyCost = activeEnergyCost,
                    transmissionCost = transmissionCost,
                    distributionCost = distributionCost,
                    oieCost = oieCost,
                    totalCost = activeEnergyCost + transmissionCost + distributionCost + oieCost
                )
            )
        }

        return buildResult(blocks, consumptionKwh, approvedKw, rates, daysInPeriod)
    }

    /**
     * Build the final BillResult from computed block breakdowns.
     * Handles partial-period logic (< 29 days omits fixed charges, VAT, power fees).
     */
    private fun buildResult(
        blocks: List<BlockBreakdown>,
        totalKwh: Double,
        approvedKw: Double,
        rates: TariffRates,
        daysInPeriod: Int?
    ): BillResult {
        val isPartial = daysInPeriod != null && daysInPeriod < 29

        if (totalKwh == 0.0) {
            return BillResult(
                blocks = blocks,
                totalKwh = totalKwh,
                mjernoMjesto = 0.0,
                obracunskaSnaga = 0.0,
                serviceFee = 0.0,
                totalEnergy = 0.0,
                transmissionBaseCost = 0.0,
                totalTransmission = 0.0,
                distributionBaseCost = 0.0,
                totalDistribution = 0.0,
                totalOie = 0.0,
                subtotal = 0.0,
                vatAmount = 0.0,
                total = 0.0,
                consumptionKwh = totalKwh,
                isPartialObračun = isPartial
            )
        }

        val serviceFee = rates.serviceFee
        val transmissionPowerFee = roundMoney(approvedKw * rates.powerFlatRate)
        val distributionPowerFee = roundMoney(approvedKw * rates.powerKwRate)

        val totalEnergy = roundMoney(blocks.sumOf { it.activeEnergyCost })
        val transmissionBaseCost = roundMoney(blocks.sumOf { it.transmissionCost })
        val distributionBaseCost = roundMoney(blocks.sumOf { it.distributionCost })
        val totalOie = roundMoney(blocks.sumOf { it.oieCost })

        val includedTransmissionPowerFee = if (isPartial) 0.0 else transmissionPowerFee
        val includedDistributionPowerFee = if (isPartial) 0.0 else distributionPowerFee
        val totalTransmission = roundMoney(transmissionBaseCost + includedTransmissionPowerFee)
        val totalDistribution = roundMoney(distributionBaseCost + includedDistributionPowerFee)

        val includedFixedCharges = if (isPartial) 0.0 else serviceFee
        val subtotal = roundMoney(includedFixedCharges + totalEnergy + totalTransmission + totalDistribution + totalOie)
        val vatAmount = if (isPartial) 0.0 else roundMoney(subtotal * rates.vat)

        return BillResult(
            blocks = blocks,
            totalKwh = totalKwh,
            mjernoMjesto = serviceFee,
            obracunskaSnaga = if (isPartial) 0.0 else transmissionPowerFee + distributionPowerFee,
            serviceFee = serviceFee,
            totalEnergy = totalEnergy,
            transmissionBaseCost = transmissionBaseCost,
            totalTransmission = totalTransmission,
            distributionBaseCost = distributionBaseCost,
            totalDistribution = totalDistribution,
            totalOie = totalOie,
            subtotal = subtotal,
            vatAmount = vatAmount,
            total = roundMoney(subtotal + vatAmount),
            consumptionKwh = totalKwh,
            isPartialObračun = isPartial
        )
    }

    // Port of summarizeBlocks from tariff.ts
    fun summarizeBlocks(blocks: List<BlockBreakdown>): SummaryResult {
        val result = blocks.fold(SummaryResult()) { acc, block ->
            SummaryResult(
                totalKwh = acc.totalKwh + block.kwh,
                totalEnergy = roundMoney(acc.totalEnergy + block.activeEnergyCost),
                totalTransmission = roundMoney(acc.totalTransmission + block.transmissionCost),
                totalDistribution = roundMoney(acc.totalDistribution + block.distributionCost),
                totalOie = roundMoney(acc.totalOie + block.oieCost),
                subtotal = roundMoney(acc.subtotal + block.totalCost)
            )
        }
        return result
    }

    data class SummaryResult(
        val totalKwh: Double = 0.0,
        val totalEnergy: Double = 0.0,
        val totalTransmission: Double = 0.0,
        val totalDistribution: Double = 0.0,
        val totalOie: Double = 0.0,
        val subtotal: Double = 0.0
    )
}
