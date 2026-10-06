package com.fatihenes.photoreport.feature.camera.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

private const val LEVEL_THRESHOLD_DEGREES = 1.2f
private const val LOW_PASS_ALPHA = 0.20f
private const val FLAT_PITCH_THRESHOLD = 75f

@Stable
class LevelSensorState(
    val rollAngle: State<Float>,
    val pitchAngle: State<Float>,
    val isLevel: State<Boolean>,
    val isPointingFlat: State<Boolean>
)

@Composable
fun rememberLevelSensor(
    context: Context = LocalContext.current,
    enabled: Boolean = true
): LevelSensorState {
    val rollState = remember { mutableFloatStateOf(0f) }
    val pitchState = remember { mutableFloatStateOf(0f) }
    val isLevelState = remember { mutableStateOf(false) }
    val isPointingFlatState = remember { mutableStateOf(false) }

    DisposableEffect(context, enabled) {
        if (!enabled) return@DisposableEffect onDispose {}

        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var filteredX = 0f
        var filteredY = 0f
        var filteredZ = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Low-pass filter to eliminate micro hand vibrations
                filteredX = filteredX + LOW_PASS_ALPHA * (event.values[0] - filteredX)
                filteredY = filteredY + LOW_PASS_ALPHA * (event.values[1] - filteredY)
                filteredZ = filteredZ + LOW_PASS_ALPHA * (event.values[2] - filteredZ)

                val angle = Math.toDegrees(atan2(filteredX.toDouble(), filteredY.toDouble())).toFloat()
                val currentRoll = -angle
                rollState.floatValue = currentRoll

                // Pitch angle (elevation)
                val pitch = Math.toDegrees(
                    atan2(
                        filteredZ.toDouble(),
                        sqrt((filteredX * filteredX + filteredY * filteredY).toDouble())
                    )
                ).toFloat()
                pitchState.floatValue = pitch

                val isFlat = abs(pitch) > FLAT_PITCH_THRESHOLD
                isPointingFlatState.value = isFlat

                val absAngle = abs(currentRoll)
                val level = (absAngle < LEVEL_THRESHOLD_DEGREES) ||
                    (abs(absAngle - 90f) < LEVEL_THRESHOLD_DEGREES) ||
                    (abs(absAngle - 180f) < LEVEL_THRESHOLD_DEGREES)
                isLevelState.value = level
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (sensor != null) {
            sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sm.unregisterListener(listener)
        }
    }

    return remember(rollState, pitchState, isLevelState, isPointingFlatState) {
        LevelSensorState(rollState, pitchState, isLevelState, isPointingFlatState)
    }
}
