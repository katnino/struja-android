package ba.noniboy.struja.domain.models

/**
 * Breakdown of a single consumption block (I, II, or III).
 */
data class BlockBreakdown(
    val label: String,
    val kwh: Double,
    val rate: Double,
    val activeEnergyCost: Double,
    val transmissionCost: Double,
    val distributionCost: Double,
    val oieCost: Double,
    val totalCost: Double
)

/**
 * Full bill calculation result.
 * Mirrors the BillResult interface from tariff.ts.
 */
data class BillResult(
    val blocks: List<BlockBreakdown>,
    val totalKwh: Double,
    val mjernoMjesto: Double,
    val obracunskaSnaga: Double,
    val serviceFee: Double,
    val totalEnergy: Double,
    val transmissionBaseCost: Double,
    val totalTransmission: Double,
    val distributionBaseCost: Double,
    val totalDistribution: Double,
    val totalOie: Double,
    val subtotal: Double,
    val vatAmount: Double,
    val total: Double,
    val consumptionKwh: Double,
    val isPartialObračun: Boolean = false
)
