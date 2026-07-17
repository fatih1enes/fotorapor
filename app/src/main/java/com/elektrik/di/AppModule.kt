package com.sarikaya.santiye.gunlugu.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.sarikaya.santiye.gunlugu.data.AppDatabase
import com.sarikaya.santiye.gunlugu.data.DailyLogDao
import com.sarikaya.santiye.gunlugu.data.PhotoDao
import com.sarikaya.santiye.gunlugu.data.ProjectDao
import com.sarikaya.santiye.gunlugu.manager.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "santiye_gunlugu_database"
        )
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
        .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .enableMultiInstanceInvalidation()
        .build()
    }

    @Provides
    @Singleton
    fun provideProjectDao(database: AppDatabase): ProjectDao {
        return database.projectDao()
    }

    @Provides
    @Singleton
    fun provideDailyLogDao(database: AppDatabase): DailyLogDao {
        return database.dailyLogDao()
    }

    @Provides
    @Singleton
    fun providePhotoDao(database: AppDatabase): PhotoDao {
        return database.photoDao()
    }

    @Provides
    @Singleton
    fun provideAppRepository(
        @ApplicationContext context: Context,
        projectDao: ProjectDao,
        dailyLogDao: DailyLogDao,
        photoDao: PhotoDao,
        mediaProcessor: com.sarikaya.santiye.gunlugu.util.MediaProcessor,
        fileManager: FileManager
    ): com.sarikaya.santiye.gunlugu.repository.AppRepository {
        return com.sarikaya.santiye.gunlugu.repository.AppRepositoryImpl(context, projectDao, dailyLogDao, photoDao, mediaProcessor, fileManager)
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("settings") }
        )
    }

    @Provides
    @Singleton
    fun provideFileManager(localFileManager: LocalFileManager): FileManager {
        return localFileManager
    }

    @Provides
    @Singleton
    fun providePdfExportManager(pdfBoxExportManager: PdfBoxExportManager): PdfExportManager {
        return pdfBoxExportManager
    }

    @Provides
    @Singleton
    fun provideBackupManager(localBackupManager: LocalBackupManager): BackupManager {
        return localBackupManager
    }
}
