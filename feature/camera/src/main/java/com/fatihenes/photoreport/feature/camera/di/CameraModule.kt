package com.fatihenes.photoreport.feature.camera.di

import com.fatihenes.photoreport.feature.camera.engine.CameraEngine
import com.fatihenes.photoreport.feature.camera.engine.CameraXEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CameraModule {

    @Binds
    @Singleton
    abstract fun bindCameraEngine(
        cameraXEngine: CameraXEngine
    ): CameraEngine
}
