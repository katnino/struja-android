package ba.noniboy.struja.data.repository

import ba.noniboy.struja.data.local.dao.MeterDao
import ba.noniboy.struja.data.local.entity.MeterEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Repository for electricity meters.
 */
class MeterRepository @Inject constructor(
    private val dao: MeterDao
) {
    fun getAll(userId: String): Flow<List<MeterEntity>> = dao.getAllFlow(userId)

    suspend fun get(id: String): MeterEntity? = dao.get(id)

    suspend fun insert(meter: MeterEntity) = dao.insert(meter)

    suspend fun update(meter: MeterEntity) = dao.update(meter)

    suspend fun delete(meter: MeterEntity) = dao.delete(meter)

    /**
     * Create a new meter with sensible defaults.
     */
    suspend fun createMeter(
        userId: String,
        name: String,
        tariffGroup: String,
        approvedKw: Double,
        notes: String? = null
    ): MeterEntity {
        val meter = MeterEntity(
            userId = userId,
            name = name,
            tariffGroup = tariffGroup,
            approvedKw = approvedKw,
            notes = notes
        )
        dao.insert(meter)
        return meter
    }
}
