package ba.noniboy.struja.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import ba.noniboy.struja.data.local.dao.BillDao
import ba.noniboy.struja.data.local.dao.MeterDao
import ba.noniboy.struja.data.local.dao.ReadingDao
import ba.noniboy.struja.data.local.dao.TariffRatesDao
import ba.noniboy.struja.data.local.dao.UserSettingsDao
import ba.noniboy.struja.data.local.entity.BillEntity
import ba.noniboy.struja.data.local.entity.MeterEntity
import ba.noniboy.struja.data.local.entity.ReadingEntity
import ba.noniboy.struja.data.local.entity.TariffRatesEntity
import ba.noniboy.struja.data.local.entity.UserSettingsEntity

/***
 * Room database for the Struja app.
 * Contains all local-only tables: tariff rates, meters, readings, bills, user settings.
 */
@Database(
    entities = [
        TariffRatesEntity::class,
        MeterEntity::class,
        ReadingEntity::class,
        BillEntity::class,
        UserSettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tariffRatesDao(): TariffRatesDao
    abstract fun meterDao(): MeterDao
    abstract fun readingDao(): ReadingDao
    abstract fun billDao(): BillDao
    abstract fun userSettingsDao(): UserSettingsDao
}
