package ba.noniboy.struja.data.repository

import ba.noniboy.struja.data.local.dao.TariffRatesDao
import ba.noniboy.struja.data.local.DEFAULT_RATES_ENTITY
import ba.noniboy.struja.data.local.toDomain
import ba.noniboy.struja.data.local.toEntity
import ba.noniboy.struja.data.local.entity.TariffRatesEntity
import ba.noniboy.struja.domain.models.TariffRates
import javax.inject.Inject

/**
 * Repository for tariff rates.
 * Provides fallback to DEFAULT_RATES when no rates are stored locally.
 */
class TariffRepository @Inject constructor(
    private val dao: TariffRatesDao
) {
    suspend fun getRates(): TariffRates {
        val entity = dao.get()
        return if (entity != null) {
            entity.toDomain()
        } else {
            // Seed with defaults if not yet stored
            dao.insert(DEFAULT_RATES_ENTITY)
            TariffRates()
        }
    }

    suspend fun getRatesEntity(): TariffRatesEntity? {
        return dao.get()
    }

    suspend fun updateRates(rates: TariffRates) {
        dao.update(rates.toEntity())
    }
}