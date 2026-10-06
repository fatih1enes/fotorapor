package com.fatihenes.photoreport.feature.camera.sensors

import android.content.Context
import android.view.OrientationEventListener
import android.view.Surface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Stable
class OrientationMonitorState(
    val surfaceRotation: State<Int>,
    val iconRotation: State<Float>
)

/**
 * Monitors physical orientation and calculates shortest-path smooth rotation
 * for UI icons and Surface rotation for CameraX targetRotation.
 */
@Composable
fun rememberOrientationMonitor(
    context: Context = LocalContext.current,
    onOrientationChanged: ((Int) -> Unit)? = null
): OrientationMonitorState {
    val surfaceRotationState = remember { mutableIntStateOf(Surface.ROTATION_0) }
    val iconRotationState = remember { mutableFloatStateOf(0f) }

    DisposableEffect(context) {
        val listener = object : OrientationEventListener(context.applicationContext) {
            private var lastRawOrientation = -1

            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return

                val targetSurfaceRot = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }

                if (surfaceRotationState.intValue != targetSurfaceRot) {
                    surfaceRotationState.intValue = targetSurfaceRot
                    onOrientationChanged?.invoke(targetSurfaceRot)
                }

                // Calculate shortest target angle in degrees for smooth icon rotation
                val targetAngle = when (targetSurfaceRot) {
                    Surface.ROTATION_90 -> 90f
                    Surface.ROTATION_180 -> 180f
                    Surface.ROTATION_270 -> -90f
                    else -> 0f
                }
                iconRotationState.floatValue = targetAngle
            }
        }

        if (listener.canDetectOrientation()) {
            listener.enable()
        }

        onDispose {
            listener.disable()
        }
    }

    val animatedRotation = animateFloatAsState(
        targetValue = iconRotationState.floatValue,
        animationSpec = tween(durationMillis = 220),
        label = "iconRotation"
    )

    return remember(surfaceRotationState, animatedRotation) {
        OrientationMonitorState(surfaceRotationState, animatedRotation)
    }
}
