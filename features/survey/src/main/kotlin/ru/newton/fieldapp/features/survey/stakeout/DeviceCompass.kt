package ru.newton.fieldapp.features.survey.stakeout

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Device-magnetic-north heading, in degrees (0 = N, 90 = E). Powered by
 * [Sensor.TYPE_ROTATION_VECTOR] — modern and fused, so the value is stable
 * even with the phone tilted (which is exactly the stakeout posture).
 *
 * Returns 0f until the first sensor event. Gracefully degrades to a static 0
 * on devices without a rotation-vector sensor (rare on mid-range Android).
 */
@Composable
fun rememberDeviceHeadingDeg(): Float {
    val context = LocalContext.current
    var heading by remember { mutableFloatStateOf(0f) }

    DisposableEffect(context) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (manager == null || sensor == null) return@DisposableEffect onDispose {}

        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val azimuthRad = orientation[0]
                val azimuthDeg = (Math.toDegrees(azimuthRad.toDouble()) + 360.0) % 360.0
                heading = azimuthDeg.toFloat()
            }
            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { manager.unregisterListener(listener) }
    }
    return heading
}
