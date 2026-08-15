package ba.noniboy.struja.data.local

import ba.noniboy.struja.data.local.entity.TariffRatesEntity
import ba.noniboy.struja.domain.models.TariffRates

/**
 * Mapper functions between domain models and Room entities.
 */

fun TariffRatesEntity.toDomain(): TariffRates {
    return TariffRates(
        serviceFee = serviceFee,
        powerFlatRate = powerFlatRate,
        powerKwRate = powerKwRate,
        oieRate = oieRate,
        vat = vat,
        blockI = blockI,
        blockII = blockII,
        vt = ba.noniboy.struja.domain.models.TariffTierRates(vtI, vtII, vtIII),
        mt = ba.noniboy.struja.domain.models.TariffTierRates(mtI, mtII, mtIII),
        transmission = ba.noniboy.struja.domain.models.TariffDirectionRates(transmissionVt, transmissionMt),
        distribution = ba.noniboy.struja.domain.models.TariffDirectionRates(distributionVt, distributionMt)
    )
}

fun TariffRates.toEntity(effectiveFrom: String = "2026-06-01"): TariffRatesEntity {
    return TariffRatesEntity(
        id = 1,
        effectiveFrom = effectiveFrom,
        serviceFee = serviceFee,
        powerFlatRate = powerFlatRate,
        powerKwRate = powerKwRate,
        oieRate = oieRate,
        vat = vat,
        blockI = blockI,
        blockII = blockII,
        vtI = vt.i,
        vtII = vt.ii,
        vtIII = vt.iii,
        mtI = mt.i,
        mtII = mt.ii,
        mtIII = mt.iii,
        transmissionVt = transmission.vt,
        transmissionMt = transmission.mt,
        distributionVt = distribution.vt,
        distributionMt = distribution.mt
    )
}

/**
 * Default seed rates entity from DEFAULT_RATES constants.
 */
val DEFAULT_RATES_ENTITY: TariffRatesEntity = TariffRates().toEntity("2026-06-01")
