package ba.noniboy.struja.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents an electricity meter.
 * Mirrors the web app's Meter interface but localized to local-only.
 */
@Entity(
    tableName = "meters",
    indices = [Index("userId")]
)
data class MeterEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val tariffGroup: String,     // "TG1" or "TG2"
    val approvedKw: Double,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
