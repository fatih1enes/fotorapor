package com.fatihenes.photoreport.feature.camera.engine

import android.content.Context
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import com.fatihenes.photoreport.core.common.di.Dispatcher
import com.fatihenes.photoreport.core.common.di.FotoRaporDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderWarmup @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(FotoRaporDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) {
    private val mutex = Mutex()
    private var cachedProvider: ProcessCameraProvider? = null
    private var cachedExtensions: ExtensionsManager? = null

    suspend fun awaitCameraProvider(): ProcessCameraProvider = withContext(ioDispatcher) {
        cachedProvider?.let { return@withContext it }
        mutex.withLock {
            cachedProvider?.let { return@withLock it }
            val provider = ProcessCameraProvider.getInstance(context).get()
            cachedProvider = provider
            provider
        }
    }

    suspend fun awaitExtensionsManager(provider: ProcessCameraProvider): ExtensionsManager = withContext(ioDispatcher) {
        cachedExtensions?.let { return@withContext it }
        mutex.withLock {
            cachedExtensions?.let { return@withLock it }
            val extensions = ExtensionsManager.getInstanceAsync(context, provider).get()
            cachedExtensions = extensions
            extensions
        }
    }

    fun prewarm() {
        // Fire-and-forget prewarming
        CoroutineScope(ioDispatcher).launch {
            try {
                val provider = awaitCameraProvider()
                awaitExtensionsManager(provider)
            } catch (e: Exception) {
                android.util.Log.w("ProviderWarmup", "Warmup failed: ${e.message}")
            }
        }
    }
}
