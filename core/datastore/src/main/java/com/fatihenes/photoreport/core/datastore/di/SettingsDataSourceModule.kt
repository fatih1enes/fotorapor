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
fun interface SettingsDataSourceModule {
    @Binds
    fun bindLocalSettingsDataSource(impl: SettingsPreferencesDataSource): LocalSettingsDataSource
}
