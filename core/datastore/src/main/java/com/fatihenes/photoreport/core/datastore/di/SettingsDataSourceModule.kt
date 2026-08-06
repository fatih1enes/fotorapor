package com.fatihenes.photoreport.core.datastore.di

import com.fatihenes.photoreport.core.datastore.SettingsPreferencesDataSource
import com.fatihenes.photoreport.core.domain.datasource.LocalSettingsDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsDataSourceModule {
    @Binds
    @Singleton
    abstract fun bindLocalSettingsDataSource(impl: SettingsPreferencesDataSource): LocalSettingsDataSource
}
