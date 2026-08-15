package ba.noniboy.struja.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single row storing the current REERS tariff rates.
 * These rates are seeded with DEFAULT_RATES on first app launch.
 */
@Entity(tableName = "tariff_rates")
data class TariffRatesEntity(
    @PrimaryKey val id: Int = 1,
    val effectiveFrom: String,
    val serviceFee: Double,
    val powerFlatRate: Double,
    val powerKwRate: Double,
    val oieRate: Double,
    val vat: Double,
    val blockI: Int,
    val blockII: Int,
    val vtI: Double,
    val vtII: Double,
    val vtIII: Double,
    val mtI: Double,
    val mtII: Double,
    val mtIII: Double,
    val transmissionVt: Double,
    val transmissionMt: Double,
    val distributionVt: Double,
    val distributionMt: Double
)
