package ba.noniboy.struja.data.di

import android.content.Context
import androidx.room.Room
import ba.noniboy.struja.data.local.AppDatabase
import ba.noniboy.struja.data.local.dao.BillDao
import ba.noniboy.struja.data.local.dao.MeterDao
import ba.noniboy.struja.data.local.dao.ReadingDao
import ba.noniboy.struja.data.local.dao.TariffRatesDao
import ba.noniboy.struja.data.local.dao.UserSettingsDao
import ba.noniboy.struja.data.security.CryptoManager
import ba.noniboy.struja.data.vision.MeterOcrProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing database, DAOs, crypto, and OCR dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "struja-db"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTariffRatesDao(db: AppDatabase): TariffRatesDao = db.tariffRatesDao()

    @Provides
    fun provideMeterDao(db: AppDatabase): MeterDao = db.meterDao()

    @Provides
    fun provideReadingDao(db: AppDatabase): ReadingDao = db.readingDao()

    @Provides
    fun provideBillDao(db: AppDatabase): BillDao = db.billDao()

    @Provides
    fun provideUserSettingsDao(db: AppDatabase): UserSettingsDao = db.userSettingsDao()

    @Provides
    @Singleton
    fun provideCryptoManager(): CryptoManager = CryptoManager()

    @Provides
    @Singleton
    fun provideMeterOcrProcessor(): MeterOcrProcessor = MeterOcrProcessor()
}
