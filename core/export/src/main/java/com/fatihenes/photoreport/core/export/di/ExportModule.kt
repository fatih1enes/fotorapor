package com.fatihenes.photoreport.core.export.di

import com.fatihenes.photoreport.core.export.NativePdfExportManager
import com.fatihenes.photoreport.core.export.PdfExportManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExportModule {

    @Binds
    @Singleton
    abstract fun bindPdfExportManager(
        nativePdfExportManager: NativePdfExportManager
    ): PdfExportManager
}
