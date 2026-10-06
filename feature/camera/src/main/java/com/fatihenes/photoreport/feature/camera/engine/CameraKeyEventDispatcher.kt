package com.fatihenes.photoreport.feature.camera.engine

import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface CameraHardwareKeyEvent {
    data object Shutter : CameraHardwareKeyEvent
}

@Singleton
class CameraKeyEventDispatcher @Inject constructor() {

    private val _events = MutableSharedFlow<CameraHardwareKeyEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<CameraHardwareKeyEvent> = _events.asSharedFlow()

    @Volatile
    var isListening: Boolean = false

    fun onKeyEvent(event: KeyEvent): Boolean {
        val result: Boolean
        if (!isListening) {
            result = false
        } else {
            // Check if this is a camera-triggering key
            val isShutterKey = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                event.keyCode == KeyEvent.KEYCODE_CAMERA

            if (!isShutterKey) {
                result = false
            } else {
                // Only emit on ACTION_DOWN to avoid double-triggers on release
                val shouldEmit = event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0
                if (shouldEmit) {
                    _events.tryEmit(CameraHardwareKeyEvent.Shutter)
                }

                // Return true to consume the volume key event so system volume slider does not show up
                result = true
            }
        }
        return result
    }
}
