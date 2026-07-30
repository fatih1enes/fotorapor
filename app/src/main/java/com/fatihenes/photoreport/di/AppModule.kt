package com.fatihenes.photoreport.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.fatihenes.photoreport.data.AppDatabase
import com.fatihenes.photoreport.data.DailyLogDao
import com.fatihenes.photoreport.data.PhotoDao
import com.fatihenes.photoreport.data.ProjectDao
import com.fatihenes.photoreport.manager.*
import com.fatihenes.photoreport.repository.*
import com.fatihenes.photoreport.util.MediaProcessor
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
    fun provideProjectDao(database: AppDatabase): ProjectDao = database.projectDao()

    @Provides
    @Singleton
    fun provideDailyLogDao(database: AppDatabase): DailyLogDao = database.dailyLogDao()

    @Provides
    @Singleton
    fun providePhotoDao(database: AppDatabase): PhotoDao = database.photoDao()

    @Provides
    @Singleton
    fun provideProjectRepository(
        @ApplicationContext context: Context,
        projectDao: ProjectDao
    ): ProjectRepository = ProjectRepositoryImpl(context, projectDao)

    @Provides
    @Singleton
    fun provideLogRepository(
        dailyLogDao: DailyLogDao
    ): LogRepository = LogRepositoryImpl(dailyLogDao)

    @Provides
    @Singleton
    fun providePhotoRepository(
        @ApplicationContext context: Context,
        photoDao: PhotoDao,
        dailyLogDao: DailyLogDao,
        mediaProcessor: MediaProcessor,
        watermarkRenderer: com.fatihenes.photoreport.util.WatermarkRenderer
    ): PhotoRepository = PhotoRepositoryImpl(context, photoDao, dailyLogDao, mediaProcessor, watermarkRenderer)

    @Provides
    @Singleton
    fun provideTrashRepository(
        projectDao: ProjectDao,
        dailyLogDao: DailyLogDao,
        photoDao: PhotoDao,
        fileManager: FileManager,
        projectRepository: ProjectRepository
    ): TrashRepository = TrashRepositoryImpl(projectDao, dailyLogDao, photoDao, fileManager, projectRepository)

    @Provides
    @Singleton
    fun provideAppRepository(
        projectRepository: ProjectRepository,
        logRepository: LogRepository,
        photoRepository: PhotoRepository,
        trashRepository: TrashRepository
    ): AppRepository = AppRepositoryImpl(projectRepository, logRepository, photoRepository, trashRepository)

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("settings") }
        )
    }

    @Provides
    @Singleton
    fun provideFileManager(localFileManager: LocalFileManager): FileManager = localFileManager

    @Provides
    @Singleton
    fun providePdfExportManager(pdfBoxExportManager: PdfBoxExportManager): PdfExportManager = pdfBoxExportManager

    @Provides
    @Singleton
    fun provideBackupManager(localBackupManager: LocalBackupManager): BackupManager = localBackupManager
}
