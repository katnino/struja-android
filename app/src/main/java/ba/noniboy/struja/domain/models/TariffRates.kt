package ba.noniboy.struja.domain.models

/**
 * Represents the tariff rates for electricity billing.
 * Based on REERS Odluka 17.12.2024, effective 01.06.2026.
 */
data class TariffRates(
    val serviceFee: Double = 2.48,
    val powerFlatRate: Double = 0.2467,
    val powerKwRate: Double = 3.2425,
    val oieRate: Double = 0.0007,
    val vat: Double = 0.17,
    val blockI: Int = 500,
    val blockII: Int = 1500,
    val vt: TariffTierRates = TariffTierRates(0.0813, 0.1277, 0.2425),
    val mt: TariffTierRates = TariffTierRates(0.0406, 0.0638, 0.1212),
    val transmission: TariffDirectionRates = TariffDirectionRates(0.0120, 0.0060),
    val distribution: TariffDirectionRates = TariffDirectionRates(0.0673, 0.0337)
)

data class TariffTierRates(
    val i: Double,
    val ii: Double,
    val iii: Double
)

data class TariffDirectionRates(
    val vt: Double,
    val mt: Double
)

val DEFAULT_RATES = TariffRates()
