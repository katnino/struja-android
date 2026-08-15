package ba.noniboy.struja.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A point-in-time meter reading.
 * For TG2 (dual-tariff): vt and mt are set, reading is null.
 * For TG1 (single-tariff): reading is set, vt/mt are null.
 */
@Entity(
    tableName = "readings",
    indices = [Index("meterId"), Index("userId")]
)
data class ReadingEntity(
    @PrimaryKey override val id: String = UUID.randomUUID().toString(),
    val meterId: String,
    val userId: String,
    override val recordedAt: String,      // YYYY-MM-DD
    val reading: Double? = null,
    override val vt: Double? = null,
    override val mt: Double? = null,
    val source: String,          // "manual", "ai"
    val confidence: String? = null, // "high", "low"
    val createdAt: Long = System.currentTimeMillis()
) : ba.noniboy.struja.domain.outlook.ReadingData