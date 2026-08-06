package com.fatihenes.photoreport.core.database.di

import android.content.Context
import androidx.room.Room
import com.fatihenes.photoreport.core.database.AppDatabase
import com.fatihenes.photoreport.core.database.DailyLogDao
import com.fatihenes.photoreport.core.database.PhotoDao
import com.fatihenes.photoreport.core.database.ProjectDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "photoreport_database"
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
}
