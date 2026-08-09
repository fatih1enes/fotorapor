package com.fatihenes.photoreport.core.database.di

import com.fatihenes.photoreport.core.database.datasource.LocalLogDataSourceImpl
import com.fatihenes.photoreport.core.database.datasource.LocalPhotoDataSourceImpl
import com.fatihenes.photoreport.core.database.datasource.LocalProjectDataSourceImpl
import com.fatihenes.photoreport.core.domain.datasource.LocalLogDataSource
import com.fatihenes.photoreport.core.domain.datasource.LocalPhotoDataSource
import com.fatihenes.photoreport.core.domain.datasource.LocalProjectDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataSourceModule {
    @Binds
    @Singleton
    fun bindLocalProjectDataSource(impl: LocalProjectDataSourceImpl): LocalProjectDataSource

    @Binds
    @Singleton
    fun bindLocalLogDataSource(impl: LocalLogDataSourceImpl): LocalLogDataSource

    @Binds
    @Singleton
    fun bindLocalPhotoDataSource(impl: LocalPhotoDataSourceImpl): LocalPhotoDataSource
}
