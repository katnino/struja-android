package ba.noniboy.struja.data.repository

import ba.noniboy.struja.data.local.dao.BillDao
import ba.noniboy.struja.data.local.entity.BillEntity
import ba.noniboy.struja.data.local.entity.ReadingEntity
import ba.noniboy.struja.domain.models.BlockBreakdown
import ba.noniboy.struja.domain.models.TariffRates
import ba.noniboy.struja.domain.tariff.TariffCalculator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Repository for electricity bills.
 * Bills are auto-generated when a new reading is added, similar to the web app.
 */
class BillRepository @Inject constructor(
    private val billDao: BillDao
) {
    fun getAllForMeter(meterId: String): Flow<List<BillEntity>> = billDao.getAllForMeterFlow(meterId)

    suspend fun getAllForMeterSync(meterId: String): List<BillEntity> = billDao.getAllForMeter(meterId)

    suspend fun get(id: String): BillEntity? = billDao.get(id)

    suspend fun insert(bill: BillEntity) = billDao.insert(bill)

    suspend fun deleteAllForMeter(meterId: String) = billDao.deleteAllForMeter(meterId)

    /**
     * Convert a BillResult (domain model) to a BillEntity for storage.
     */
    fun billResultToEntity(
        meterId: String,
        userId: String,
        periodStart: String,
        periodEnd: String,
        prevReadingId: String?,
        currReadingId: String,
        approvedKw: Double,
        rates: TariffRates,
        result: ba.noniboy.struja.domain.models.BillResult
    ): BillEntity {
        val gson = Gson()
        val blocksJson = gson.toJson(result.blocks)

        return BillEntity(
            meterId = meterId,
            userId = userId,
            periodStart = periodStart,
            periodEnd = periodEnd,
            prevReadingId = prevReadingId,
            currReadingId = currReadingId,
            approvedKw = approvedKw,
            consumptionKwh = result.consumptionKwh,
            mjernoMjesto = result.mjernoMjesto,
            obracunskaSnaga = result.obracunskaSnaga,
            energyCost = result.totalEnergy,
            oieCost = result.totalOie,
            subtotal = result.subtotal,
            vatAmount = result.vatAmount,
            total = result.total,
            blocksJson = blocksJson,
            isPartialObračun = result.isPartialObračun
        )
    }

    /**
     * Parse blocks JSON back to a list of BlockBreakdown.
     */
    fun parseBlocks(blocksJson: String): List<BlockBreakdown> {
        if (blocksJson.isBlank()) return emptyList()
        val gson = Gson()
        val type = object : TypeToken<List<BlockBreakdown>>() {}.type
        return gson.fromJson(blocksJson, type)
    }

    /**
     * Generate a bill when a new reading is added.
     * Mirrors the web app's createReadingAction logic.
     */
    suspend fun generateBill(
        meterId: String,
        userId: String,
        prevReading: ReadingEntity,
        currReading: ReadingEntity,
        rates: TariffRates,
        approvedKw: Double
    ): BillEntity? {
        val prevDate = prevReading.recordedAt
        val currDate = currReading.recordedAt

        val daysInPeriod = try {
            val start = java.time.LocalDate.parse(prevDate)
            val end = java.time.LocalDate.parse(currDate)
            java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt()
        } catch (e: Exception) {
            null
        }

        // For TG1 (single-tariff), use the single reading value
        // For TG2 (dual-tariff), use vt and mt values
        val vtKwh = currReading.vt ?: 0.0
        val mtKwh = currReading.mt ?: 0.0

        val prevVt = prevReading.vt
        val prevMt = prevReading.mt
        val prevReadingVal = prevReading.reading

        val consumptionVt = if (prevVt != null) maxOf(vtKwh - prevVt, 0.0) else 0.0
        val consumptionMt = if (prevMt != null) maxOf(mtKwh - prevMt, 0.0) else 0.0

        val result = TariffCalculator.calculateBill(
            vtKwh = consumptionVt,
            mtKwh = consumptionMt,
            approvedKw = approvedKw,
            rates = rates,
            daysInPeriod = daysInPeriod
        )

        return billResultToEntity(
            meterId = meterId,
            userId = userId,
            periodStart = prevDate,
            periodEnd = currDate,
            prevReadingId = prevReading.id,
            currReadingId = currReading.id,
            approvedKw = approvedKw,
            rates = rates,
            result = result
        ).also { billDao.insert(it) }
    }
}
