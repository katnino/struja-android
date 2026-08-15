package ba.noniboy.struja.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * An electricity bill generated from two consecutive readings.
 * Append-only — bills are never updated, only inserted.
 * The blocks JSON uses kotlinx-serialization to store a list of BlockBreakdown.
 */
@Entity(
    tableName = "bills",
    indices = [Index("meterId")]
)
data class BillEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val meterId: String,
    val userId: String,
    val periodStart: String,
    val periodEnd: String,
    val prevReadingId: String? = null,
    val currReadingId: String? = null,
    val approvedKw: Double,
    val consumptionKwh: Double,
    val mjernoMjesto: Double,
    val obracunskaSnaga: Double,
    val energyCost: Double,
    val oieCost: Double,
    val subtotal: Double,
    val vatAmount: Double,
    val total: Double,
    val blocksJson: String,      // JSON of BlockBreakdown[]
    val isPartialObračun: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
