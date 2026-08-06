package com.fatihenes.photoreport.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.fatihenes.photoreport.core.database.*
import com.fatihenes.photoreport.core.common.manager.*
import com.fatihenes.photoreport.manager.*
import com.fatihenes.photoreport.repository.*
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
    fun provideSettingsRepository(settingsRepositoryImpl: SettingsRepositoryImpl): SettingsRepository = settingsRepositoryImpl

    @Provides
    @Singleton
    fun provideReportRepository(reportRepositoryImpl: ReportRepositoryImpl): ReportRepository = reportRepositoryImpl

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
    ): PhotoRepository = PhotoRepositoryImpl(context, photoDao)

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
    fun provideFileManager(localFileManager: LocalFileManager): FileManager = localFileManager

    @Provides
    @Singleton
    fun provideBackupManager(localBackupManager: LocalBackupManager): BackupManager = localBackupManager

    @Provides
    @Singleton
    fun provideCommercialManager(localCommercialManager: LocalCommercialManager): CommercialManager = localCommercialManager
}
