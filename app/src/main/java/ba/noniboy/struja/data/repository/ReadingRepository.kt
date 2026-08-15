package ba.noniboy.struja.data.repository

import ba.noniboy.struja.data.local.dao.ReadingDao
import ba.noniboy.struja.data.local.entity.ReadingEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/**
 * Repository for meter readings.
 */
class ReadingRepository @Inject constructor(
    private val dao: ReadingDao
) {
    fun getForMeter(meterId: String): Flow<List<ReadingEntity>> = dao.getForMeterFlow(meterId)

    suspend fun getForMeterSync(meterId: String): List<ReadingEntity> = dao.getForMeter(meterId)

    suspend fun get(id: String): ReadingEntity? = dao.get(id)

    suspend fun getLatest(meterId: String): ReadingEntity? = dao.getLatest(meterId)

    suspend fun insert(reading: ReadingEntity) = dao.insert(reading)

    suspend fun update(reading: ReadingEntity) = dao.update(reading)

    suspend fun delete(reading: ReadingEntity) = dao.delete(reading)

    /**
     * Create a new reading entry.
     */
    suspend fun createReading(
        meterId: String,
        userId: String,
        recordedAt: String,
        vt: Double? = null,
        mt: Double? = null,
        reading: Double? = null,
        source: String,
        confidence: String? = null
    ): ReadingEntity {
        val entry = ReadingEntity(
            meterId = meterId,
            userId = userId,
            recordedAt = recordedAt,
            vt = vt,
            mt = mt,
            reading = reading,
            source = source,
            confidence = confidence
        )
        dao.insert(entry)
        return entry
    }

    /**
     * Get the previous reading for a meter (the reading immediately before the given date).
     */
    suspend fun getPreviousReading(meterId: String, beforeDate: String): ReadingEntity? {
        val all = dao.getForMeter(meterId)
        val sorted = all.sortedBy { it.recordedAt }
        return sorted.lastOrNull { it.recordedAt < beforeDate }
    }

    /**
     * Calculate days between two readings.
     */
    fun daysBetween(start: String, end: String): Int {
        return try {
            val startDate = LocalDate.parse(start)
            val endDate = LocalDate.parse(end)
            java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt()
        } catch (e: Exception) {
            0
        }
    }
}
